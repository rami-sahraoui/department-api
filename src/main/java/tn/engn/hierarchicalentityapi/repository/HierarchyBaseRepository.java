package tn.engn.hierarchicalentityapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

@NoRepositoryBean
public interface HierarchyBaseRepository<E extends HierarchyBaseEntity>
        extends JpaRepository<E, Long>, QuerydslPredicateExecutor<E> {
}
