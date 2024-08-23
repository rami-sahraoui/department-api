package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Job;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

/**
 * Service implementation for managing jobs using the Materialized Path Model.
 */
@Service
@Slf4j
public class MaterializedPathJobService extends MaterializedPathEntityService<Job, HierarchyRequestDto, HierarchyResponseDto> implements JobService {
    /**
     * Constructor for MaterializedPathEntityService.
     *
     * @param entityRepository the repository for the entity
     * @param entityMapper     the mapper for converting between entities and DTOs
     * @param jpaQueryFactory  the JPA query factory for executing queries
     */
    public MaterializedPathJobService(HierarchyBaseRepository<Job> entityRepository,
                                      HierarchyMapper<Job,HierarchyRequestDto,HierarchyResponseDto> entityMapper,
                                      JPAQueryFactory jpaQueryFactory) {
        super(entityRepository, entityMapper, jpaQueryFactory, Job.class);
    }
}
