package com.gym.mapper;

import com.gym.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminMapper {
    Admin getAdminByAccount(Integer adminAccount);
    List<Admin> getAllAdmins();
    void addAdmin(Admin admin);
    void updateAdmin(Admin admin);
    void deleteAdmin(Integer adminAccount);
}
