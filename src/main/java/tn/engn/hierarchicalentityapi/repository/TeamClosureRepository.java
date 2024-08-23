package tn.engn.hierarchicalentityapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.TeamClosure;

/**
 * Repository interface for TeamClosure entities.
 * Provides CRUD operations and custom query methods for the TeamClosure entity.
 */
@Repository
public interface TeamClosureRepository extends HierarchyBaseClosureRepository<TeamClosure> {}
