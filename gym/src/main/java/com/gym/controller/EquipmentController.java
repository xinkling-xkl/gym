package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.common.Result;
import com.gym.entity.Equipment;
import com.gym.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@CrossOrigin(origins = "*")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    @GetMapping("/list")
    @SentinelResource(value = "equipment-list", blockHandler = "handleBlock")
    public Result<List<Equipment>> getAllEquipments() {
        List<Equipment> equipments = equipmentService.getAllEquipments();
        return Result.success(equipments);
    }

    @GetMapping("/{id}")
    @SentinelResource(value = "equipment-get", blockHandler = "handleBlock")
    public Result<Equipment> getEquipmentById(@PathVariable Integer id) {
        Equipment equipment = equipmentService.getEquipmentById(id);
        if (equipment != null) {
            return Result.success(equipment);
        }
        return Result.error(404, "器材不存在");
    }

    @PostMapping("/add")
    @SentinelResource(value = "equipment-add", blockHandler = "handleBlock")
    public Result<Void> addEquipment(@RequestBody Equipment equipment) {
        equipmentService.addEquipment(equipment);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update")
    @SentinelResource(value = "equipment-update", blockHandler = "handleBlock")
    public Result<Void> updateEquipment(@RequestBody Equipment equipment) {
        equipmentService.updateEquipment(equipment);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/delete/{id}")
    @SentinelResource(value = "equipment-delete", blockHandler = "handleBlock")
    public Result<Void> deleteEquipment(@PathVariable Integer id) {
        equipmentService.deleteEquipment(id);
        return Result.success("删除成功", null);
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
