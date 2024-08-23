package tn.engn.employeeapi.dto;

import lombok.Builder;
import lombok.Data;
import tn.engn.assignmentapi.dto.AssignableEntityResponseDto;

import java.time.LocalDate;

/**
 * Data Transfer Object for returning employee details.
 */
@Data
@Builder
public class EmployeeResponseDto implements AssignableEntityResponseDto {

    /**
     * The unique identifier for the employee.
     */
    private Long id;

    /**
     * The full name of the employee, which is not stored in the database.
     */
    private String name;

    /**
     * The first name of the employee.
     */
    private String firstName;

    /**
     * The last name of the employee.
     */
    private String lastName;

    /**
     * The email address of the employee.
     */
    private String email;

    /**
     * The date of birth of the employee.
     */
    private LocalDate dateOfBirth;

    /**
     * The position held by the employee.
     */
    private String position;
}
