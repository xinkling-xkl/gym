package com.gym.service;

import com.gym.entity.Admin;

public interface AdminService {
    Admin getAdminByAccount(Integer adminAccount);
    void addAdmin(Admin admin);
    void updateAdmin(Admin admin);
    void deleteAdmin(Integer adminAccount);
}
