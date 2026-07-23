package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.common.Result;
import com.gym.entity.Member;
import com.gym.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member")
@CrossOrigin(origins = "*")
public class MemberController {

    @Autowired
    private MemberService memberService;

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

    @DeleteMapping("/delete/{account}")
    @SentinelResource(value = "member-delete", blockHandler = "handleBlock")
    public Result<Void> deleteMember(@PathVariable Integer account) {
        memberService.deleteMember(account);
        return Result.success("删除成功", null);
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
