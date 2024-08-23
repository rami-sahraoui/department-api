package tn.engn.hierarchicalentityapi.factory;

import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.model.TeamClosure;

/**
 * Factory for creating instances of {@link TeamClosure}.
 * <p>
 * This service provides a method to create closure entries for the Team entity,
 * which represent the hierarchical relationship between ancestor and descendant teams.
 */
@Service
public class TeamClosureFactory implements HierarchyClosureFactory<TeamClosure> {

    /**
     * Creates a new instance of a {@link TeamClosure}.
     *
     * @param ancestorId   the ID of the ancestor team
     * @param descendantId the ID of the descendant team
     * @param level        the level of the relationship, representing the distance between the ancestor and descendant
     * @return a new instance of the {@link TeamClosure}
     */
    @Override
    public TeamClosure createClosure(Long ancestorId, Long descendantId, int level) {
        // Construct and return a new TeamClosure instance with the provided parameters
        return TeamClosure.builder()
                .ancestorId(ancestorId)
                .descendantId(descendantId)
                .level(level)
                .build();
    }
}
