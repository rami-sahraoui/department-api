package tn.engn.hierarchicalentityapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.Department;

import java.util.List;

@Repository
public interface DepartmentRepository extends HierarchyBaseRepository<Department> {}
