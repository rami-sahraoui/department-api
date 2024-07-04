package tn.engn.departmentapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a Department.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequestDto {
    private Long id;
    private String name;
    private Long parentDepartmentId;
}
