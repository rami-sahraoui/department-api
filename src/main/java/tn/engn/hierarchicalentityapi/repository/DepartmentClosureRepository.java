package tn.engn.hierarchicalentityapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.DepartmentClosure;

/**
 * Repository interface for DepartmentClosure entities.
 * Provides CRUD operations and custom query methods for the DepartmentClosure entity.
 */
@Repository
public interface DepartmentClosureRepository extends HierarchyBaseClosureRepository<DepartmentClosure> {}
