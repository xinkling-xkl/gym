package com.gym.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.gym.common.Result;
import com.gym.entity.ClassTable;
import com.gym.service.ClassTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/class")
@CrossOrigin(origins = "*")
public class ClassTableController {

    @Autowired
    private ClassTableService classTableService;

    @GetMapping("/list")
    @SentinelResource(value = "class-list", blockHandler = "handleBlock")
    public Result<List<ClassTable>> getAllClasses() {
        List<ClassTable> classes = classTableService.getAllClasses();
        return Result.success(classes);
    }

    @GetMapping("/{id}")
    @SentinelResource(value = "class-get", blockHandler = "handleBlock")
    public Result<ClassTable> getClassById(@PathVariable Integer id) {
        ClassTable classTable = classTableService.getClassById(id);
        if (classTable != null) {
            return Result.success(classTable);
        }
        return Result.error(404, "课程不存在");
    }

    @PostMapping("/add")
    @SentinelResource(value = "class-add", blockHandler = "handleBlock")
    public Result<Void> addClass(@RequestBody ClassTable classTable) {
        classTableService.addClass(classTable);
        return Result.success("添加成功", null);
    }

    @PutMapping("/update")
    @SentinelResource(value = "class-update", blockHandler = "handleBlock")
    public Result<Void> updateClass(@RequestBody ClassTable classTable) {
        classTableService.updateClass(classTable);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/delete/{id}")
    @SentinelResource(value = "class-delete", blockHandler = "handleBlock")
    public Result<Void> deleteClass(@PathVariable Integer id) {
        classTableService.deleteClass(id);
        return Result.success("删除成功", null);
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
