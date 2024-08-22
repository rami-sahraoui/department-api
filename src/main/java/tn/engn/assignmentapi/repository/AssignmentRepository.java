package tn.engn.assignmentapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import tn.engn.assignmentapi.model.AssignableEntity;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Assignment entities with QueryDSL support.
 *
 * <p>This repository provides custom methods for querying assignments based on hierarchical entities and assignable entities.</p>
 *
 * @param <H> the type of the hierarchical entity, extending {@link HierarchyBaseEntity}
 * @param <E> the type of the assignable entity, extending {@link AssignableEntity}
 * @param <A> the type of the assignment, extending {@link Assignment}
 */
public interface AssignmentRepository<H extends HierarchyBaseEntity<H>, E extends AssignableEntity<E>, A extends Assignment<H,E>>
        extends JpaRepository<A, Long>, QuerydslPredicateExecutor<A> {

    /**
     * Finds an assignment by a given hierarchical entity and assignable entity.
     *
     * @param hierarchicalEntity the hierarchical entity instance
     * @param assignableEntity   the assignable entity instance
     * @return an {@link Optional} containing the found assignment, or empty if not found
     */
    Optional<A> findByHierarchicalEntityAndAssignableEntity(H hierarchicalEntity, E assignableEntity);

    /**
     * Checks if an assignment exists for a given hierarchical entity and assignable entity.
     *
     * @param hierarchicalEntity the hierarchical entity instance
     * @param assignableEntity   the assignable entity instance
     * @return true if such an assignment exists, false otherwise
     */
    boolean existsByHierarchicalEntityAndAssignableEntity(H hierarchicalEntity, E assignableEntity);

    /**
     * Finds all assignments by the class types of the hierarchical entity and the assignable entity.
     *
     * @param hierarchicalEntityClass the class type of the hierarchical entity
     * @param assignableEntityClass   the class type of the assignable entity
     * @return a list of assignments matching the specified classes
     */
    @Query("SELECT a FROM #{#entityName} a WHERE TYPE(a.hierarchicalEntity) = :hierarchicalEntityClass AND TYPE(a.assignableEntity) = :assignableEntityClass")
    List<A> findByHierarchicalEntityClassAndAssignableEntityClass(
            @Param("hierarchicalEntityClass") Class<H> hierarchicalEntityClass,
            @Param("assignableEntityClass") Class<E> assignableEntityClass);

    /**
     * Finds a paginated list of assignments by the class types of the hierarchical entity and the assignable entity.
     *
     * @param hierarchicalEntityClass the class type of the hierarchical entity
     * @param assignableEntityClass   the class type of the assignable entity
     * @param pageable                pagination information
     * @return a page of assignments matching the specified classes
     */
    @Query("SELECT a FROM #{#entityName} a WHERE TYPE(a.hierarchicalEntity) = :hierarchicalEntityClass AND TYPE(a.assignableEntity) = :assignableEntityClass")
    Page<A> findByHierarchicalEntityClassAndAssignableEntityClass(
            @Param("hierarchicalEntityClass") Class<H> hierarchicalEntityClass,
            @Param("assignableEntityClass") Class<E> assignableEntityClass,
            Pageable pageable);

    /**
     * Finds assignable entities associated with a given hierarchical entity.
     *
     * @param hierarchicalEntity the hierarchical entity instance
     * @return a list of assignable entities associated with the given hierarchical entity
     */
    @Query("SELECT a.assignableEntity FROM #{#entityName} a WHERE a.hierarchicalEntity = :hierarchicalEntity")
    List<E> findAssignableEntitiesByHierarchicalEntity(@Param("hierarchicalEntity") H hierarchicalEntity);

    /**
     * Finds assignable entities associated with a given hierarchical entity, while considering the type of assignable entity.
     *
     * @param assignableEntityClass the class type of the assignable entity
     * @param hierarchicalEntity    the hierarchical entity instance
     * @return a list of assignable entities associated with the given hierarchical entity and filtered by the assignable entity class
     */
    @Query("SELECT a.assignableEntity FROM #{#entityName} a WHERE TYPE(a.assignableEntity) = :assignableEntityClass AND a.hierarchicalEntity = :hierarchicalEntity")
    List<E> findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(
            @Param("assignableEntityClass") Class<E> assignableEntityClass,
            @Param("hierarchicalEntity") H hierarchicalEntity);

    /**
     * Finds hierarchical entities associated with a given assignable entity.
     *
     * @param assignableEntity the assignable entity instance
     * @return a list of hierarchical entities associated with the given assignable entity
     */
    @Query("SELECT a.hierarchicalEntity FROM #{#entityName} a WHERE a.assignableEntity = :assignableEntity")
    List<H> findHierarchicalEntitiesByAssignableEntity(@Param("assignableEntity") E assignableEntity);

    /**
     * Finds all hierarchical entities associated with a given assignable entity,
     * while considering the type of hierarchical entity.
     *
     * @param hierarchicalEntityClass the class type of the hierarchical entity
     * @param assignableEntity        the assignable entity instance
     * @return a list of hierarchical entities associated with the given assignable entity and filtered by the hierarchical entity class
     */
    @Query("SELECT a.hierarchicalEntity FROM #{#entityName} a WHERE TYPE(a.hierarchicalEntity) = :hierarchicalEntityClass AND a.assignableEntity = :assignableEntity")
    List<H> findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(
            @Param("hierarchicalEntityClass") Class<H> hierarchicalEntityClass,
            @Param("assignableEntity") E assignableEntity);

    /**
     * Finds a paginated list of assignable entities associated with a given hierarchical entity.
     *
     * @param hierarchicalEntity the hierarchical entity instance
     * @param pageable           pagination information
     * @return a page of assignable entities associated with the given hierarchical entity
     */
    @Query("SELECT a.assignableEntity FROM #{#entityName} a WHERE a.hierarchicalEntity = :hierarchicalEntity")
    Page<E> findAssignableEntitiesByHierarchicalEntity(@Param("hierarchicalEntity") H hierarchicalEntity, Pageable pageable);

    /**
     * Finds a paginated list of assignable entities associated with a given hierarchical entity,
     * while considering the type of assignable entity.
     *
     * @param assignableEntityClass the class type of the assignable entity
     * @param hierarchicalEntity    the hierarchical entity instance
     * @param pageable              the pagination information
     * @return a page of assignable entities associated with the given hierarchical entity and filtered by the assignable entity class
     */
    @Query("SELECT a.assignableEntity FROM #{#entityName} a WHERE TYPE(a.assignableEntity) = :assignableEntityClass AND a.hierarchicalEntity = :hierarchicalEntity")
    Page<E> findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(
            @Param("assignableEntityClass") Class<E> assignableEntityClass,
            @Param("hierarchicalEntity") H hierarchicalEntity,
            Pageable pageable);

    /**
     * Finds a paginated list of hierarchical entities associated with a given assignable entity.
     *
     * @param assignableEntity the assignable entity instance
     * @param pageable         pagination information
     * @return a page of hierarchical entities associated with the given assignable entity
     */
    @Query("SELECT a.hierarchicalEntity FROM #{#entityName} a WHERE a.assignableEntity = :assignableEntity")
    Page<H> findHierarchicalEntitiesByAssignableEntity(@Param("assignableEntity") E assignableEntity, Pageable pageable);

    /**
     * Finds a paginated list of hierarchical entities associated with a given assignable entity,
     * while considering the type of hierarchical entity.
     *
     * @param hierarchicalEntityClass the class type of the hierarchical entity
     * @param assignableEntity        the assignable entity instance
     * @param pageable                the pagination information
     * @return a page of hierarchical entities associated with the given assignable entity and filtered by the hierarchical entity class
     */
    @Query("SELECT a.hierarchicalEntity FROM #{#entityName} a WHERE TYPE(a.hierarchicalEntity) = :hierarchicalEntityClass AND a.assignableEntity = :assignableEntity")
    Page<H> findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(
            @Param("hierarchicalEntityClass") Class<H> hierarchicalEntityClass,
            @Param("assignableEntity") E assignableEntity,
            Pageable pageable);


    /**
     * Finds an assignment by the hierarchical entity ID and assignable entity ID.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityId   the ID of the assignable entity
     * @return an Optional containing the found assignment, or empty if no assignment is found
     */
    Optional<A> findByHierarchicalEntityIdAndAssignableEntityId(Long hierarchicalEntityId, Long assignableEntityId);

    /**
     * Finds all assignments associated with a given hierarchical entity.
     *
     * @param he the hierarchical entity
     * @return a list of assignments associated with the specified hierarchical entity
     */
    List<A> findByHierarchicalEntity(H he);

    /**
     * Finds all assignments associated with a given hierarchical entity, with pagination support.
     *
     * @param he       the hierarchical entity
     * @param pageable the pagination information
     * @return a page of assignments associated with the specified hierarchical entity
     */
    Page<A> findByHierarchicalEntity(H he, Pageable pageable);

    /**
     * Finds all assignments associated with a given assignable entity.
     *
     * @param ae the assignable entity
     * @return a list of assignments associated with the specified assignable entity
     */
    List<A> findByAssignableEntity(E ae);

    /**
     * Finds all assignments associated with a given assignable entity, with pagination support.
     *
     * @param ae       the assignable entity
     * @param pageable the pagination information
     * @return a page of assignments associated with the specified assignable entity
     */
    Page<A> findByAssignableEntity(E ae, Pageable pageable);

    /**
     * Finds all assignments associated with a given assignable entity and limited by the specified hierarchical entity class,
     * while also considering the type of assignable entity.
     *
     * @param hierarchicalEntityClass the class type of the hierarchical entity
     * @param assignableEntityClass  the class type of the assignable entity
     * @param ae the assignable entity
     * @return a list of assignments associated with the specified assignable entity and filtered by the hierarchical entity class
     */
    @Query("SELECT a FROM #{#entityName} a WHERE TYPE(a.hierarchicalEntity) = :hierarchicalEntityClass AND TYPE(a.assignableEntity) = :assignableEntityClass AND a.assignableEntity = :ae")
    List<A> findByHierarchicalEntityClassAndAssignableEntityClassAndAssignableEntity(
            @Param("hierarchicalEntityClass") Class<H> hierarchicalEntityClass,
            @Param("assignableEntityClass") Class<E> assignableEntityClass,
            @Param("ae") E ae);

    /**
     * Finds all assignments associated with a given assignable entity and limited by the specified hierarchical entity class,
     * with pagination support, while also considering the type of assignable entity.
     *
     * @param hierarchicalEntityClass the class type of the hierarchical entity
     * @param assignableEntityClass  the class type of the assignable entity
     * @param ae       the assignable entity
     * @param pageable the pagination information
     * @return a page of assignments associated with the specified assignable entity and filtered by the hierarchical entity class
     */
    @Query("SELECT a FROM #{#entityName} a WHERE TYPE(a.hierarchicalEntity) = :hierarchicalEntityClass AND TYPE(a.assignableEntity) = :assignableEntityClass AND a.assignableEntity = :ae")
    Page<Assignment<H,E>> findByHierarchicalEntityClassAndAssignableEntityClassAndAssignableEntity(
            @Param("hierarchicalEntityClass") Class<H> hierarchicalEntityClass,
            @Param("assignableEntityClass") Class<E> assignableEntityClass,
            @Param("ae") E ae,
            Pageable pageable);

    /**
     * Finds all assignments associated with a given hierarchical entity and limited by the specified hierarchical entity class,
     * while also considering the type of hierarchical entity.
     *
     * @param hierarchicalEntityClass the class type of the hierarchical entity
     * @param assignableEntityClass  the class type of the assignable entity
     * @param he the hierarchical entity
     * @return a list of assignments associated with the specified hierarchical entity and filtered by the hierarchical entity class
     */
    @Query("SELECT a FROM #{#entityName} a WHERE TYPE(a.hierarchicalEntity) = :hierarchicalEntityClass AND TYPE(a.assignableEntity) = :assignableEntityClass AND a.hierarchicalEntity = :he")
    List<A> findByHierarchicalEntityClassAndAssignableEntityClassAndHierarchicalEntity(
            @Param("hierarchicalEntityClass") Class<H> hierarchicalEntityClass,
            @Param("assignableEntityClass") Class<E> assignableEntityClass,
            @Param("he") H he);

    /**
     * Finds all assignments associated with a given hierarchical entity and limited by the specified hierarchical entity class,
     * with pagination support, while also considering the type of hierarchical entity.
     *
     * @param hierarchicalEntityClass the class type of the hierarchical entity
     * @param assignableEntityClass  the class type of the assignable entity
     * @param he       the hierarchical entity
     * @param pageable the pagination information
     * @return a page of assignments associated with the specified hierarchical entity and filtered by the hierarchical entity class
     */
    @Query("SELECT a FROM #{#entityName} a WHERE TYPE(a.hierarchicalEntity) = :hierarchicalEntityClass AND TYPE(a.assignableEntity) = :assignableEntityClass AND a.hierarchicalEntity = :he")
    Page<Assignment<H,E>> findByHierarchicalEntityClassAndAssignableEntityClassAndHierarchicalEntity(
            @Param("hierarchicalEntityClass") Class<H> hierarchicalEntityClass,
            @Param("assignableEntityClass") Class<E> assignableEntityClass,
            @Param("he") H he,
            Pageable pageable);
}
