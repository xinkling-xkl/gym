package com.gym.serviceImpl;

import com.gym.entity.Admin;
import com.gym.mapper.AdminMapper;
import com.gym.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public Admin getAdminByAccount(Integer adminAccount) {
        return adminMapper.getAdminByAccount(adminAccount);
    }

    @Override
    public void addAdmin(Admin admin) {
        adminMapper.addAdmin(admin);
    }

    @Override
    public void updateAdmin(Admin admin) {
        adminMapper.updateAdmin(admin);
    }

    @Override
    public void deleteAdmin(Integer adminAccount) {
        adminMapper.deleteAdmin(adminAccount);
    }
}
