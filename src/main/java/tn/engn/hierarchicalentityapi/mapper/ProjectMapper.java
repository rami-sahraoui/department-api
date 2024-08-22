package tn.engn.hierarchicalentityapi.mapper;

import org.springframework.stereotype.Component;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Project;

@Component
public class ProjectMapper extends  AbstractHierarchyMapper<Project, HierarchyRequestDto, HierarchyResponseDto> {
    @Override
    protected boolean shouldFetchSubEntities() {
        return true;
    }

    @Override
    protected Project createNewEntityInstance() {
        return new Project();
    }

    @Override
    protected HierarchyResponseDto createNewResponseDtoInstance() {
        return new HierarchyResponseDto();
    }
}
