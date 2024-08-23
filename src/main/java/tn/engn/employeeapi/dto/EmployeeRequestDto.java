package tn.engn.employeeapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import tn.engn.assignmentapi.dto.AssignableEntityRequestDto;

import java.time.LocalDate;

/**
 * Data Transfer Object for creating or updating an employee.
 */
@Data
@Builder
public class EmployeeRequestDto implements AssignableEntityRequestDto {

    /**
     * The first name of the employee.
     */
    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "First name should only contain letters and spaces")
    private String firstName;

    /**
     * The last name of the employee.
     */
    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[A-Za-z ]+$", message = "Last name should only contain letters and spaces")
    private String lastName;

    /**
     * The email address of the employee.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    /**
     * The date of birth of the employee.
     */
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    /**
     * The position held by the employee.
     */
    @NotBlank(message = "Position is required")
    private String position;
}
