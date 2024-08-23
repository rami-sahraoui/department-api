package tn.engn.assignmentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.engn.assignmentapi.model.AssignmentMetadata;

/**
 * Repository interface for managing AssignmentMetadata entities.
 */
public interface AssignmentMetadataRepository extends JpaRepository<AssignmentMetadata, Long> {
}
