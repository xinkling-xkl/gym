package com.gym.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "main-server")
public interface MemberClient {

    @GetMapping("/api/member/list")
    Map<String, Object> getAllMembers();

    @GetMapping("/api/member/{account}")
    Map<String, Object> getMember(@PathVariable("account") Integer account);

    @PostMapping("/api/member/add")
    Map<String, Object> addMember(@RequestBody Map<String, Object> member);

    @PutMapping("/api/member/update")
    Map<String, Object> updateMember(@RequestBody Map<String, Object> member);

    @DeleteMapping("/api/member/delete/{account}")
    Map<String, Object> deleteMember(@PathVariable("account") Integer account);
}
