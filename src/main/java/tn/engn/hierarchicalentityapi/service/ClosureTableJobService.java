package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.factory.JobClosureFactory;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Job;
import tn.engn.hierarchicalentityapi.model.JobClosure;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseClosureRepository;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

/**
 * Service implementation for managing entities using the Closure Table Model.
 */
@Service
@Slf4j
public class ClosureTableJobService extends ClosureTableEntityService<Job, JobClosure, HierarchyRequestDto, HierarchyResponseDto> implements JobService{
    /**
     * Constructor for ClosureTableEntityService.
     *
     * @param entityRepository        the repository for the entity
     * @param entityClosureRepository the repository for the entity closure
     * @param entityMapper            the mapper for converting between entities and DTOs
     * @param jpaQueryFactory         the JPA query factory for executing queries
     * @param jobClosureFactory the factory for creating hierarchical entity closures instances
     */
    public ClosureTableJobService(HierarchyBaseRepository<Job> entityRepository,
                                  HierarchyBaseClosureRepository<JobClosure> entityClosureRepository,
                                  HierarchyMapper<Job, HierarchyRequestDto, HierarchyResponseDto> entityMapper,
                                  JPAQueryFactory jpaQueryFactory,
                                  JobClosureFactory jobClosureFactory) {
        super(entityRepository, entityClosureRepository, entityMapper, jpaQueryFactory, jobClosureFactory, Job.class, JobClosure.class);
    }
}
