package com.gym.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "main-server", contextId = "equipmentClient")
public interface EquipmentClient {

    @GetMapping("/api/equipment/list")
    Map<String, Object> getAllEquipments();

    @GetMapping("/api/equipment/{id}")
    Map<String, Object> getEquipment(@PathVariable("id") Integer id);

    @PostMapping("/api/equipment/add")
    Map<String, Object> addEquipment(@RequestBody Map<String, Object> equipment);

    @PutMapping("/api/equipment/update")
    Map<String, Object> updateEquipment(@RequestBody Map<String, Object> equipment);

    @DeleteMapping("/api/equipment/delete/{id}")
    Map<String, Object> deleteEquipment(@PathVariable("id") Integer id);
}
