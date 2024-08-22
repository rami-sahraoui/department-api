package tn.engn.hierarchicalentityapi.service;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.mapper.TeamMapper;
import tn.engn.hierarchicalentityapi.model.Team;
import tn.engn.hierarchicalentityapi.repository.TeamRepository;

/**
 * Service implementation for managing teams using the adjacency list model.
 */
@Service
@Slf4j // Lombok annotation for logging
public class AdjacencyLisTeamService extends AdjacencyListEntityService<Team, HierarchyRequestDto, HierarchyResponseDto> implements TeamService {

    /**
     * Constructs the service with necessary dependencies.
     *
     * @param entityRepository   the repository for managing entities
     * @param entityMapper       the mapper for converting entities to DTOs and vice versa
     * @param jpaQueryFactory    the JPA query factory for executing dynamic queries
     */
    public AdjacencyLisTeamService(TeamRepository entityRepository, TeamMapper entityMapper, JPAQueryFactory jpaQueryFactory) {
        super(entityRepository, entityMapper, jpaQueryFactory, Team.class);
    }
}
