package tn.engn.hierarchicalentityapi.config;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.model.Job;

/**
 * Configuration class for QueryDSL.
 */
@Configuration
public class QueryDSLConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Provides a JPAQueryFactory bean.
     *
     * @return the JPAQueryFactory bean
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    @Bean
    public EntityPathBase<Department> departmentEntityPathBase() {
        return new EntityPathBase<>(Department.class, "department");
    }

    @Bean
    public EntityPathBase<Job> jobEntityPathBase() {
        return new EntityPathBase<>(Job.class, "job");
    }
}
