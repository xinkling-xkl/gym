package com.gym.serviceImpl;

import com.gym.entity.ClassOrder;
import com.gym.entity.ClassTable;
import com.gym.entity.Member;
import com.gym.mapper.ClassOrderMapper;
import com.gym.mapper.ClassTableMapper;
import com.gym.mapper.MemberMapper;
import com.gym.service.ClassOrderService;
import com.gym.service.TimeService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClassOrderServiceImpl implements ClassOrderService {

    private static final int CANCEL_LIMIT_HOURS = 2;
    /** 学员课程签到最晚时间（开课后 4 小时内） */
    private static final int MEMBER_CHECKIN_LIMIT_HOURS = 4;
    /** 教练完成/旷课操作最晚时间（开课后 6 小时内） */
    private static final int COACH_OPERATE_LIMIT_HOURS = 6;
    /** 取消预约后冷却时间（分钟），冷却期内不可重新预约同一课程 */
    private static final int REBOOK_COOLDOWN_MINUTES = 5;

    @Autowired
    private ClassOrderMapper classOrderMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private ClassTableMapper classTableMapper;

    @Autowired
    private TimeService timeService;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public List<ClassOrder> getAllOrders() {
        return classOrderMapper.getAllOrders();
    }

    @Override
    public ClassOrder getOrderById(Integer classOrderId) {
        return classOrderMapper.getOrderById(classOrderId);
    }

    @Override
    public List<ClassOrder> getOrdersByMemberAccount(String memberAccount) {
        return classOrderMapper.getOrdersByMemberAccount(memberAccount);
    }

    @Override
    public List<ClassOrder> getOrdersByMemberAccountAndStatus(String memberAccount, String status) {
        return classOrderMapper.getOrdersByMemberAccountAndStatus(memberAccount, status);
    }

    @Override
    public List<ClassOrder> getOrdersByCoach(String coach) {
        return classOrderMapper.getOrdersByCoach(coach);
    }

    @Override
    @Transactional
    public int addOrder(ClassOrder classOrder) {
        Integer account;
        try {
            account = Integer.parseInt(classOrder.getMemberAccount());
        } catch (NumberFormatException e) {
            return -2;
        }
        // Redisson 分布式锁：按课程 ID 加锁，防止多实例并发超卖
        String lockKey = "lock:order:class:" + classOrder.getClassId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                return -8; // 获取锁超时，请稍后重试
            }
            return doAddOrder(classOrder, account);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -8;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private int doAddOrder(ClassOrder classOrder, Integer account) {
        Member member = memberMapper.getMemberByAccount(account);
        if (member == null) {
            return -2;
        }
        // 课时改为剩余天数：会员卡未过期即可预约
        if (member.getCardExpireDate() == null
                || member.getCardExpireDate().isBefore(timeService.nowDate())) {
            return -1;
        }

        ClassTable cls = classTableMapper.getClassById(classOrder.getClassId());
        if (cls == null) {
            return -3;
        }

        if (cls.getClassBegin() != null
                && cls.getClassBegin().isBefore(timeService.nowDateTime())) {
            return -4;
        }

        // 重复预约 + 时段冲突拦截：同会员已预约的 BOOKED 订单
        List<ClassOrder> existOrders = classOrderMapper.getOrdersByMemberAccountAndStatus(
                classOrder.getMemberAccount(), "BOOKED");
        if (existOrders != null) {
            int newDuration = parseDurationMinutes(cls.getClassTime());
            LocalDateTime newStart = cls.getClassBegin();
            LocalDateTime newEnd = newStart != null
                    ? newStart.plusMinutes(newDuration > 0 ? newDuration : 60)
                    : null;
            for (ClassOrder o : existOrders) {
                if (Objects.equals(o.getClassId(), classOrder.getClassId())) {
                    return -6;
                }
                // 时段冲突检测：同一会员在同一时段不能预约两门不同课程
                if (newStart != null && newEnd != null && o.getClassBegin() != null) {
                    ClassTable existCls = classTableMapper.getClassById(o.getClassId());
                    int existDuration = existCls != null ? parseDurationMinutes(existCls.getClassTime()) : -1;
                    LocalDateTime existStart = o.getClassBegin();
                    LocalDateTime existEnd = existStart.plusMinutes(existDuration > 0 ? existDuration : 60);
                    if (newStart.isBefore(existEnd) && existStart.isBefore(newEnd)) {
                        return -9;
                    }
                }
            }
        }

        // 课程已结束拦截：教练已对这节课执行过完成/旷课，禁止再预约
        int processed = classOrderMapper.countProcessedByClassId(classOrder.getClassId());
        if (processed > 0) {
            return -7;
        }

        // 取消后冷却检查：5 分钟内不可重新预约同一课程
        String cooldownKey = "cancel:cooldown:" + classOrder.getMemberAccount() + ":" + classOrder.getClassId();
        if (redissonClient.getBucket(cooldownKey).isExists()) {
            return -10;
        }

        if (cls.getMaxCapacity() != null && cls.getMaxCapacity() > 0) {
            int booked = classOrderMapper.countBookedByClassId(cls.getClassId());
            if (booked >= cls.getMaxCapacity()) {
                return -5;
            }
        }

        classOrder.setClassName(cls.getClassName());
        if (classOrder.getClassBegin() == null) {
            classOrder.setClassBegin(cls.getClassBegin());
        }
        if (classOrder.getCoach() == null) {
            classOrder.setCoach(cls.getCoach());
        }

        // 课时制已改为天数有效期制，预约不再扣减课时
        classOrderMapper.addOrder(classOrder);
        return 1;
    }

    /**
     * 解析课程时长（如"45分钟"→45），解析失败返回 -1
     */
    private int parseDurationMinutes(String classTime) {
        if (classTime == null) return -1;
        Matcher m = Pattern.compile("(\\d+)").matcher(classTime);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    @Override
    public void updateOrder(ClassOrder classOrder) {
        classOrderMapper.updateOrder(classOrder);
    }

    @Override
    public void deleteOrder(Integer classOrderId) {
        classOrderMapper.deleteOrder(classOrderId);
    }

    @Override
    @Transactional
    public int cancelOrder(Integer classOrderId) {
        ClassOrder order = classOrderMapper.getOrderById(classOrderId);
        if (order == null) {
            return -1;
        }
        if (!"BOOKED".equals(order.getStatus())) {
            return -2;
        }
        // 开课前 2 小时内不可取消
        if (order.getClassBegin() != null) {
            long hoursLeft = ChronoUnit.HOURS.between(timeService.nowDateTime(), order.getClassBegin());
            if (hoursLeft < CANCEL_LIMIT_HOURS) {
                return -3;
            }
        }
        // 天数有效期制，取消预约不再退课时
        classOrderMapper.updateStatus(classOrderId, "CANCELLED");

        // 设置冷却 key：5 分钟内不可重新预约同一课程
        String cooldownKey = "cancel:cooldown:" + order.getMemberAccount() + ":" + order.getClassId();
        redissonClient.getBucket(cooldownKey).set("1", REBOOK_COOLDOWN_MINUTES, TimeUnit.MINUTES);

        return 1;
    }

    @Override
    @Transactional
    public boolean completeOrder(Integer classOrderId) {
        ClassOrder order = classOrderMapper.getOrderById(classOrderId);
        if (order == null) {
            return false;
        }
        if (!"BOOKED".equals(order.getStatus()) && !"CHECKED_IN".equals(order.getStatus())) {
            return false;
        }
        // 教练完成操作：需在课程开始后，且开课 6 小时内
        if (!isWithinCoachOperateWindow(order)) {
            return false;
        }
        classOrderMapper.updateStatus(classOrderId, "COMPLETED");
        return true;
    }

    @Override
    @Transactional
    public boolean markNoShow(Integer classOrderId) {
        ClassOrder order = classOrderMapper.getOrderById(classOrderId);
        if (order == null) {
            return false;
        }
        if (!"BOOKED".equals(order.getStatus())) {
            return false;
        }
        // 教练旷课操作：需在课程开始后，且开课 6 小时内
        if (!isWithinCoachOperateWindow(order)) {
            return false;
        }
        classOrderMapper.updateStatus(classOrderId, "NO_SHOW");
        return true;
    }

    @Override
    @Transactional
    public int cancelByClassId(Integer classId) {
        List<ClassOrder> booked = classOrderMapper.getBookedOrdersByClassId(classId);
        if (booked == null || booked.isEmpty()) {
            return 0;
        }
        // 天数有效期制，批量取消不再退课时
        return classOrderMapper.batchCancelByClassId(classId);
    }

    @Override
    public List<ClassOrder> getBookedOrdersByClassId(Integer classId) {
        return classOrderMapper.getBookedOrdersByClassId(classId);
    }

    @Override
    @Transactional
    public boolean adminCancelOrder(Integer classOrderId) {
        ClassOrder order = classOrderMapper.getOrderById(classOrderId);
        if (order == null) {
            return false;
        }
        // 管理员强制取消：不限状态、不限时间（已终结状态也可改回 CANCELLED）
        classOrderMapper.updateStatus(classOrderId, "CANCELLED");
        return true;
    }

    /**
     * 教练完成/旷课操作时间窗：课程开始后 ~ 开课 6 小时内
     */
    private boolean isWithinCoachOperateWindow(ClassOrder order) {
        if (order.getClassBegin() == null) {
            return false;
        }
        LocalDateTime now = timeService.nowDateTime();
        if (now.isBefore(order.getClassBegin())) {
            return false; // 课程未开始
        }
        long hoursAfterBegin = ChronoUnit.HOURS.between(order.getClassBegin(), now);
        return hoursAfterBegin <= COACH_OPERATE_LIMIT_HOURS;
    }
}
