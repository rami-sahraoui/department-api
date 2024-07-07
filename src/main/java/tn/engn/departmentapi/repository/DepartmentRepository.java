package tn.engn.departmentapi.repository;

import tn.engn.departmentapi.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for Department entities.
 * Provides CRUD operations and custom query methods for the Department entity.
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByParentDepartmentId(Long parentId);

    List<Department> findByNameContainingIgnoreCase(String name);
}
