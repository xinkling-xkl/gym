package com.gym.service;

import com.gym.entity.Equipment;

import java.util.List;

public interface EquipmentService {
    List<Equipment> getAllEquipments();
    Equipment getEquipmentById(Integer equipmentId);
    void addEquipment(Equipment equipment);
    void updateEquipment(Equipment equipment);
    void deleteEquipment(Integer equipmentId);
}
