package tn.engn.hierarchicalentityapi.mapper;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tn.engn.hierarchicalentityapi.dto.DepartmentRequestDto;
import tn.engn.hierarchicalentityapi.dto.DepartmentResponseDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.model.QDepartment;

import java.util.List;
import java.util.stream.Collectors;

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
