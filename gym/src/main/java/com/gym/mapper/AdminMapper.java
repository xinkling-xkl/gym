package com.gym.mapper;

import com.gym.entity.Admin;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper {
    Admin getAdminByAccount(Integer adminAccount);
    void addAdmin(Admin admin);
    void updateAdmin(Admin admin);
    void deleteAdmin(Integer adminAccount);
}
