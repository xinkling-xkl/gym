package com.gym.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "main-server", contextId = "classTableClient")
public interface ClassTableClient {

    @GetMapping("/api/class/list")
    Map<String, Object> getAllClasses();

    @GetMapping("/api/class/{id}")
    Map<String, Object> getClassById(@PathVariable("id") Integer id);

    @PostMapping("/api/class/add")
    Map<String, Object> addClass(@RequestBody Map<String, Object> classTable);

    @PutMapping("/api/class/update")
    Map<String, Object> updateClass(@RequestBody Map<String, Object> classTable);

    @DeleteMapping("/api/class/delete/{id}")
    Map<String, Object> deleteClass(@PathVariable("id") Integer id);
}
