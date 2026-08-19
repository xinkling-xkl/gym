package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.client.NotificationClient;
import com.gym.common.Result;
import com.gym.entity.ClassOrder;
import com.gym.entity.ClassTable;
import com.gym.entity.Employee;
import com.gym.mapper.ClassTableMapper;
import com.gym.mapper.EmployeeMapper;
import com.gym.service.ClassOrderService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@CrossOrigin(origins = "*")
public class ClassOrderController {

    @Autowired
    private ClassOrderService classOrderService;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private ClassTableMapper classTableMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @GetMapping("/list")
    @SentinelResource(value = "order-list", blockHandler = "handleBlock")
    public Result<List<ClassOrder>> getAllOrders() {
        List<ClassOrder> orders = classOrderService.getAllOrders();
        return Result.success(orders);
    }

    @GetMapping("/{id}")
    @SentinelResource(value = "order-get", blockHandler = "handleBlock")
    public Result<ClassOrder> getOrderById(@PathVariable Integer id) {
        ClassOrder order = classOrderService.getOrderById(id);
        if (order != null) {
            return Result.success(order);
        }
        return Result.error(404, "订单不存在");
    }

    @GetMapping("/member/{account}")
    @SentinelResource(value = "order-member-list", blockHandler = "handleBlock")
    public Result<List<ClassOrder>> getOrdersByMemberAccount(@PathVariable String account) {
        List<ClassOrder> orders = classOrderService.getOrdersByMemberAccount(account);
        return Result.success(orders);
    }

    @GetMapping("/member/{account}/{status}")
    @SentinelResource(value = "order-member-list-status", blockHandler = "handleBlock")
    public Result<List<ClassOrder>> getOrdersByMemberAccountAndStatus(@PathVariable String account,
                                                                       @PathVariable String status) {
        List<ClassOrder> orders = classOrderService.getOrdersByMemberAccountAndStatus(account, status);
        return Result.success(orders);
    }

    @GetMapping("/coach/{coachName}")
    @SentinelResource(value = "order-coach-list", blockHandler = "handleBlock")
    public Result<List<ClassOrder>> getOrdersByCoach(@PathVariable String coachName) {
        List<ClassOrder> orders = classOrderService.getOrdersByCoach(coachName);
        return Result.success(orders);
    }

    @PostMapping("/add")
    @SentinelResource(value = "order-add", blockHandler = "handleBlock")
    public Result<Void> addOrder(@RequestBody ClassOrder classOrder) {
        int result = classOrderService.addOrder(classOrder);
        if (result == 1) {
            String beginStr = formatTime(classOrder.getClassBegin());
            // 通知会员：预约成功
            sendNotification(classOrder.getMemberAccount(),
                    "预约成功",
                    "您已成功预约课程【" + classOrder.getClassName() + "】，开课时间：" + beginStr
                            + "，教练：" + classOrder.getCoach() + "。请提前10分钟到达，不要错过上课时间！",
                    "BOOKING_SUCCESS");
            // 通知教练：课程被预约
            String coachAccount = getCoachAccount(classOrder.getCoach());
            if (coachAccount != null) {
                sendNotification(coachAccount,
                        "课程预约通知",
                        "会员【" + (classOrder.getMemberName() != null ? classOrder.getMemberName() : classOrder.getMemberAccount())
                                + "】已预约您的课程【" + classOrder.getClassName() + "】，开课时间：" + beginStr + "。",
                        "BOOKING_SUCCESS");
            }
            return Result.success("预约成功", null);
        } else if (result == -1) {
            return Result.error(400, "会员卡已过期或无效，请续费后再预约");
        } else if (result == -3) {
            return Result.error(404, "课程不存在");
        } else if (result == -4) {
            return Result.error(400, "课程已开始，无法预约");
        } else if (result == -5) {
            return Result.error(400, "课程已约满");
        } else if (result == -6) {
            return Result.error(400, "您已预约该课程，请勿重复预约");
        } else if (result == -7) {
            return Result.error(400, "课程已结束，无法预约");
        } else if (result == -8) {
            return Result.error(429, "当前预约人数较多，请稍后重试");
        } else if (result == -9) {
            return Result.error(400, "该时段您已预约其他课程，请勿重复预约");
        } else {
            return Result.error(404, "会员不存在");
        }
    }

    @PutMapping("/update")
    @SentinelResource(value = "order-update", blockHandler = "handleBlock")
    public Result<Void> updateOrder(@RequestBody ClassOrder classOrder) {
        classOrderService.updateOrder(classOrder);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/delete/{id}")
    @SentinelResource(value = "order-delete", blockHandler = "handleBlock")
    public Result<Void> deleteOrder(@PathVariable Integer id) {
        classOrderService.deleteOrder(id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/cancel/{id}")
    @SentinelResource(value = "order-cancel", blockHandler = "handleBlock")
    public Result<Void> cancelOrder(@PathVariable Integer id) {
        // 先查订单详情用于发通知
        ClassOrder order = classOrderService.getOrderById(id);
        int result = classOrderService.cancelOrder(id);
        if (result == 1) {
            String beginStr = formatTime(order != null ? order.getClassBegin() : null);
            String className = order != null ? order.getClassName() : "未知";
            String memberName = order != null ? order.getMemberName() : "";
            String memberAccount = order != null ? order.getMemberAccount() : "";
            // 通知会员：用户取消
            sendNotification(memberAccount,
                    "预约取消通知",
                    "您已取消课程【" + className + "】的预约（开课时间：" + beginStr + "）。",
                    "BOOKING_CANCELLED");
            // 通知教练：用户取消
            if (order != null) {
                String coachAccount = getCoachAccount(order.getCoach());
                if (coachAccount != null) {
                    sendNotification(coachAccount,
                            "课程预约取消通知",
                            "会员【" + (memberName != null ? memberName : memberAccount)
                                    + "】已取消课程【" + className + "】的预约（开课时间：" + beginStr + "）。",
                            "BOOKING_CANCELLED");
                }
            }
            return Result.success("取消预约成功", null);
        } else if (result == -1) {
            return Result.error(404, "订单不存在");
        } else if (result == -2) {
            return Result.error(400, "订单状态不可取消");
        } else if (result == -3) {
            return Result.error(400, "开课前2小时内不可取消预约");
        }
        return Result.error(400, "取消失败");
    }

    @PostMapping("/adminCancel/{id}")
    @SentinelResource(value = "order-admin-cancel", blockHandler = "handleBlock")
    public Result<Void> adminCancelOrder(@PathVariable Integer id) {
        ClassOrder order = classOrderService.getOrderById(id);
        boolean success = classOrderService.adminCancelOrder(id);
        if (success) {
            // 通知会员：预约被强制取消
            if (order != null) {
                String beginStr = formatTime(order.getClassBegin());
                sendNotification(order.getMemberAccount(),
                        "预约取消通知",
                        "您的课程【" + order.getClassName() + "】预约已被取消（开课时间：" + beginStr + "）。",
                        "BOOKING_CANCELLED");
            }
            return Result.success("已强制取消该订单", null);
        }
        return Result.error(404, "订单不存在");
    }

    @PostMapping("/complete/{id}")
    @SentinelResource(value = "order-complete", blockHandler = "handleBlock")
    public Result<Void> completeOrder(@PathVariable Integer id) {
        boolean success = classOrderService.completeOrder(id);
        if (success) {
            return Result.success("已标记为完成", null);
        }
        return Result.error(400, "操作失败：订单不存在、状态不可标记或不在可操作时间(开课后6小时内)");
    }

    @PostMapping("/noshow/{id}")
    @SentinelResource(value = "order-noshow", blockHandler = "handleBlock")
    public Result<Void> markNoShow(@PathVariable Integer id) {
        boolean success = classOrderService.markNoShow(id);
        if (success) {
            return Result.success("已标记为旷课", null);
        }
        return Result.error(400, "操作失败：订单不存在、状态不可标记或不在可操作时间(开课后6小时内)");
    }

    @PostMapping("/cancelByClass/{classId}")
    @SentinelResource(value = "order-cancel-by-class", blockHandler = "handleBlock")
    public Result<Void> cancelByClassId(@PathVariable Integer classId) {
        // 取消前先拿到被影响的预约列表，用于发通知
        List<ClassOrder> bookedOrders = classOrderService.getBookedOrdersByClassId(classId);
        ClassTable cls = classTableMapper.getClassById(classId);
        String className = cls != null ? cls.getClassName() : ("课程#" + classId);

        int count = classOrderService.cancelByClassId(classId);

        // 通知每位被取消预约的会员（教练取消）
        String beginStr = cls != null ? formatTime(cls.getClassBegin()) : "未知";
        for (ClassOrder o : bookedOrders) {
            sendNotification(o.getMemberAccount(),
                    "课程取消通知",
                    "教练已取消课程【" + className + "】（开课时间：" + beginStr + "），您的预约已被取消。",
                    "COURSE_CANCEL");
        }
        // 通知教练：课程已取消
        if (cls != null) {
            String coachAccount = getCoachAccount(cls.getCoach());
            if (coachAccount != null) {
                sendNotification(coachAccount,
                        "课程取消通知",
                        "课程【" + className + "】已取消，所有预约已撤销。",
                        "COURSE_CANCEL");
            }
        }
        return Result.success("课程已取消，共取消 " + count + " 条预约并通知会员", null);
    }

    /** 发送通知（失败不影响主流程） */
    private void sendNotification(String userAccount, String title, String content, String type) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userAccount", userAccount);
            body.put("title", title);
            body.put("content", content);
            body.put("type", type);
            notificationClient.sendNotification(body);
        } catch (Exception ignored) {
        }
    }

    /** 通过教练名字查教练账号（用于发通知） */
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

    /** 格式化时间 */
    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "未知";
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
