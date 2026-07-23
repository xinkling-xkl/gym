package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.common.Result;
import com.gym.entity.Admin;
import com.gym.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/{account}")
    @SentinelResource(value = "admin-get", blockHandler = "handleBlock")
    public Result<Admin> getAdminByAccount(@PathVariable Integer account) {
        Admin admin = adminService.getAdminByAccount(account);
        if (admin != null) {
            return Result.success(admin);
        }
        return Result.error(404, "管理员不存在");
    }

    @PostMapping("/add")
    @SentinelResource(value = "admin-add", blockHandler = "handleBlock")
    public Result<Void> addAdmin(@RequestBody Admin admin) {
        adminService.addAdmin(admin);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update")
    @SentinelResource(value = "admin-update", blockHandler = "handleBlock")
    public Result<Void> updateAdmin(@RequestBody Admin admin) {
        adminService.updateAdmin(admin);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/delete/{account}")
    @SentinelResource(value = "admin-delete", blockHandler = "handleBlock")
    public Result<Void> deleteAdmin(@PathVariable Integer account) {
        adminService.deleteAdmin(account);
        return Result.success("删除成功", null);
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
