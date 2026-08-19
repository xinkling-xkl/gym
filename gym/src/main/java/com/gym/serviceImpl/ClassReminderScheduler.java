package com.gym.serviceImpl;

import com.gym.service.NotificationProducer;
import com.gym.entity.ClassOrder;
import com.gym.entity.Employee;
import com.gym.mapper.ClassOrderMapper;
import com.gym.mapper.EmployeeMapper;
import com.gym.service.TimeService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 课程开课前提醒定时任务
 * 每 10 分钟扫描一次，对 30 分钟内即将开课的 BOOKED 订单发送提醒，
 * 用 Redis SETNX 去重，确保每个订单只提醒一次。
 */
@Component
public class ClassReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClassReminderScheduler.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private ClassOrderMapper classOrderMapper;

    @Autowired
    private NotificationProducer notificationProducer;

    @Autowired
    private TimeService timeService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 每 10 分钟执行一次，提醒未来 30 分钟内开课的预约
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void remindUpcomingClasses() {
        LocalDateTime now = timeService.nowDateTime();
        LocalDateTime end = now.plusMinutes(30);
        List<ClassOrder> orders = classOrderMapper.getBookedOrdersInRange(now, end);
        if (orders == null || orders.isEmpty()) {
            return;
        }
        int sentCount = 0;
        for (ClassOrder order : orders) {
            try {
                // 1. 通知会员
                String memberKey = "reminder:order:" + order.getClassOrderId();
                RBucket<String> memberBucket = redissonClient.getBucket(memberKey);
                if (memberBucket.trySet("1", 24, TimeUnit.HOURS)) {
                    Map<String, Object> body = new HashMap<>();
                    body.put("userAccount", order.getMemberAccount());
                    body.put("title", "开课提醒");
                    body.put("content", "您预约的课程【" + order.getClassName() + "】即将于 "
                            + (order.getClassBegin() != null ? order.getClassBegin().format(FORMATTER) : "")
                            + " 开课，教练：" + (order.getCoach() != null ? order.getCoach() : "-")
                            + "。请提前 10 分钟到场准备。");
                    body.put("type", "CLASS_REMINDER");
                    notificationProducer.sendNotification(body);
                    sentCount++;
                }

                // 2. 通知教练
                String coachAccount = getCoachAccount(order.getCoach());
                if (coachAccount != null) {
                    String coachKey = "reminder:order:coach:" + order.getClassOrderId();
                    RBucket<String> coachBucket = redissonClient.getBucket(coachKey);
                    if (coachBucket.trySet("1", 24, TimeUnit.HOURS)) {
                        Map<String, Object> body = new HashMap<>();
                        body.put("userAccount", coachAccount);
                        body.put("title", "开课提醒");
                        body.put("content", "您教授的课程【" + order.getClassName() + "】即将于 "
                                + (order.getClassBegin() != null ? order.getClassBegin().format(FORMATTER) : "")
                                + " 开课，请提前准备。");
                        body.put("type", "CLASS_REMINDER");
                        notificationProducer.sendNotification(body);
                        sentCount++;
                    }
                }
            } catch (Exception e) {
                log.error("发送开课提醒失败, classOrderId={}", order.getClassOrderId(), e);
            }
        }
        log.info("开课提醒完成，共发送 {} 条通知", sentCount);
    }

    /**
     * 通过教练名字查教练账号（用于发通知）
     */
    private String getCoachAccount(String coachName) {
        if (coachName == null || coachName.isEmpty()) {
            return null;
        }
        try {
            Employee emp = employeeMapper.getEmployeeByName(coachName);
            return emp != null ? String.valueOf(emp.getEmployeeAccount()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
