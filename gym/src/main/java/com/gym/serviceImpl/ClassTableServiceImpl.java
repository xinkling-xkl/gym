package com.gym.serviceImpl;

import com.gym.entity.ClassTable;
import com.gym.mapper.ClassTableMapper;
import com.gym.service.ClassTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassTableServiceImpl implements ClassTableService {

    @Autowired
    private ClassTableMapper classTableMapper;

    @Override
    public List<ClassTable> getAllClasses() {
        return classTableMapper.getAllClasses();
    }

    @Override
    public ClassTable getClassById(Integer classId) {
        return classTableMapper.getClassById(classId);
    }

    @Override
    public void addClass(ClassTable classTable) {
        classTableMapper.addClass(classTable);
    }

    @Override
    public void updateClass(ClassTable classTable) {
        classTableMapper.updateClass(classTable);
    }

    @Override
    public void deleteClass(Integer classId) {
        classTableMapper.deleteClass(classId);
    }
}
