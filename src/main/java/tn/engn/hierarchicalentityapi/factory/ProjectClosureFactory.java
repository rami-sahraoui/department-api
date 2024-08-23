package tn.engn.hierarchicalentityapi.factory;

import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.model.ProjectClosure;

/**
 * Factory for creating instances of {@link ProjectClosure}.
 * <p>
 * This service provides a method to create closure entries for the Project entity,
 * which represent the hierarchical relationship between ancestor and descendant projects.
 */
@Service
public class ProjectClosureFactory implements HierarchyClosureFactory<ProjectClosure> {

    /**
     * Creates a new instance of a {@link ProjectClosure}.
     *
     * @param ancestorId   the ID of the ancestor project
     * @param descendantId the ID of the descendant project
     * @param level        the level of the relationship, representing the distance between the ancestor and descendant
     * @return a new instance of the {@link ProjectClosure}
     */
    @Override
    public ProjectClosure createClosure(Long ancestorId, Long descendantId, int level) {
        // Construct and return a new ProjectClosure instance with the provided parameters
        return ProjectClosure.builder()
                .ancestorId(ancestorId)
                .descendantId(descendantId)
                .level(level)
                .build();
    }
}
