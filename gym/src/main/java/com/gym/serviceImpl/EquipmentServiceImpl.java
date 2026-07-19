package com.gym.serviceImpl;

import com.gym.entity.Equipment;
import com.gym.mapper.EquipmentMapper;
import com.gym.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Override
    public List<Equipment> getAllEquipments() {
        return equipmentMapper.getAllEquipments();
    }

    @Override
    public Equipment getEquipmentById(Integer equipmentId) {
        return equipmentMapper.getEquipmentById(equipmentId);
    }

    @Override
    public void addEquipment(Equipment equipment) {
        equipmentMapper.addEquipment(equipment);
    }

    @Override
    public void updateEquipment(Equipment equipment) {
        equipmentMapper.updateEquipment(equipment);
    }

    @Override
    public void deleteEquipment(Integer equipmentId) {
        equipmentMapper.deleteEquipment(equipmentId);
    }
}
