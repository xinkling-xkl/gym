package com.gym.mapper;

import com.gym.entity.Equipment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EquipmentMapper {
    List<Equipment> getAllEquipments();
    Equipment getEquipmentById(Integer equipmentId);
    void addEquipment(Equipment equipment);
    void updateEquipment(Equipment equipment);
    void deleteEquipment(Integer equipmentId);
}
