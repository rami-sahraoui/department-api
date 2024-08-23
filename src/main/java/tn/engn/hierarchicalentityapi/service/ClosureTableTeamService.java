package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.factory.TeamClosureFactory;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.Team;
import tn.engn.hierarchicalentityapi.model.TeamClosure;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseClosureRepository;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

/**
 * Service implementation for managing entities using the Closure Table Model.
 */
@Service
@Slf4j
public class ClosureTableTeamService extends ClosureTableEntityService<Team, TeamClosure, HierarchyRequestDto, HierarchyResponseDto> implements TeamService {
    /**
     * Constructor for ClosureTableEntityService.
     *
     * @param entityRepository        the repository for the entity
     * @param entityClosureRepository the repository for the entity closure
     * @param entityMapper            the mapper for converting between entities and DTOs
     * @param jpaQueryFactory         the JPA query factory for executing queries
     * @param teamClosureFactory the factory for creating hierarchical entity closures instances
     */
    public ClosureTableTeamService(HierarchyBaseRepository<Team> entityRepository,
                                   HierarchyBaseClosureRepository<TeamClosure> entityClosureRepository,
                                   HierarchyMapper<Team, HierarchyRequestDto, HierarchyResponseDto> entityMapper,
                                   JPAQueryFactory jpaQueryFactory,
                                   TeamClosureFactory teamClosureFactory) {
        super(entityRepository, entityClosureRepository, entityMapper, jpaQueryFactory, teamClosureFactory, Team.class, TeamClosure.class);
    }
}
