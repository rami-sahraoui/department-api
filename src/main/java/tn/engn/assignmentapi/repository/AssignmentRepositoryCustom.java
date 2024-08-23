package tn.engn.assignmentapi.repository;

import tn.engn.assignmentapi.model.AssignableEntity;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

import java.util.List;

/**
 * Custom repository interface for Assignment entities.
 * This interface defines custom methods to be implemented using QueryDSL.
 *
 * @param <H> the type of the hierarchical entity
 * @param <E> the type of the assignable entity
 * @param <A> the type of the assignment entity
 */
public interface AssignmentRepositoryCustom<H extends HierarchyBaseEntity<H>, E extends AssignableEntity<E>, A extends Assignment<H, E>> {

    /**
     * Finds assignments by the class type of the hierarchical entity and the assignable entity.
     * This method uses QueryDSL for dynamic query generation.
     *
     * @param hierarchicalEntityClass the class of the hierarchical entity
     * @param assignableEntityClass   the class of the assignable entity
     * @return the list of assignments matching the specified class types
     */
    List<A> findByHierarchicalEntityClassAndAssignableEntityClass(Class<H> hierarchicalEntityClass, Class<E> assignableEntityClass);

    /**
     * Finds assignable entities by a given hierarchical entity.
     * This method uses QueryDSL for dynamic query generation.
     *
     * @param hierarchicalEntity the hierarchical entity
     * @return the list of assignable entities associated with the specified hierarchical entity
     */
    List<E> findAssignableEntitiesByHierarchicalEntity(H hierarchicalEntity);

    /**
     * Finds hierarchical entities by a given assignable entity.
     * This method uses QueryDSL for dynamic query generation.
     *
     * @param assignableEntity the assignable entity
     * @return the list of hierarchical entities associated with the specified assignable entity
     */
    List<H> findHierarchicalEntitiesByAssignableEntity(E assignableEntity);
}
