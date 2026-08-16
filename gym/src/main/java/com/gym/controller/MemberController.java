package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.common.Result;
import com.gym.entity.Member;
import com.gym.service.MemberService;
import com.gym.service.TimeService;
import com.gym.serviceImpl.MemberExpireScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
@CrossOrigin(origins = "*")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private TimeService timeService;

    @Autowired
    private MemberExpireScheduler memberExpireScheduler;

    @GetMapping("/list")
    @SentinelResource(value = "member-list", blockHandler = "handleBlock")
    public Result<List<Member>> getAllMembers() {
        List<Member> members = memberService.getAllMembers();
        return Result.success(members);
    }

    @GetMapping("/{account}")
    @SentinelResource(value = "member-get", blockHandler = "handleBlock")
    public Result<Member> getMemberByAccount(@PathVariable Integer account) {
        Member member = memberService.getMemberByAccount(account);
        if (member != null) {
            return Result.success(member);
        }
        return Result.error(404, "会员不存在");
    }

    @PostMapping("/add")
    @SentinelResource(value = "member-add", blockHandler = "handleBlock")
    public Result<Void> addMember(@RequestBody Member member) {
        memberService.addMember(member);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update")
    @SentinelResource(value = "member-update", blockHandler = "handleBlock")
    public Result<Void> updateMember(@RequestBody Member member) {
        memberService.updateMember(member);
        return Result.success("更新成功", null);
    }

    /**
     * 会员自助修改个人资料（仅姓名/性别/年龄/身高/体重/电话/头像，不可改卡类型和时长）
     */
    @PutMapping("/profile")
    @SentinelResource(value = "member-profile", blockHandler = "handleBlock")
    public Result<Void> updateProfile(@RequestBody Member member) {
        boolean success = memberService.updateProfile(member);
        if (success) {
            return Result.success("资料修改成功", null);
        }
        return Result.error(400, "资料修改失败：会员不存在");
    }

    /**
     * 前台续费：修改会员卡类型和有效期天数
     * body: { memberAccount, cardClass(可空), addDays(可空) }
     * cardClass 非空时按月卡/季卡/年卡续费；为空时按 addDays 自定义天数续费
     */
    @PutMapping("/renew")
    @SentinelResource(value = "member-renew", blockHandler = "handleBlock")
    public Result<Void> renewMember(@RequestBody Map<String, Object> body) {
        Integer account = Integer.parseInt(body.get("memberAccount").toString());
        Integer cardClass = body.get("cardClass") != null && !"".equals(body.get("cardClass").toString())
                ? Integer.parseInt(body.get("cardClass").toString()) : null;
        Integer addDays = body.get("addDays") != null && !"".equals(body.get("addDays").toString())
                ? Integer.parseInt(body.get("addDays").toString()) : null;
        if (cardClass == null && (addDays == null || addDays <= 0)) {
            return Result.error(400, "续费失败：请选择卡类型或填写有效的续费天数");
        }
        boolean success = memberService.renewMember(account, cardClass, addDays);
        if (success) {
            return Result.success("续费成功", null);
        }
        if (cardClass != null && (cardClass < 1 || cardClass > 3)) {
            return Result.error(400, "续费失败：卡类型无效（仅支持1=月卡/2=季卡/3=年卡）");
        }
        return Result.error(400, "续费失败：会员不存在");
    }

    @DeleteMapping("/delete/{account}")
    @SentinelResource(value = "member-delete", blockHandler = "handleBlock")
    public Result<Void> deleteMember(@PathVariable Integer account) {
        memberService.deleteMember(account);
        return Result.success("删除成功", null);
    }

    @PutMapping("/password")
    @SentinelResource(value = "member-password", blockHandler = "handleBlock")
    public Result<Void> updatePassword(@RequestBody Map<String, Object> body) {
        Integer account = Integer.parseInt(body.get("memberAccount").toString());
        String oldPassword = (String) body.get("oldPassword");
        String newPassword = (String) body.get("newPassword");
        Member member = memberService.getMemberByAccount(account);
        if (member == null) {
            return Result.error(404, "会员不存在");
        }
        if (!member.getMemberPassword().equals(oldPassword)) {
            return Result.error(400, "原密码错误");
        }
        boolean success = memberService.updatePassword(account, newPassword);
        if (success) {
            return Result.success("密码修改成功", null);
        }
        return Result.error(400, "密码修改失败");
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }

    /**
     * 【测试用】手动触发会员过期检查（无需等到 cron 8:00）
     * 配合 Nacos common.yaml 的 mock.time 可模拟任意时间点。
     * 返回当前 TimeService 日期、过期会员数量，并调用定时任务发送通知。
     * 测试完毕后请清空 mock.time 恢复真实时间。
     */
    @PostMapping("/test/expire-check")
    public Result<Map<String, Object>> testExpireCheck() {
        LocalDate today = timeService.nowDate();
        List<Member> expired = memberService.getExpiredMembers();

        // 调用定时任务逻辑，发送过期通知
        memberExpireScheduler.checkExpiredMembers();

        Map<String, Object> data = new HashMap<>();
        data.put("currentTime", today.toString());
        data.put("expiredCount", expired.size());
        data.put("expiredMembers", expired);
        return Result.success("过期检查已触发，查看日志确认通知发送结果", data);
    }
}
