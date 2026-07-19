package com.gym.serviceImpl;

import com.gym.entity.Employee;
import com.gym.mapper.EmployeeMapper;
import com.gym.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public List<Employee> getAllEmployees() {
        return employeeMapper.getAllEmployees();
    }

    @Override
    public Employee getEmployeeByAccount(Integer employeeAccount) {
        return employeeMapper.getEmployeeByAccount(employeeAccount);
    }

    @Override
    public void addEmployee(Employee employee) {
        employeeMapper.addEmployee(employee);
    }

    @Override
    public void updateEmployee(Employee employee) {
        employeeMapper.updateEmployee(employee);
    }

    @Override
    public void deleteEmployee(Integer employeeAccount) {
        employeeMapper.deleteEmployee(employeeAccount);
    }
}
