package com.gym.serviceImpl;

import com.gym.entity.Admin;
import com.gym.mapper.AdminMapper;
import com.gym.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Admin getAdminByAccount(Integer adminAccount) {
        return adminMapper.getAdminByAccount(adminAccount);
    }

    private boolean isEncrypted(String pwd) {
        return pwd != null && (pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$"));
    }

    @Override
    public void addAdmin(Admin admin) {
        if (admin.getAdminPassword() != null && !isEncrypted(admin.getAdminPassword())) {
            admin.setAdminPassword(passwordEncoder.encode(admin.getAdminPassword()));
        }
        adminMapper.addAdmin(admin);
    }

    @Override
    public void updateAdmin(Admin admin) {
        if (admin.getAdminPassword() != null && !isEncrypted(admin.getAdminPassword())) {
            admin.setAdminPassword(passwordEncoder.encode(admin.getAdminPassword()));
        }
        adminMapper.updateAdmin(admin);
    }

    @Override
    public void deleteAdmin(Integer adminAccount) {
        adminMapper.deleteAdmin(adminAccount);
    }
}
