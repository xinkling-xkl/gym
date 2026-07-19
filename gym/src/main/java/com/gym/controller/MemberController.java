package com.gym.controller;

import com.gym.entity.Member;
import com.gym.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
@CrossOrigin(origins = "*")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping("/list")
    public Map<String, Object> getAllMembers() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Member> members = memberService.getAllMembers();
            result.put("code", 200);
            result.put("data", members);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/{account}")
    public Map<String, Object> getMemberByAccount(@PathVariable Integer account) {
        Map<String, Object> result = new HashMap<>();
        try {
            Member member = memberService.getMemberByAccount(account);
            if (member != null) {
                result.put("code", 200);
                result.put("data", member);
                result.put("message", "查询成功");
            } else {
                result.put("code", 404);
                result.put("message", "会员不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> addMember(@RequestBody Member member) {
        Map<String, Object> result = new HashMap<>();
        try {
            memberService.addMember(member);
            result.put("code", 200);
            result.put("message", "添加成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> updateMember(@RequestBody Member member) {
        Map<String, Object> result = new HashMap<>();
        try {
            memberService.updateMember(member);
            result.put("code", 200);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{account}")
    public Map<String, Object> deleteMember(@PathVariable Integer account) {
        Map<String, Object> result = new HashMap<>();
        try {
            memberService.deleteMember(account);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }
}
