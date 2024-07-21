package tn.engn.hierarchicalentityapi.factory;

import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.model.DepartmentClosure;

/**
 * Factory for creating instances of {@link DepartmentClosure}.
 * <p>
 * This service provides a method to create closure entries for the Department entity,
 * which represent the hierarchical relationship between ancestor and descendant departments.
 */
@Service
public class DepartmentClosureFactory implements HierarchyClosureFactory<DepartmentClosure> {

    /**
     * Creates a new instance of a {@link DepartmentClosure}.
     *
     * @param ancestorId   the ID of the ancestor department
     * @param descendantId the ID of the descendant department
     * @param level        the level of the relationship, representing the distance between the ancestor and descendant
     * @return a new instance of the {@link DepartmentClosure}
     */
    @Override
    public DepartmentClosure createClosure(Long ancestorId, Long descendantId, int level) {
        // Construct and return a new DepartmentClosure instance with the provided parameters
        return DepartmentClosure.builder()
                .ancestorId(ancestorId)
                .descendantId(descendantId)
                .level(level)
                .build();
    }
}
