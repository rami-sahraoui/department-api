package tn.engn.hierarchicalentityapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.ProjectClosure;

/**
 * Repository interface for ProjectClosure entities.
 * Provides CRUD operations and custom query methods for the ProjectClosure entity.
 */
@Repository
public interface ProjectClosureRepository extends HierarchyBaseClosureRepository<ProjectClosure> {}
