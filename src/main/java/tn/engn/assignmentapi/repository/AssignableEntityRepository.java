package tn.engn.assignmentapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import tn.engn.assignmentapi.model.AssignableEntity;

/**
 * Repository interface for handling assignable entities.
 * This interface extends JpaRepository for CRUD operations and QuerydslPredicateExecutor for query capabilities.
 *
 * @param <E> the type of assignable entity
 */
@NoRepositoryBean
public interface AssignableEntityRepository<E extends AssignableEntity>
        extends JpaRepository<E, Long>, QuerydslPredicateExecutor<E> {
}
