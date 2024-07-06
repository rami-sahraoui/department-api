package tn.engn.departmentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.engn.departmentapi.model.Department;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, QuerydslPredicateExecutor<Department> {
    List<Department> findByParentDepartmentId(Long id);

    boolean existsByParentDepartmentId(Long id);

    List<Department> findByNameContaining(String name);

    List<Department> findByPathStartingWith(String path);
}
