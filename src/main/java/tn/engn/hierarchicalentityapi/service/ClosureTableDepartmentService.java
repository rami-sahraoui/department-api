package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.factory.DepartmentClosureFactory;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.model.DepartmentClosure;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseClosureRepository;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

/**
 * Service implementation for managing entities using the Closure Table Model.
 */
@Service
@Slf4j
public class ClosureTableDepartmentService extends ClosureTableEntityService<Department, DepartmentClosure, HierarchyRequestDto, HierarchyResponseDto> implements DepartmentService {
    /**
     * Constructor for ClosureTableEntityService.
     *
     * @param entityRepository        the repository for the entity
     * @param entityClosureRepository the repository for the entity closure
     * @param entityMapper            the mapper for converting between entities and DTOs
     * @param jpaQueryFactory         the JPA query factory for executing queries
     * @param departmentClosureFactory the factory for creating hierarchical entity closures instances
     */
    public ClosureTableDepartmentService(HierarchyBaseRepository<Department> entityRepository,
                                         HierarchyBaseClosureRepository<DepartmentClosure> entityClosureRepository,
                                         HierarchyMapper<Department, HierarchyRequestDto, HierarchyResponseDto> entityMapper,
                                         JPAQueryFactory jpaQueryFactory,
                                         DepartmentClosureFactory departmentClosureFactory) {
        super(entityRepository, entityClosureRepository, entityMapper, jpaQueryFactory, departmentClosureFactory, Department.class, DepartmentClosure.class);
    }
}
