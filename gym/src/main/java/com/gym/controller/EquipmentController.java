package com.gym.controller;

import com.gym.entity.Equipment;
import com.gym.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipment")
@CrossOrigin(origins = "*")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @GetMapping("/list")
    public Map<String, Object> getAllEquipments() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Equipment> equipments = equipmentService.getAllEquipments();
            result.put("code", 200);
            result.put("data", equipments);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getEquipmentById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Equipment equipment = equipmentService.getEquipmentById(id);
            if (equipment != null) {
                result.put("code", 200);
                result.put("data", equipment);
                result.put("message", "查询成功");
            } else {
                result.put("code", 404);
                result.put("message", "器材不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> addEquipment(@RequestBody Equipment equipment) {
        Map<String, Object> result = new HashMap<>();
        try {
            equipmentService.addEquipment(equipment);
            result.put("code", 200);
            result.put("message", "添加成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> updateEquipment(@RequestBody Equipment equipment) {
        Map<String, Object> result = new HashMap<>();
        try {
            equipmentService.updateEquipment(equipment);
            result.put("code", 200);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteEquipment(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            equipmentService.deleteEquipment(id);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }
}
