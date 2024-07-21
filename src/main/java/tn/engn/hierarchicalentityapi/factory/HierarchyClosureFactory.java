package tn.engn.hierarchicalentityapi.factory;

import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntityClosure;

/**
 * Interface for creating instances of hierarchical entity closures.
 * <p>
 * This factory interface defines a method for creating closure entries,
 * which represent the hierarchical relationship between ancestor and descendant entities.
 *
 * @param <C> the type of the hierarchical entity closure
 */
public interface HierarchyClosureFactory<C extends HierarchyBaseEntityClosure<C>> {

    /**
     * Creates a new instance of a hierarchical entity closure.
     *
     * @param ancestorId   the ID of the ancestor entity
     * @param descendantId the ID of the descendant entity
     * @param level        the level of the relationship, representing the distance between the ancestor and descendant
     * @return a new instance of the hierarchical entity closure
     */
    C createClosure(Long ancestorId, Long descendantId, int level);
}
