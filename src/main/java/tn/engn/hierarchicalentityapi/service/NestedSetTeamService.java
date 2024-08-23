package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Team;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

/**
 * Service implementation for managing teams using the Nested Set Model.
 */
@Service
@Slf4j
public class NestedSetTeamService extends NestedSetEntityService<Team, HierarchyRequestDto, HierarchyResponseDto>
implements TeamService {
    /**
     * Constructor for NestedSetEntityService.
     *
     * @param entityRepository the repository for the entity
     * @param entityMapper     the mapper for converting between entities and DTOs
     * @param jpaQueryFactory  the JPA query factory for executing queries
     */
    public NestedSetTeamService(HierarchyBaseRepository<Team> entityRepository, HierarchyMapper<Team, HierarchyRequestDto, HierarchyResponseDto> entityMapper, JPAQueryFactory jpaQueryFactory) {
        super(entityRepository, entityMapper, jpaQueryFactory, Team.class);
    }
}
