package tn.engn.hierarchicalentityapi.mapper;

import org.springframework.stereotype.Component;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Team;

@Component
public class TeamMapper extends  AbstractHierarchyMapper<Team, HierarchyRequestDto, HierarchyResponseDto> {
    @Override
    protected boolean shouldFetchSubEntities() {
        return true;
    }

    @Override
    protected Team createNewEntityInstance() {
        return new Team();
    }

    @Override
    protected HierarchyResponseDto createNewResponseDtoInstance() {
        return new HierarchyResponseDto();
    }
}
