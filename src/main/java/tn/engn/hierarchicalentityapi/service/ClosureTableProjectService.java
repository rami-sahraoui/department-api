package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.factory.ProjectClosureFactory;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Project;
import tn.engn.hierarchicalentityapi.model.ProjectClosure;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseClosureRepository;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

/**
 * Service implementation for managing entities using the Closure Table Model.
 */
@Service
@Slf4j
public class ClosureTableProjectService extends ClosureTableEntityService<Project, ProjectClosure, HierarchyRequestDto, HierarchyResponseDto> implements ProjectService {
    /**
     * Constructor for ClosureTableEntityService.
     *
     * @param entityRepository        the repository for the entity
     * @param entityClosureRepository the repository for the entity closure
     * @param entityMapper            the mapper for converting between entities and DTOs
     * @param jpaQueryFactory         the JPA query factory for executing queries
     * @param projectClosureFactory the factory for creating hierarchical entity closures instances
     */
    public ClosureTableProjectService(HierarchyBaseRepository<Project> entityRepository,
                                      HierarchyBaseClosureRepository<ProjectClosure> entityClosureRepository,
                                      HierarchyMapper<Project, HierarchyRequestDto, HierarchyResponseDto> entityMapper,
                                      JPAQueryFactory jpaQueryFactory,
                                      ProjectClosureFactory projectClosureFactory) {
        super(entityRepository, entityClosureRepository, entityMapper, jpaQueryFactory, projectClosureFactory, Project.class, ProjectClosure.class);
    }
}
