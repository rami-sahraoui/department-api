package tn.engn.assignmentapi.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import tn.engn.assignmentapi.model.AssignableEntity;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.assignmentapi.model.QAssignment;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

import java.util.List;

/**
 * Implementation of custom repository methods for Assignment entities using QueryDSL.
 *
 * @param <H> the type of the hierarchical entity
 * @param <E> the type of the assignable entity
 * @param <A> the type of the assignment entity
 */
@Repository
public class AssignmentRepositoryImpl<H extends HierarchyBaseEntity<H>, E extends AssignableEntity<E>, A extends Assignment<H, E>>
        implements AssignmentRepositoryCustom<H, E, A> {

    private final JPAQueryFactory queryFactory;

    /**
     * Constructor for AssignmentRepositoryImpl.
     *
     * @param queryFactory the JPAQueryFactory used for building QueryDSL queries
     */
    public AssignmentRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    /**
     * Finds assignments by the class type of the hierarchical entity and the assignable entity.
     * This method uses QueryDSL to dynamically construct and execute the query.
     *
     * @param hierarchicalEntityClass the class of the hierarchical entity
     * @param assignableEntityClass   the class of the assignable entity
     * @return the list of assignments matching the specified class types
     */
    @Override
    public List<A> findByHierarchicalEntityClassAndAssignableEntityClass(Class<H> hierarchicalEntityClass, Class<E> assignableEntityClass) {
        QAssignment assignment = QAssignment.assignment;

        // Build and execute the QueryDSL query
        return (List<A>) queryFactory.selectFrom(assignment)
                .where(assignment.hierarchicalEntity.instanceOf(hierarchicalEntityClass)
                        .and(assignment.assignableEntity.instanceOf(assignableEntityClass)))
                .fetch();
    }

    /**
     * Finds assignable entities by a given hierarchical entity.
     * This method uses QueryDSL to dynamically construct and execute the query.
     *
     * @param hierarchicalEntity the hierarchical entity
     * @return the list of assignable entities associated with the specified hierarchical entity
     */
    @Override
    public List<E> findAssignableEntitiesByHierarchicalEntity(H hierarchicalEntity) {
        QAssignment assignment = QAssignment.assignment;

        // Build and execute the QueryDSL query
        return (List<E>) queryFactory.select(assignment.assignableEntity)
                .from(assignment)
                .where(assignment.hierarchicalEntity.eq(hierarchicalEntity))
                .fetch();
    }

    /**
     * Finds hierarchical entities by a given assignable entity.
     * This method uses QueryDSL to dynamically construct and execute the query.
     *
     * @param assignableEntity the assignable entity
     * @return the list of hierarchical entities associated with the specified assignable entity
     */
    @Override
    public List<H> findHierarchicalEntitiesByAssignableEntity(E assignableEntity) {
        QAssignment assignment = QAssignment.assignment;

        // Build and execute the QueryDSL query
        return (List<H>) queryFactory.select(assignment.hierarchicalEntity)
                .from(assignment)
                .where(assignment.assignableEntity.eq(assignableEntity))
                .fetch();
    }
}
