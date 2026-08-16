package com.gym.controller;

import com.gym.common.Result;
import com.gym.entity.Admin;
import com.gym.entity.Employee;
import com.gym.entity.Member;
import com.gym.service.AdminService;
import com.gym.service.EmployeeService;
import com.gym.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一用户查询 — 根据账号自动判断属于 admin / member / employee 哪张表
 */
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/{account}")
    public Result<Map<String, Object>> getUserByAccount(@PathVariable Integer account) {
        Admin admin = adminService.getAdminByAccount(account);
        if (admin != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("role", "ADMIN");
            data.put("account", admin.getAdminAccount());
            data.put("password", admin.getAdminPassword());
            data.put("name", "管理员");
            return Result.success(data);
        }

        Member member = memberService.getMemberByAccount(account);
        if (member != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("role", "MEMBER");
            data.put("account", member.getMemberAccount());
            data.put("password", member.getMemberPassword());
            data.put("name", member.getMemberName());
            return Result.success(data);
        }

        Employee employee = employeeService.getEmployeeByAccount(account);
        if (employee != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("role", "EMPLOYEE");
            data.put("account", employee.getEmployeeAccount());
            data.put("password", employee.getEmployeePassword());
            data.put("name", employee.getEmployeeName());
            data.put("staff", employee.getStaff());
            return Result.success(data);
        }

        return Result.error(404, "账号不存在");
    }
}
