package tn.engn.hierarchicalentityapi.factory;

import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.model.JobClosure;

/**
 * Factory for creating instances of {@link JobClosure}.
 * <p>
 * This service provides a method to create closure entries for the Job entity,
 * which represent the hierarchical relationship between ancestor and descendant jobs.
 */
@Service
public class JobClosureFactory implements HierarchyClosureFactory<JobClosure> {

    /**
     * Creates a new instance of a {@link JobClosure}.
     *
     * @param ancestorId   the ID of the ancestor job
     * @param descendantId the ID of the descendant job
     * @param level        the level of the relationship, representing the distance between the ancestor and descendant
     * @return a new instance of the {@link JobClosure}
     */
    @Override
    public JobClosure createClosure(Long ancestorId, Long descendantId, int level) {
        // Construct and return a new JobClosure instance with the provided parameters
        return JobClosure.builder()
                .ancestorId(ancestorId)
                .descendantId(descendantId)
                .level(level)
                .build();
    }
}
