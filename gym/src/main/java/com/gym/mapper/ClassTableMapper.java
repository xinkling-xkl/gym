package com.gym.mapper;

import com.gym.entity.ClassTable;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ClassTableMapper {
    List<ClassTable> getAllClasses();
    ClassTable getClassById(Integer classId);
    void addClass(ClassTable classTable);
    void updateClass(ClassTable classTable);
    void deleteClass(Integer classId);
}
