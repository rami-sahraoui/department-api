package tn.engn.hierarchicalentityapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.Department;

/**
 * Repository interface for Department entities.
 * Provides CRUD operations and custom query methods for the Department entity.
 */
@Repository
public interface DepartmentRepository extends HierarchyBaseRepository<Department> {}
