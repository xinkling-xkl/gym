package com.gym.serviceImpl;

import com.gym.entity.Employee;
import com.gym.mapper.EmployeeMapper;
import com.gym.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private boolean isEncrypted(String pwd) {
        return pwd != null && (pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$"));
    }

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
        if (employee.getEmployeePassword() != null && !isEncrypted(employee.getEmployeePassword())) {
            employee.setEmployeePassword(passwordEncoder.encode(employee.getEmployeePassword()));
        }
        employeeMapper.addEmployee(employee);
    }

    @Override
    public void updateEmployee(Employee employee) {
        // 全字段更新：密码为空或已加密则回填原密码，明文则加密
        if (employee.getEmployeePassword() == null || employee.getEmployeePassword().isEmpty()
                || isEncrypted(employee.getEmployeePassword())) {
            Employee origin = employeeMapper.getEmployeeByAccount(employee.getEmployeeAccount());
            employee.setEmployeePassword(origin != null ? origin.getEmployeePassword() : null);
        } else {
            employee.setEmployeePassword(passwordEncoder.encode(employee.getEmployeePassword()));
        }
        employeeMapper.updateEmployee(employee);
    }

    @Override
    public void deleteEmployee(Integer employeeAccount) {
        employeeMapper.deleteEmployee(employeeAccount);
    }
}
