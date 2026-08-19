package com.gym.serviceImpl;

import com.gym.common.UserContext;
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
    public List<Employee> getCoaches() {
        return employeeMapper.getEmployeesByStaff("教练");
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
        // 非管理员只能修改本人，且不能修改职务（防止员工自我提升越权）
        String role = UserContext.getRole();
        Integer currentAccount = UserContext.getAccount();
        if (role != null && !"ADMIN".equals(role)) {
            if (currentAccount == null || !currentAccount.equals(employee.getEmployeeAccount())) {
                throw new IllegalStateException("无权修改他人资料");
            }
            Employee origin = employeeMapper.getEmployeeByAccount(employee.getEmployeeAccount());
            if (origin != null) {
                employee.setStaff(origin.getStaff());
            }
        }

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
