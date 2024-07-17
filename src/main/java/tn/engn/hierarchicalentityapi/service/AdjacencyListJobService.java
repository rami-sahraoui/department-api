package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.JobMapper;
import tn.engn.hierarchicalentityapi.model.Job;
import tn.engn.hierarchicalentityapi.repository.JobRepository;

/**
 * Service implementation for managing departments using the adjacency list model.
 */
@Service
@Slf4j // Lombok annotation for logging
public class AdjacencyListJobService extends AdjacencyListEntityService<Job, HierarchyRequestDto, HierarchyResponseDto> implements JobService {
    /**
     * Constructs the service with necessary dependencies.
     *
     * @param entityRepository the repository for managing entities
     * @param entityMapper     the mapper for converting entities to DTOs and vice versa
     * @param jpaQueryFactory  the JPA query factory for executing dynamic queries
     */
    public AdjacencyListJobService(JobRepository entityRepository, JobMapper entityMapper, JPAQueryFactory jpaQueryFactory) {
        super(entityRepository, entityMapper, jpaQueryFactory, Job.class);
    }
}
