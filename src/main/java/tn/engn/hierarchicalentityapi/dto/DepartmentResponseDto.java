package tn.engn.hierarchicalentityapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for representing a Department in response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponseDto {
    private Long id;
    private String name;
    private Long parentDepartmentId;
    private List<DepartmentResponseDto> subDepartments;
}
