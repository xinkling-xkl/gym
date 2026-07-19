package com.gym.controller;

import com.gym.entity.ClassTable;
import com.gym.service.ClassTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/class")
@CrossOrigin(origins = "*")
public class ClassTableController {

    @Autowired
    private ClassTableService classTableService;

    @GetMapping("/list")
    public Map<String, Object> getAllClasses() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ClassTable> classes = classTableService.getAllClasses();
            result.put("code", 200);
            result.put("data", classes);
            result.put("message", "查询成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getClassById(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            ClassTable classTable = classTableService.getClassById(id);
            if (classTable != null) {
                result.put("code", 200);
                result.put("data", classTable);
                result.put("message", "查询成功");
            } else {
                result.put("code", 404);
                result.put("message", "课程不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询失败：" + e.getMessage());
        }
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> addClass(@RequestBody ClassTable classTable) {
        Map<String, Object> result = new HashMap<>();
        try {
            classTableService.addClass(classTable);
            result.put("code", 200);
            result.put("message", "添加成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "添加失败：" + e.getMessage());
        }
        return result;
    }

    @PutMapping("/update")
    public Map<String, Object> updateClass(@RequestBody ClassTable classTable) {
        Map<String, Object> result = new HashMap<>();
        try {
            classTableService.updateClass(classTable);
            result.put("code", 200);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "更新失败：" + e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteClass(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            classTableService.deleteClass(id);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }
}
