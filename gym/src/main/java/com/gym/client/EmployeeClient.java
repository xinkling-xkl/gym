package com.gym.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "main-server", contextId = "employeeClient")
public interface EmployeeClient {

    @GetMapping("/api/employee/list")
    Map<String, Object> getAllEmployees();

    @GetMapping("/api/employee/{account}")
    Map<String, Object> getEmployee(@PathVariable("account") Integer account);

    @PostMapping("/api/employee/add")
    Map<String, Object> addEmployee(@RequestBody Map<String, Object> employee);

    @PutMapping("/api/employee/update")
    Map<String, Object> updateEmployee(@RequestBody Map<String, Object> employee);

    @DeleteMapping("/api/employee/delete/{account}")
    Map<String, Object> deleteEmployee(@PathVariable("account") Integer account);
}
