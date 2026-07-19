package com.gym.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "main-server")
public interface AdminClient {

    @GetMapping("/api/admin/{account}")
    Map<String, Object> getAdmin(@PathVariable("account") Integer account);

    @PostMapping("/api/admin/add")
    Map<String, Object> addAdmin(@RequestBody Map<String, Object> admin);

    @PutMapping("/api/admin/update")
    Map<String, Object> updateAdmin(@RequestBody Map<String, Object> admin);

    @DeleteMapping("/api/admin/delete/{account}")
    Map<String, Object> deleteAdmin(@PathVariable("account") Integer account);
}
