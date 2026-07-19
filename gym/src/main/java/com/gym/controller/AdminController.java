package com.gym.controller;

import com.gym.entity.Admin;
import com.gym.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/{account}")
    public Map<String, Object> getAdminByAccount(@PathVariable Integer account) {
        Map<String, Object> result = new HashMap<>();
        try {
            Admin admin = adminService.getAdminByAccount(account);
            if (admin != null) {
                result.put("code", 200);
                result.put("data", admin);
                result.put("message", "查询成功");
            } else {
                result.put("code", 404);
                result.put("message", "管理员不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> addAdmin(@RequestBody Admin admin) {
        Map<String, Object> result = new HashMap<>();
        try {
            adminService.addAdmin(admin);
            result.put("code", 200);
            result.put("message", "添加成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> updateAdmin(@RequestBody Admin admin) {
        Map<String, Object> result = new HashMap<>();
        try {
            adminService.updateAdmin(admin);
            result.put("code", 200);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{account}")
    public Map<String, Object> deleteAdmin(@PathVariable Integer account) {
        Map<String, Object> result = new HashMap<>();
        try {
            adminService.deleteAdmin(account);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }
}
