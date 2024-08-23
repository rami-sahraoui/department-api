package tn.engn.employeeapi.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.employeeapi.exception.EmployeeNotFoundException;
import tn.engn.employeeapi.mapper.EmployeeMapper;
import tn.engn.employeeapi.model.Employee;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.HierarchicalEntityApiApplication;
import tn.engn.hierarchicalentityapi.TestContainerSetup;
import tn.engn.hierarchicalentityapi.exception.ValidationException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the EmployeeServiceImpl using DTOs.
 */
@Slf4j
@SpringBootTest(classes = HierarchicalEntityApiApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Reset context after each test
//@ActiveProfiles("test-real-db")
//public class EmployeeServiceImplIT {
@ActiveProfiles("test-container")
public class EmployeeServiceImplIT  extends TestContainerSetup {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private EmployeeService employeeService;

    private EmployeeRequestDto employeeRequestDto;

    @Autowired
    private Environment environment;

    @BeforeEach
    void currentSetUp() {
        employeeRepository.deleteAll();

        // Initialize common EmployeeRequestDto for tests
        employeeRequestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();
    }

    /**
     * Clean up the database after each test to ensure isolation.
     */
    @AfterEach
    public void cleanUp() {
        employeeRepository.deleteAll();
    }

    /**
     * Test method to verify if the 'test' profile is active.
     * <p>
     * This test checks if the 'test' profile is correctly activated for the integration test.
     * It retrieves the active profiles from the Spring environment, logs them, and asserts
     * that the 'test' profile is present among the active profiles.
     */
    @Test
    public void testProfileActivation() {
        // Retrieve active profiles from the environment
        String[] activeProfiles = environment.getActiveProfiles();

        // Log active profiles to provide visibility during test execution
        log.info("Active Profiles: {}", Arrays.toString(activeProfiles));

        // Assertion to check if 'test' profile is active
        assertThat(activeProfiles).contains("test-container");
    }

    // Test Cases

    /**
     * Tests the creation of a new employee.
     * Given a valid EmployeeRequestDto,
     * When createEmployee is called,
     * Then it should save the employee and return the corresponding EmployeeResponseDto.
     */
    @Test
    void testCreateEmployee_Success() {
        // When
        EmployeeResponseDto result = employeeService.createEmployee(employeeRequestDto);

        // Then
        assertNotNull(result);
        assertEquals(employeeRequestDto.getFirstName(), result.getFirstName());
        assertEquals(employeeRequestDto.getLastName(), result.getLastName());
        assertEquals(employeeRequestDto.getEmail(), result.getEmail());
        assertEquals(employeeRequestDto.getDateOfBirth(), result.getDateOfBirth());
        assertEquals(employeeRequestDto.getPosition(), result.getPosition());

        assertTrue(employeeRepository.existsByEmail(employeeRequestDto.getEmail()));
    }

    /**
     * Tests the validation error when creating an employee with an invalid email.
     * Given an EmployeeRequestDto with an invalid email format,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployee_InvalidEmail_ThrowsValidationException() {
        // Given
        employeeRequestDto.setEmail("invalid-email");

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(employeeRequestDto);
        });
        assertEquals("Email should be valid.", exception.getMessage());
    }

    /**
     * Tests employee creation with a blank first name.
     * This test ensures that creating an employee with a blank first name throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeBlankFirstName() {
        // Setup: Use a builder to create an EmployeeRequestDto with a blank first name
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(requestDto));
    }

    /**
     * Tests employee creation with an email that already exists in the repository.
     * This test ensures that creating an employee with an existing email throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeExistingEmail() {
        // Setup: Create and save an existing employee with an email address
        EmployeeRequestDto existingDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();
        employeeService.createEmployee(existingDto);

        // Create a new employee DTO with the same email address
        EmployeeRequestDto newDto = EmployeeRequestDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1995, 2, 15))
                .position("Tester")
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(newDto));
    }

    /**
     * Tests employee creation with a blank last name.
     * This test ensures that creating an employee with a blank last name throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeBlankLastName() {
        // Setup: Use a builder to create an EmployeeRequestDto with a blank last name
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(requestDto));
    }

    /**
     * Tests employee creation with a first name that is too short.
     * This test ensures that creating an employee with a first name shorter than 3 characters throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeShortFirstName() {
        // Setup: Use a builder to create an EmployeeRequestDto with a short first name
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("Jo")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(requestDto));
    }

    /**
     * Tests employee creation with a last name that is too short.
     * This test ensures that creating an employee with a last name shorter than 3 characters throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeShortLastName() {
        // Setup: Use a builder to create an EmployeeRequestDto with a short last name
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Do")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(requestDto));
    }

    /**
     * Tests employee creation with an invalid email format.
     * This test ensures that creating an employee with an invalid email format throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeInvalidEmailFormat() {
        // Setup: Use a builder to create an EmployeeRequestDto with an invalid email format
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(requestDto));
    }

    /**
     * Tests employee creation with a blank email.
     * This test ensures that creating an employee with a blank email throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeBlankEmail() {
        // Setup: Use a builder to create an EmployeeRequestDto with a blank email
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("") // Blank email
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(requestDto));
    }

    /**
     * Tests employee creation with a date of birth that makes the employee underage.
     * This test ensures that creating an employee with an age less than 20 throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeUnderage() {
        // Setup: Use a builder to create an EmployeeRequestDto with an underage date of birth
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.now().minusYears(18)) // Less than 20 years old
                .position("Developer")
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(requestDto));
    }

    /**
     * Tests employee creation with a null date of birth.
     * This test ensures that creating an employee with a null date of birth throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeNullDateOfBirth() {
        // Setup: Use a builder to create an EmployeeRequestDto with a null date of birth
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(null) // Null date of birth
                .position("Developer")
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(requestDto));
    }

    /**
     * Tests employee creation with an invalid position.
     * This test ensures that creating an employee with an invalid position throws a ValidationException.
     */
    @Test
    public void testCreateEmployeeInvalidPosition() {
        // Setup: Use a builder to create an EmployeeRequestDto with an invalid position
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("InvalidPosition") // Invalid position
                .build();

        // Execute and Verify: Check that creating the employee throws a ValidationException
        assertThrows(ValidationException.class, () -> employeeService.createEmployee(requestDto));
    }

    /**
     * Tests the update of an existing employee.
     * Given a valid EmployeeRequestDto and an existing employee ID,
     * When updateEmployee is called,
     * Then it should update the employee and return the corresponding EmployeeResponseDto.
     */
    @Test
    void testUpdateEmployee_Success() {
        // Given
        Employee savedEmployee = employeeRepository.save(employeeMapper.toEntity(employeeRequestDto));
        EmployeeRequestDto updateDto = EmployeeRequestDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1992, 4, 15))
                .position("Analyst")
                .build();

        // When
        EmployeeResponseDto result = employeeService.updateEmployee(savedEmployee.getId(), updateDto);

        // Then
        assertNotNull(result);
        assertEquals(updateDto.getFirstName(), result.getFirstName());
        assertEquals(updateDto.getLastName(), result.getLastName());
        assertEquals(updateDto.getEmail(), result.getEmail());
        assertEquals(updateDto.getDateOfBirth(), result.getDateOfBirth());
        assertEquals(updateDto.getPosition(), result.getPosition());
    }

    /**
     * Tests the deletion of an existing employee.
     * Given an existing employee ID,
     * When deleteEmployee is called,
     * Then it should remove the employee from the repository.
     */
    @Test
    void testDeleteEmployee_Success() {
        // Given
        Employee savedEmployee = employeeRepository.save(employeeMapper.toEntity(employeeRequestDto));

        // When
        employeeService.deleteEmployee(savedEmployee.getId());

        // Then
        assertFalse(employeeRepository.existsById(savedEmployee.getId()));
    }

    /**
     * Tests the retrieval of an employee by ID.
     * Given an existing employee ID,
     * When getEmployeeById is called,
     * Then it should return the corresponding EmployeeResponseDto.
     */
    @Test
    void testGetEmployeeById_Success() {
        // Given
        Employee savedEmployee = employeeRepository.save(employeeMapper.toEntity(employeeRequestDto));

        // When
        EmployeeResponseDto result = employeeService.getEmployeeById(savedEmployee.getId());

        // Then
        assertNotNull(result);
        assertEquals(savedEmployee.getFirstName(), result.getFirstName());
        assertEquals(savedEmployee.getLastName(), result.getLastName());
        assertEquals(savedEmployee.getEmail(), result.getEmail());
    }

    /**
     * Tests the retrieval of an employee by a non-existent ID.
     * Given a non-existent employee ID,
     * When getEmployeeById is called,
     * Then it should throw an EmployeeNotFoundException.
     */
    @Test
    void testGetEmployeeById_EmployeeNotFound_ThrowsException() {
        // Given
        Long nonExistentId = -1L;

        // When & Then
        EmployeeNotFoundException exception = assertThrows(EmployeeNotFoundException.class, () -> {
            employeeService.getEmployeeById(nonExistentId);
        });
        assertEquals("Employee not found with id: " + nonExistentId, exception.getMessage());
    }

    /**
     * Tests the retrieval of all employees without pagination.
     * Given employees exist in the repository,
     * When getAllEmployees is called,
     * Then it should return a list of EmployeeResponseDto representing all employees.
     */
    @Test
    void testGetAllEmployees_Success() {
        // Given
        Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build());

        Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1992, 4, 15))
                .position("Analyst")
                .build());

        // When
        List<EmployeeResponseDto> result = employeeService.getAllEmployees();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    /**
     * Tests the retrieval of all employees with pagination.
     * Given employees exist in the repository and a Pageable object,
     * When getAllEmployees(Pageable pageable) is called,
     * Then it should return a PaginatedResponseDto containing a paginated list of EmployeeResponseDto.
     */
    @Test
    void testGetAllEmployeesWithPagination_Success() {
        // Given
        Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build());

        Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1992, 4, 15))
                .position("Analyst")
                .build());

        Pageable pageable = PageRequest.of(0, 10);

        // When
        PaginatedResponseDto<EmployeeResponseDto> result = employeeService.getAllEmployees(pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getTotalPages());
    }

    /**
     * Tests the retrieval of employees by first name without pagination.
     * Given employees with the same first name exist in the repository,
     * When getEmployeesByFirstName is called,
     * Then it should return a list of EmployeeResponseDto representing those employees.
     */
    @Test
    void testGetEmployeesByFirstName_Success() {
        // Given
        Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build());

        Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .dateOfBirth(LocalDate.of(1988, 3, 21))
                .position("Manager")
                .build());

        // When
        List<EmployeeResponseDto> result = employeeService.getEmployeesByFirstName("John");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    /**
     * Tests retrieval of employees by their first name with pagination.
     * This test verifies that employees are correctly paginated when searching by first name.
     */
    @Test
    public void testGetEmployeesByFirstNameWithPagination() {
        // Setup: Create and save test employees
        Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build());

        Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .dateOfBirth(LocalDate.of(1988, 3, 21))
                .position("Manager")
                .build());

        // Define pagination: Page 0 with size 1
        Pageable pageable = PageRequest.of(0, 1);

        // Execute: Retrieve paginated employees by first name "John"
        PaginatedResponseDto<EmployeeResponseDto> result = employeeService.getEmployeesByFirstName("John", pageable);

        // Verify: Check that only one employee is returned and has the expected first name
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
    }

    /**
     * Tests retrieving employees by last name without pagination.
     * This test ensures that employees with a specific last name are retrieved correctly without pagination.
     */
    @Test
    public void testGetEmployeesByLastName_Success() {
        // Setup: Save some test data
        Employee employee1 = Employee.builder().firstName("John").lastName("Doe").email("john.doe1@example.com").dateOfBirth(LocalDate.of(1990, 1, 1)).position("Developer").build();
        Employee employee2 = Employee.builder().firstName("Jane").lastName("Doe").email("jane.doe@example.com").dateOfBirth(LocalDate.of(1995, 2, 15)).position("Tester").build();
        employeeRepository.saveAll(Arrays.asList(employee1, employee2));

        // Execute: Retrieve employees by last name
        List<EmployeeResponseDto> employees = employeeService.getEmployeesByLastName("Doe");

        // Verify: Check that the retrieved employees match the expected results
        assertEquals(2, employees.size());
        assertTrue(employees.stream().anyMatch(e -> e.getEmail().equals("john.doe1@example.com")));
        assertTrue(employees.stream().anyMatch(e -> e.getEmail().equals("jane.doe@example.com")));
    }

    /**
     * Tests retrieving employees by last name with pagination.
     * This test ensures that employees with a specific last name are retrieved correctly with pagination.
     */
    @Test
    public void testGetEmployeesByLastNameWithPagination() {
        // Setup: Save some test data
        Employee employee1 = Employee.builder().firstName("John").lastName("Doe").email("john.doe1@example.com").dateOfBirth(LocalDate.of(1990, 1, 1)).position("Developer").build();
        Employee employee2 = Employee.builder().firstName("Jane").lastName("Doe").email("jane.doe@example.com").dateOfBirth(LocalDate.of(1995, 2, 15)).position("Tester").build();
        employeeRepository.saveAll(Arrays.asList(employee1, employee2));

        // Setup: Define pagination
        Pageable pageable = PageRequest.of(0, 1); // Page 0, size 1

        // Execute: Retrieve paginated employees by last name
        PaginatedResponseDto<EmployeeResponseDto> paginatedEmployees = employeeService.getEmployeesByLastName("Doe", pageable);

        // Verify: Check that the paginated results match the expected results
        assertEquals(1, paginatedEmployees.getContent().size());
        assertTrue(paginatedEmployees.getContent().stream().anyMatch(e -> e.getEmail().equals("john.doe1@example.com")));
        assertEquals(2, paginatedEmployees.getTotalElements()); // Ensure the total count is correct
    }

    /**
     * Tests retrieval of employees by their email without pagination.
     * This test ensures that employees can be correctly retrieved when searching by email.
     */
    @Test
    public void testGetEmployeesByEmail() {
        // Setup: Create and save test employees
        Employee employee = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build());

        // Execute: Retrieve employees by email "john.doe@example.com"
        List<EmployeeResponseDto> result = employeeService.getEmployeesByEmail("john.doe@example.com");

        // Verify: Check that the result contains the expected employee
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("john.doe@example.com");
    }

    /**
     * Tests retrieval of employees by their email with pagination.
     * This test verifies that employees are correctly paginated when searching by email.
     */
    @Test
    public void testGetEmployeesByEmailWithPagination() {
        // Setup: Create and save test employees
        Employee employee1 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build());

        Employee employee2 = employeeRepository.save(Employee.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .dateOfBirth(LocalDate.of(1988, 3, 21))
                .position("Manager")
                .build());

        // Define pagination: Page 0 with size 1
        Pageable pageable = PageRequest.of(0, 1);

        // Execute: Retrieve paginated employees by email "john.doe@example.com"
        PaginatedResponseDto<EmployeeResponseDto> result = employeeService.getEmployeesByEmail("john.doe@example.com", pageable);

        // Verify: Check that only one employee is returned and has the expected email
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john.doe@example.com");
    }
}
