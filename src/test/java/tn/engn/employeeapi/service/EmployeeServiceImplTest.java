package tn.engn.employeeapi.service;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.exception.EmployeeNotFoundException;
import tn.engn.employeeapi.mapper.EmployeeMapper;
import tn.engn.employeeapi.model.Employee;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.ValidationException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Tests the successful creation of an employee.
     *
     * Given a valid EmployeeRequestDto,
     * When createEmployee is called,
     * Then it should return an EmployeeResponseDto of the created employee.
     */
    @Test
    void testCreateEmployeeSuccess() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto = EmployeeResponseDto.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toDto(employee)).thenReturn(responseDto);

        // When
        EmployeeResponseDto result = employeeService.createEmployee(requestDto);

        // Then
        verify(employeeMapper, times(1)).toEntity(requestDto);
        verify(employeeRepository, times(1)).save(employee);
        verify(employeeMapper, times(1)).toDto(employee);
        assertNotNull(result, "Result should not be null");
        assertEquals(responseDto, result, "EmployeeResponseDto should match");
    }


    /**
     * Tests the creation of an employee with a missing first name.
     *
     * Given an EmployeeRequestDto with a missing first name,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeMissingFirstName() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("")  // Invalid input
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .firstName("")  // Invalid input
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertEquals("First name is required and cannot be blank.", thrown.getMessage());
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }
    /**
     * Tests the creation of an employee with a first name shorter than 3 characters.
     *
     * Given an EmployeeRequestDto with a first name shorter than 3 characters,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeFirstNameTooShort() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("Jo")  // First name too short
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .firstName("Jo")  // First name too short
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertEquals("First name must be at least 3 characters long.", thrown.getMessage());
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }

    /**
     * Tests the creation of an employee with a missing last name.
     *
     * Given an EmployeeRequestDto with a missing last name,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeMissingLastName() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("")  // Invalid input
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("")  // Invalid input
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertEquals("Last name is required and cannot be blank.", thrown.getMessage());
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }

    /**
     * Tests the creation of an employee with a last name shorter than 3 characters.
     *
     * Given an EmployeeRequestDto with a last name shorter than 3 characters,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeLastNameTooShort() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Do")  // Last name too short
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Do")  // Last name too short
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertEquals("Last name must be at least 3 characters long.", thrown.getMessage());
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }

    /**
     * Tests the creation of an employee with a missing email.
     *
     * Given an EmployeeRequestDto with a missing email,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeMissingEmail() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("")  // Missing email
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("")  // Missing email
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertEquals("Email is required and cannot be blank.", thrown.getMessage());
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }

    /**
     * Tests the creation of an employee with an invalid email format.
     *
     * Given an EmployeeRequestDto with an invalid email,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeInvalidEmail() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")  // Invalid email format
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")  // Invalid email format
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertEquals("Email should be valid.", thrown.getMessage());
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }

    /**
     * Tests the creation of an employee with an email that already exists.
     *
     * Given an EmployeeRequestDto with an email that already exists in the database,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeExistingEmail() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")  // Existing email
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")  // Existing email
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);
        when(employeeRepository.existsByEmail(requestDto.getEmail())).thenReturn(true);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertEquals("An employee with this email already exists.", thrown.getMessage());
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }

    /**
     * Tests the creation of an employee with a null date of birth.
     *
     * Given an EmployeeRequestDto with a null date of birth,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeNullDateOfBirth() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(null)  // Null date of birth
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(null)  // Null date of birth
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertEquals("Date of birth is required.", thrown.getMessage());
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }

    /**
     * Tests the creation of an employee who is less than 20 years old.
     *
     * Given an EmployeeRequestDto with an age less than 20,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeAgeLessThan20() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.now().minusYears(19))  // Age less than 20
                .position("Developer")
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.now().minusYears(19))  // Age less than 20
                .position("Developer")
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertEquals("Employee must be at least 20 years old.", thrown.getMessage());
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }

    /**
     * Tests the creation of an employee with an invalid position.
     *
     * Given an EmployeeRequestDto with an invalid position,
     * When createEmployee is called,
     * Then it should throw a ValidationException.
     */
    @Test
    void testCreateEmployeeInvalidPosition() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("InvalidPosition")  // Invalid position
                .build();

        Employee employee = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("InvalidPosition")  // Invalid position
                .build();

        when(employeeMapper.toEntity(requestDto)).thenReturn(employee);

        // When & Then
        ValidationException thrown = assertThrows(ValidationException.class, () -> {
            employeeService.createEmployee(requestDto);
        });
        assertThat(thrown.getMessage()).contains("Invalid position. Allowed values are: Manager, Engineer, Developer," +
                " Analyst, Tester, Designer, Architect, Consultant, Advisor");
        verify(employeeMapper).toEntity(requestDto); // Ensure mapping was called
        verify(employeeRepository, never()).save(any()); // Ensure save was not called
    }

    /**
     * Tests the successful update of an employee.
     *
     * Given an existing employee and a valid EmployeeRequestDto with updated data,
     * When updateEmployee is called,
     * Then it should return the updated EmployeeResponseDto and save the updated employee.
     */
    @Test
    void testUpdateEmployeeSuccess() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .position("Manager")
                .build();

        Employee existingEmployee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee updatedEmployee = Employee.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .position("Manager")
                .build();

        EmployeeResponseDto responseDto = EmployeeResponseDto.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .position("Manager")
                .build();

        when(employeeRepository.findById(anyLong())).thenReturn(Optional.of(existingEmployee));
        when(employeeMapper.toEntity(any(EmployeeRequestDto.class))).thenReturn(updatedEmployee);
        when(employeeRepository.save(any(Employee.class))).thenReturn(updatedEmployee);
        when(employeeMapper.toDto(any(Employee.class))).thenReturn(responseDto);

        // When
        EmployeeResponseDto result = employeeService.updateEmployee(1L, requestDto);

        // Then
        verify(employeeRepository, times(1)).save(any(Employee.class));
        verify(employeeMapper, times(1)).toDto(any(Employee.class));
        assertNotNull(result, "Result should not be null");
        assertEquals("Jane", result.getFirstName(), "First name should be Jane");
    }

    /**
     * Tests the update of an employee when the employee is not found.
     *
     * Given a non-existing employee ID and a valid EmployeeRequestDto,
     * When updateEmployee is called,
     * Then it should throw an EmployeeNotFoundException.
     */
    @Test
    void testUpdateEmployeeNotFoundException() {
        // Given
        EmployeeRequestDto requestDto = EmployeeRequestDto.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .position("Manager")
                .build();

        when(employeeRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When / Then
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.updateEmployee(1L, requestDto));
    }

    /**
     * Tests the retrieval of all employees.
     *
     * Given a list of employees in the repository,
     * When getAllEmployees is called,
     * Then it should return a list of EmployeeResponseDto representing all employees.
     */
    @Test
    void testGetAllEmployeesSuccess() {
        // Given
        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Manager")
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2);

        EmployeeResponseDto responseDto1 = EmployeeResponseDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto2 = EmployeeResponseDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Manager")
                .build();

        List<EmployeeResponseDto> responseDtos = Arrays.asList(responseDto1, responseDto2);

        when(employeeRepository.findAll()).thenReturn(employees);
        when(employeeMapper.toDtoList(employees)).thenReturn(responseDtos);

        // When
        List<EmployeeResponseDto> result = employeeService.getAllEmployees();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(responseDto1, result.get(0));
        assertEquals(responseDto2, result.get(1));
        verify(employeeRepository).findAll(); // Ensure repository was called
        verify(employeeMapper).toDtoList(employees); // Ensure mapping was called
    }

    /**
     * Tests the successful retrieval of all employees.
     *
     * Given a pageable request,
     * When getAllEmployees is called,
     * Then it should return a PaginatedResponseDto with a list of EmployeeResponseDto.
     */
    @Test
    void testGetAllEmployeesWithPagination() {
        // Given
        Employee employee1 = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee2 = Employee.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .position("Manager")
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2);
        Page<Employee> employeePage = new PageImpl<>(employees, Pageable.unpaged(), employees.size());

        EmployeeResponseDto responseDto1 = EmployeeResponseDto.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto2 = EmployeeResponseDto.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .position("Manager")
                .build();

        List<EmployeeResponseDto> responseDtos = Arrays.asList(responseDto1, responseDto2);

        PaginatedResponseDto<EmployeeResponseDto> paginatedResponseDto = PaginatedResponseDto.<EmployeeResponseDto>builder()
                .content(responseDtos)
                .page(0)
                .totalPages(1)
                .totalElements(2)
                .build();

        when(employeeRepository.findAll(any(Pageable.class))).thenReturn(employeePage);
        when(employeeMapper.toDtoPage(any())).thenReturn(paginatedResponseDto);

        // When
        PaginatedResponseDto<EmployeeResponseDto> result = employeeService.getAllEmployees(Pageable.unpaged());

        // Then
        verify(employeeRepository, times(1)).findAll(any(Pageable.class));
        assertNotNull(result, "Result should not be null");
        assertEquals(paginatedResponseDto, result, "PaginatedResponseDto should match");
    }

    /**
     * Tests the successful retrieval of an employee by ID.
     *
     * Given an existing employee ID,
     * When getEmployeeById is called,
     * Then it should return the corresponding EmployeeResponseDto.
     */
    @Test
    void testGetEmployeeByIdSuccess() {
        // Given
        Employee employee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto = EmployeeResponseDto.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeRepository.findById(anyLong())).thenReturn(Optional.of(employee));
        when(employeeMapper.toDto(any(Employee.class))).thenReturn(responseDto);

        // When
        EmployeeResponseDto result = employeeService.getEmployeeById(1L);

        // Then
        verify(employeeRepository, times(1)).findById(anyLong());
        verify(employeeMapper, times(1)).toDto(any(Employee.class));
        assertNotNull(result, "Result should not be null");
        assertEquals(1L, result.getId(), "Employee ID should be 1");
    }

    /**
     * Tests the retrieval of an employee by ID when the employee does not exist.
     *
     * Given a non-existing employee ID,
     * When getEmployeeById is called,
     * Then it should throw an EmployeeNotFoundException.
     */
    @Test
    void testGetEmployeeByIdNotFoundException() {
        // Given
        when(employeeRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When / Then
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(1L));
    }

    /**
     * Tests the retrieval of employees by first name without pagination.
     *
     * Given a first name,
     * When getEmployeesByFirstName is called,
     * Then it should return a list of EmployeeResponseDto representing employees with the given first name.
     */
    @Test
    void testGetEmployeesByFirstNameSuccess() {
        // Given
        String firstName = "John";

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee2 = Employee.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 4, 20))
                .position("Manager")
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2);

        EmployeeResponseDto responseDto1 = EmployeeResponseDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto2 = EmployeeResponseDto.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 4, 20))
                .position("Manager")
                .build();

        List<EmployeeResponseDto> expectedResponse = Arrays.asList(responseDto1, responseDto2);

        when(employeeRepository.findByFirstName(firstName)).thenReturn(employees);
        when(employeeMapper.toDtoList(employees)).thenReturn(expectedResponse);

        // When
        List<EmployeeResponseDto> result = employeeService.getEmployeesByFirstName(firstName);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(responseDto1, result.get(0));
        assertEquals(responseDto2, result.get(1));
        verify(employeeRepository).findByFirstName(firstName); // Ensure repository was called
        verify(employeeMapper).toDtoList(employees); // Ensure mapping was called
    }

    /**
     * Tests the retrieval of employees by first name with pagination.
     *
     * Given a first name and a pageable object,
     * When getEmployeesByFirstName is called,
     * Then it should return a PaginatedResponseDto containing the list of EmployeeResponseDto.
     */
    @Test
    void testGetEmployeesByFirstNameWithPagination() {
        // Given
        String firstName = "John";
        Pageable pageable = PageRequest.of(0, 10);

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee2 = Employee.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 4, 20))
                .position("Manager")
                .build();

        Page<Employee> employeePage = new PageImpl<>(Arrays.asList(employee1, employee2), pageable, 2);

        EmployeeResponseDto responseDto1 = EmployeeResponseDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto2 = EmployeeResponseDto.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 4, 20))
                .position("Manager")
                .build();

        PaginatedResponseDto<EmployeeResponseDto> expectedResponse = PaginatedResponseDto.<EmployeeResponseDto>builder()
                .content(Arrays.asList(responseDto1, responseDto2))
                .page(0)
                .totalPages(1)
                .totalElements(2)
                .build();

        when(employeeRepository.findByFirstName(firstName, pageable)).thenReturn(employeePage);
        when(employeeMapper.toDtoPage(employeePage)).thenReturn(expectedResponse);

        // When
        PaginatedResponseDto<EmployeeResponseDto> result = employeeService.getEmployeesByFirstName(firstName, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(responseDto1, result.getContent().get(0));
        assertEquals(responseDto2, result.getContent().get(1));
        verify(employeeRepository).findByFirstName(firstName, pageable); // Ensure repository was called
        verify(employeeMapper).toDtoPage(employeePage); // Ensure mapping was called
    }

    /**
     * Tests the retrieval of employees by last name without pagination.
     *
     * Given a last name,
     * When getEmployeesByLastName is called,
     * Then it should return a list of EmployeeResponseDto representing employees with the given last name.
     */
    @Test
    void testGetEmployeesByLastNameSuccess() {
        // Given
        String lastName = "Doe";

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Manager")
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2);

        EmployeeResponseDto responseDto1 = EmployeeResponseDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto2 = EmployeeResponseDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Manager")
                .build();

        List<EmployeeResponseDto> expectedResponse = Arrays.asList(responseDto1, responseDto2);

        when(employeeRepository.findByLastName(lastName)).thenReturn(employees);
        when(employeeMapper.toDtoList(employees)).thenReturn(expectedResponse);

        // When
        List<EmployeeResponseDto> result = employeeService.getEmployeesByLastName(lastName);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(responseDto1, result.get(0));
        assertEquals(responseDto2, result.get(1));
        verify(employeeRepository).findByLastName(lastName); // Ensure repository was called
        verify(employeeMapper).toDtoList(employees); // Ensure mapping was called
    }

    /**
     * Tests the retrieval of employees by last name with pagination.
     *
     * Given a last name and pagination details,
     * When getEmployeesByLastName is called,
     * Then it should return a PaginatedResponseDto containing a list of EmployeeResponseDto
     * representing employees with the given last name.
     */
    @Test
    void testGetEmployeesByLastNameWithPagination() {
        // Given
        String lastName = "Doe";
        Pageable pageable = PageRequest.of(0, 10);

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Analyst")
                .build();

        Page<Employee> employeePage = new PageImpl<>(Arrays.asList(employee1, employee2), pageable, 2);

        EmployeeResponseDto responseDto1 = EmployeeResponseDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto2 = EmployeeResponseDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Analyst")
                .build();

        List<EmployeeResponseDto> responseDtos = Arrays.asList(responseDto1, responseDto2);
        PaginatedResponseDto<EmployeeResponseDto> expectedResponse = PaginatedResponseDto.<EmployeeResponseDto>builder()
                .content(responseDtos)
                .page(0)
                .totalPages(1)
                .totalElements(2)
                .build();

        when(employeeRepository.findByLastName(lastName, pageable)).thenReturn(employeePage);
        when(employeeMapper.toDtoPage(employeePage)).thenReturn(expectedResponse);

        // When
        PaginatedResponseDto<EmployeeResponseDto> result = employeeService.getEmployeesByLastName(lastName, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedResponse, result);
        verify(employeeRepository).findByLastName(lastName, pageable); // Ensure repository was called
        verify(employeeMapper).toDtoPage(employeePage); // Ensure mapping was called
    }

    /**
     * Tests the retrieval of employees by email without pagination.
     *
     * Given an email address,
     * When getEmployeesByEmail is called,
     * Then it should return a list of EmployeeResponseDto representing employees with the given email.
     */
    @Test
    void testGetEmployeesByEmailSuccess() {
        // Given
        String email = "john.doe@example.com";

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Analyst")
                .build();

        List<Employee> employees = Arrays.asList(employee1, employee2);

        EmployeeResponseDto responseDto1 = EmployeeResponseDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto2 = EmployeeResponseDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Analyst")
                .build();

        List<EmployeeResponseDto> expectedResponse = Arrays.asList(responseDto1, responseDto2);

        when(employeeRepository.findByEmail(email)).thenReturn(employees);
        when(employeeMapper.toDtoList(employees)).thenReturn(expectedResponse);

        // When
        List<EmployeeResponseDto> result = employeeService.getEmployeesByEmail(email);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedResponse, result);
        verify(employeeRepository).findByEmail(email); // Ensure repository was called
        verify(employeeMapper).toDtoList(employees); // Ensure mapping was called
    }

    /**
     * Tests the retrieval of employees by email with pagination.
     *
     * Given an email address and pagination details,
     * When getEmployeesByEmail is called,
     * Then it should return a PaginatedResponseDto containing a list of EmployeeResponseDto representing employees with the given email.
     */
    @Test
    void testGetEmployeesByEmailWithPagination() {
        // Given
        String email = "john.doe@example.com";
        Pageable pageable = PageRequest.of(0, 10);

        Employee employee1 = Employee.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        Employee employee2 = Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Analyst")
                .build();

        Page<Employee> employeePage = new PageImpl<>(Arrays.asList(employee1, employee2), pageable, 2);

        EmployeeResponseDto responseDto1 = EmployeeResponseDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto responseDto2 = EmployeeResponseDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1988, 5, 15))
                .position("Analyst")
                .build();

        List<EmployeeResponseDto> responseDtos = Arrays.asList(responseDto1, responseDto2);
        PaginatedResponseDto<EmployeeResponseDto> expectedResponse = PaginatedResponseDto.<EmployeeResponseDto>builder()
                .content(responseDtos)
                .page(0)
                .totalPages(1)
                .totalElements(2)
                .build();

        when(employeeRepository.findByEmail(email, pageable)).thenReturn(employeePage);
        when(employeeMapper.toDtoPage(employeePage)).thenReturn(expectedResponse);

        // When
        PaginatedResponseDto<EmployeeResponseDto> result = employeeService.getEmployeesByEmail(email, pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(expectedResponse, result);
        verify(employeeRepository).findByEmail(email, pageable); // Ensure repository was called
        verify(employeeMapper).toDtoPage(employeePage); // Ensure mapping was called
    }

    /**
     * Tests the successful deletion of an employee.
     *
     * Given an existing employee ID,
     * When deleteEmployee is called,
     * Then it should delete the employee and not throw any exception.
     */
    @Test
    void testDeleteEmployeeSuccess() {
        // Given
        Employee employee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        when(employeeRepository.findById(anyLong())).thenReturn(Optional.of(employee));
        doNothing().when(employeeRepository).delete(any());

        // When
        employeeService.deleteEmployee(1L);

        // Then
        verify(employeeRepository, times(1)).delete(any());
    }

    /**
     * Tests the deletion of an employee when the employee does not exist.
     *
     * Given a non-existing employee ID,
     * When deleteEmployee is called,
     * Then it should throw an EmployeeNotFoundException.
     */
    @Test
    void testDeleteEmployeeNotFoundException() {
        // Given
        doThrow(new EmployeeNotFoundException("Employee not found")).when(employeeRepository).deleteById(anyLong());

        // When / Then
        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(1L));
    }
}
