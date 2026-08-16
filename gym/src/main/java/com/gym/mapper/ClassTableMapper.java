package com.gym.mapper;

import com.gym.entity.ClassTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ClassTableMapper {
    List<ClassTable> getAllClasses();
    /** 可预约课程：开课时间 >= 当前时间（未开课，未自动下架） */
    List<ClassTable> getAvailableClasses(@Param("now") LocalDateTime now);
    ClassTable getClassById(Integer classId);
    List<ClassTable> getClassesByCoach(String coach);
    void addClass(ClassTable classTable);
    void updateClass(ClassTable classTable);
    void deleteClass(Integer classId);
}
