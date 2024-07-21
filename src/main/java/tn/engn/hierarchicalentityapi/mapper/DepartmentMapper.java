package tn.engn.hierarchicalentityapi.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Department;

/**
 * Mapper class for converting between Department entities and DTOs.
 */
@Component
@Slf4j
public class DepartmentMapper extends AbstractHierarchyMapper<Department, HierarchyRequestDto, HierarchyResponseDto> {
    @Override
    protected boolean shouldFetchSubEntities() {
        return true;
    }

    @Override
    protected Department createNewEntityInstance() {
        return new Department();
    }

    @Override
    protected HierarchyResponseDto createNewResponseDtoInstance() {
        return new HierarchyResponseDto();
    }
}
