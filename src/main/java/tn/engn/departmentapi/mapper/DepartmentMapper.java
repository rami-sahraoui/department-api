package tn.engn.departmentapi.mapper;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.model.QDepartment;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class for converting between Department entities and DTOs.
 */
@Component
@Slf4j
public class DepartmentMapper {

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

    public Department toEntity(DepartmentRequestDto departmentRequestDto) {
        if (departmentRequestDto == null) {
            return null;
        }

        Department department = Department.builder()
                .id(departmentRequestDto.getId())
                .name(departmentRequestDto.getName())
                .parentDepartmentId(departmentRequestDto.getParentDepartmentId())
                .build();

        return department;
    }

    public DepartmentResponseDto toDto(Department department) {
        if (department == null) {
            return null;
        }

        DepartmentResponseDto departmentResponseDto = DepartmentResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .parentDepartmentId(department.getParentDepartmentId())
                .build();

        populateSubDepartments(departmentResponseDto, department);

        return departmentResponseDto;
    }

    public List<DepartmentResponseDto> toDtoList(List<Department> departments) {
        return departments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    protected void populateSubDepartments(DepartmentResponseDto dto, Department department) {
        // Logging to verify method invoked
        log.info("Populating sub-departments for department: {}", department.getId());

        QDepartment qDepartment = QDepartment.department;

        // Refresh the department to get the latest state
        Department refreshedDepartment = jpaQueryFactory.selectFrom(qDepartment)
                .where(qDepartment.id.eq(department.getId()))
                .fetchOne();

        List<Department> subDepartments = jpaQueryFactory.selectFrom(qDepartment)
                .where(qDepartment.parentDepartmentId.eq(refreshedDepartment.getId()))
                .fetch();

        List<DepartmentResponseDto> subDepartmentDtos = subDepartments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        dto.setSubDepartments(subDepartmentDtos);

        // Logging to verify sub-department mapping
        log.info("Populated sub-departments for department ID {}: {}", department.getId(), subDepartmentDtos);
    }
}
