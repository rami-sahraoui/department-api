package tn.engn.hierarchicalentityapi.repository;

import org.springframework.stereotype.Repository;
import tn.engn.hierarchicalentityapi.model.Job;

/**
 * Repository interface for Job entities.
 * Provides CRUD operations and custom query methods for the Job entity.
 */
@Repository
public interface JobRepository extends HierarchyBaseRepository<Job> {}
