package tn.engn.departmentapi.mapper;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class DepartmentMapper {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * Converts a DepartmentRequestDto to a Department entity.
     *
     * @param departmentRequestDto the DepartmentRequestDto to convert
     * @return the converted Department entity
     */
    public Department toEntity(DepartmentRequestDto departmentRequestDto) {
        if (departmentRequestDto == null) {
            return null;
        }

        // Building Department entity from DTO
        Department department = Department.builder()
                .id(departmentRequestDto.getId())
                .name(departmentRequestDto.getName())
                .parentDepartmentId(departmentRequestDto.getParentDepartmentId())
                .build();

        return department;
    }

    /**
     * Converts a Department entity to a DepartmentResponseDto.
     *
     * @param department the Department entity to convert
     * @return the converted DepartmentResponseDto
     */
    public DepartmentResponseDto toDto(Department department) {
        if (department == null) {
            return null;
        }

        // Building DepartmentResponseDto from entity
        DepartmentResponseDto departmentResponseDto = DepartmentResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .parentDepartmentId(department.getParentDepartmentId())
                .build();

        // Populating sub-departments
        populateSubDepartments(departmentResponseDto, department);

        return departmentResponseDto;
    }

    /**
     * Converts a list of Department entities to a list of DepartmentResponseDtos.
     *
     * @param departments the list of Department entities to convert
     * @return the list of converted DepartmentResponseDtos
     */
    public List<DepartmentResponseDto> toDtoList(List<Department> departments) {
        return departments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Populates the sub-departments of a given DepartmentResponseDto.
     *
     * @param dto the DepartmentResponseDto to populate
     * @param department the Department entity to fetch sub-departments from
     */
    protected void populateSubDepartments(DepartmentResponseDto dto, Department department) {
        // Logging to verify method invocation
        log.info("Populating sub-departments for department: {}", department.getId());

        QDepartment qDepartment = QDepartment.department;

        // Refresh the department to get the latest state
        Department refreshedDepartment = jpaQueryFactory.selectFrom(qDepartment)
                .where(qDepartment.id.eq(department.getId()))
                .fetchOne();

        // Fetching sub-departments
        List<Department> subDepartments = jpaQueryFactory.selectFrom(qDepartment)
                .where(qDepartment.parentDepartmentId.eq(refreshedDepartment.getId()))
                .fetch();

        // Converting sub-departments to DTOs
        List<DepartmentResponseDto> subDepartmentDtos = subDepartments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        dto.setSubDepartments(subDepartmentDtos);

        // Logging to verify sub-department mapping
        log.info("Populated sub-departments for department ID {}: {}", department.getId(), subDepartmentDtos);
    }
}
