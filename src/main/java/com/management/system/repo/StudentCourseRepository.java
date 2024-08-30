package com.management.system.repo;

import com.management.system.entity.StudentCourseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentCourseRepository extends BaseRepo<StudentCourseEntity> {

    List<StudentCourseEntity> findByStudentId(Long studentId);

    int deleteByCourseId(Long id);

}
