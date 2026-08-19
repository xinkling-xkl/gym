package com.gym.serviceImpl;

import com.gym.service.NotificationProducer;
import com.gym.entity.ClassOrder;
import com.gym.entity.ClassTable;
import com.gym.mapper.ClassOrderMapper;
import com.gym.mapper.ClassTableMapper;
import com.gym.service.ClassTableService;
import com.gym.service.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClassTableServiceImpl implements ClassTableService {

    @Autowired
    private ClassTableMapper classTableMapper;

    @Autowired
    private ClassOrderMapper classOrderMapper;

    @Autowired
    private NotificationProducer notificationProducer;

    @Autowired
    private TimeService timeService;

    @Override
    public List<ClassTable> getAllClasses() {
        return classTableMapper.getAllClasses();
    }

    @Override
    public List<ClassTable> getAvailableClasses() {
        return classTableMapper.getAvailableClasses(timeService.nowDateTime());
    }

    @Override
    public ClassTable getClassById(Integer classId) {
        return classTableMapper.getClassById(classId);
    }

    @Override
    public List<ClassTable> getClassesByCoach(String coach) {
        return classTableMapper.getClassesByCoach(coach);
    }

    @Override
    public void addClass(ClassTable classTable) {
        classTableMapper.addClass(classTable);
    }

    @Override
    @Transactional
    public void updateClass(ClassTable classTable) {
        classTableMapper.updateClass(classTable);
        // 课程信息变更后，同步更新所有未开课(BOOKED)订单的快照字段，
        // 已完成/已取消/旷课等历史订单保持原快照不变
        classOrderMapper.syncBookedByClassId(
                classTable.getClassId(),
                classTable.getClassName(),
                classTable.getClassBegin(),
                classTable.getCoach()
        );
        // 通知已预约该课程的会员课程信息已变更
        List<ClassOrder> booked = classOrderMapper.getBookedOrdersByClassId(classTable.getClassId());
        if (booked != null) {
            for (ClassOrder o : booked) {
                try {
                    Map<String, Object> body = new HashMap<>();
                    body.put("userAccount", o.getMemberAccount());
                    body.put("title", "课程信息变更通知");
                    body.put("content", "您预约的课程【" + classTable.getClassName()
                            + "】信息已更新，请关注最新安排。");
                    body.put("type", "COURSE_UPDATE");
                    notificationProducer.sendNotification(body);
                } catch (Exception ignored) {
                    // 通知失败不影响主流程
                }
            }
        }
    }

    @Override
    public void deleteClass(Integer classId) {
        classTableMapper.deleteClass(classId);
    }
}
