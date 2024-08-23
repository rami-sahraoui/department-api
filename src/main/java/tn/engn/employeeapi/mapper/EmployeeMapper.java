package tn.engn.employeeapi.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.model.Employee;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Employee entities and DTOs.
 */
@Component
@Slf4j
public class EmployeeMapper implements AssignableEntityMapper<Employee, EmployeeRequestDto, EmployeeResponseDto> {

    /**
     * Converts EmployeeRequestDto to Employee entity.
     * @param dto the EmployeeRequestDto
     * @return the Employee entity
     */
    public Employee toEntity(EmployeeRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return Employee.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .dateOfBirth(dto.getDateOfBirth())
                .position(dto.getPosition())
                .build();
    }

    /**
     * Converts Employee entity to EmployeeResponseDto.
     * @param entity the Employee entity
     * @return the EmployeeResponseDto
     */
    public EmployeeResponseDto toDto(Employee entity) {
        if (entity == null) {
            return null;
        }

        return EmployeeResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .dateOfBirth(entity.getDateOfBirth())
                .position(entity.getPosition())
                .build();
    }

    /**
     * Converts a list of Employee entities to a list of EmployeeResponseDto.
     * @param entities the list of Employee entities
     * @return the list of EmployeeResponseDto
     */
    public List<EmployeeResponseDto> toDtoList(List<Employee> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Converts a Page of Employee entities to PaginatedResponseDto containing a list of EmployeeResponseDto.
     * @param page the Page of Employee entities
     * @return the PaginatedResponseDto containing the list of EmployeeResponseDto
     */
    public PaginatedResponseDto<EmployeeResponseDto> toDtoPage(Page<Employee> page) {
        if (page == null) {
            return null;
        }

        List<EmployeeResponseDto> content = page.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return PaginatedResponseDto.<EmployeeResponseDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
