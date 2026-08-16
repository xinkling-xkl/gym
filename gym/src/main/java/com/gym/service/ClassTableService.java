package com.gym.service;

import com.gym.entity.ClassTable;

import java.util.List;

public interface ClassTableService {
    List<ClassTable> getAllClasses();
    /** 可预约课程（未开课） */
    List<ClassTable> getAvailableClasses();
    ClassTable getClassById(Integer classId);
    List<ClassTable> getClassesByCoach(String coach);
    void addClass(ClassTable classTable);
    void updateClass(ClassTable classTable);
    void deleteClass(Integer classId);
}
