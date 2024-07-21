package tn.engn.hierarchicalentityapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.JobClosure;

/**
 * Repository interface for JobClosure entities.
 * Provides CRUD operations and custom query methods for the JobClosure entity.
 */
@Repository
public interface JobClosureRepository extends HierarchyBaseClosureRepository<JobClosure> {}
