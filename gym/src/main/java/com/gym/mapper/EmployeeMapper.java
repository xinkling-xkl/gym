package com.gym.mapper;

import com.gym.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmployeeMapper {
    List<Employee> getAllEmployees();
    Employee getEmployeeByAccount(Integer employeeAccount);
    Employee getEmployeeByName(String employeeName);
    List<Employee> getEmployeesByStaff(String staff);
    void addEmployee(Employee employee);
    void updateEmployee(Employee employee);
    void deleteEmployee(Integer employeeAccount);
}
