package com.gym.controller;

import com.gym.entity.Employee;
import com.gym.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/list")
    public Map<String, Object> getAllEmployees() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Employee> employees = employeeService.getAllEmployees();
            result.put("code", 200);
            result.put("data", employees);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/{account}")
    public Map<String, Object> getEmployeeByAccount(@PathVariable Integer account) {
        Map<String, Object> result = new HashMap<>();
        try {
            Employee employee = employeeService.getEmployeeByAccount(account);
            if (employee != null) {
                result.put("code", 200);
                result.put("data", employee);
                result.put("message", "查询成功");
            } else {
                result.put("code", 404);
                result.put("message", "员工不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> addEmployee(@RequestBody Employee employee) {
        Map<String, Object> result = new HashMap<>();
        try {
            employeeService.addEmployee(employee);
            result.put("code", 200);
            result.put("message", "添加成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> updateEmployee(@RequestBody Employee employee) {
        Map<String, Object> result = new HashMap<>();
        try {
            employeeService.updateEmployee(employee);
            result.put("code", 200);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{account}")
    public Map<String, Object> deleteEmployee(@PathVariable Integer account) {
        Map<String, Object> result = new HashMap<>();
        try {
            employeeService.deleteEmployee(account);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }
}
