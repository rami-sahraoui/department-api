package tn.engn.hierarchicalentityapi.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
