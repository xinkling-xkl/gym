package com.gym.serviceImpl;

import com.gym.client.NotificationClient;
import com.gym.entity.Member;
import com.gym.service.MemberService;
import com.gym.service.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员卡过期检查定时任务
 * 每天早上 8 点执行，检查所有已过期会员并发送通知提醒
 */
@Component
public class MemberExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(MemberExpireScheduler.class);

    @Autowired
    private MemberService memberService;

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private TimeService timeService;

    /**
     * 每天早上 8 点执行一次
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkExpiredMembers() {
        log.info("开始检查过期会员...");
        List<Member> expiredMembers = memberService.getExpiredMembers();
        if (expiredMembers == null || expiredMembers.isEmpty()) {
            log.info("没有过期会员");
            return;
        }

        LocalDate today = timeService.nowDate();
        int sentCount = 0;
        for (Member member : expiredMembers) {
            try {
                long expiredDays = ChronoUnit.DAYS.between(member.getCardExpireDate(), today);
                Map<String, Object> notification = new HashMap<>();
                notification.put("userAccount", String.valueOf(member.getMemberAccount()));
                notification.put("title", "会员卡已过期");
                notification.put("content", "您的会员卡已于 " + member.getCardExpireDate()
                        + " 过期（已过期 " + expiredDays + " 天），请及时续费以继续享受健身服务。");
                notification.put("type", "SYSTEM");

                notificationClient.sendNotification(notification);
                sentCount++;
            } catch (Exception e) {
                log.error("发送过期通知失败, memberAccount={}", member.getMemberAccount(), e);
            }
        }
        log.info("过期会员检查完成，共发送 {} 条通知", sentCount);
    }
}
