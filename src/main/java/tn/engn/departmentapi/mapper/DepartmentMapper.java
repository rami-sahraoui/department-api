package tn.engn.departmentapi.mapper;

import org.mapstruct.*;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.model.Department;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    @Mapping(target = "parentDepartment.id", source = "parentDepartmentId")
    Department toEntity(DepartmentRequestDto departmentRequestDto);

    @Mapping(source = "parentDepartment.id", target = "parentDepartmentId")
    DepartmentResponseDto toDto(Department department);

    List<DepartmentResponseDto> toDtoList(List<Department> departments);

    @AfterMapping
    default void setSubDepartments(@MappingTarget DepartmentResponseDto departmentResponseDto, Department department) {
        departmentResponseDto.setSubDepartments(toDtoList(department.getSubDepartments()));
    }
}
