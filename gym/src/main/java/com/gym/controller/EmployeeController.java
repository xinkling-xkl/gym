package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.common.Result;
import com.gym.entity.Employee;
import com.gym.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employee")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/list")
    @SentinelResource(value = "employee-list", blockHandler = "handleBlock")
    public Result<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeService.getAllEmployees();
        return Result.success(employees);
    }

    @GetMapping("/{account}")
    @SentinelResource(value = "employee-get", blockHandler = "handleBlock")
    public Result<Employee> getEmployeeByAccount(@PathVariable Integer account) {
        Employee employee = employeeService.getEmployeeByAccount(account);
        if (employee != null) {
            return Result.success(employee);
        }
        return Result.error(404, "员工不存在");
    }

    @PostMapping("/add")
    @SentinelResource(value = "employee-add", blockHandler = "handleBlock")
    public Result<Void> addEmployee(@RequestBody Employee employee) {
        employeeService.addEmployee(employee);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update")
    @SentinelResource(value = "employee-update", blockHandler = "handleBlock")
    public Result<Void> updateEmployee(@RequestBody Employee employee) {
        employeeService.updateEmployee(employee);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/delete/{account}")
    @SentinelResource(value = "employee-delete", blockHandler = "handleBlock")
    public Result<Void> deleteEmployee(@PathVariable Integer account) {
        employeeService.deleteEmployee(account);
        return Result.success("删除成功", null);
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
