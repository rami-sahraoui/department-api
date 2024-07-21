package tn.engn.hierarchicalentityapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntityClosure;

@NoRepositoryBean
public interface HierarchyBaseClosureRepository<C extends HierarchyBaseEntityClosure>
        extends JpaRepository<C, Long>, QuerydslPredicateExecutor<C> {
}
