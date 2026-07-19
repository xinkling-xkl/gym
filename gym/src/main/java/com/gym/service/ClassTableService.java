package com.gym.service;

import com.gym.entity.ClassTable;

import java.util.List;

public interface ClassTableService {
    List<ClassTable> getAllClasses();
    ClassTable getClassById(Integer classId);
    void addClass(ClassTable classTable);
    void updateClass(ClassTable classTable);
    void deleteClass(Integer classId);
}
