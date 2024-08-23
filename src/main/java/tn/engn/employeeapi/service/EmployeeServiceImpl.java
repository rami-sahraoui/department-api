package tn.engn.employeeapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.exception.EmployeeNotFoundException;
import tn.engn.employeeapi.mapper.EmployeeMapper;
import tn.engn.employeeapi.model.Employee;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.ValidationException;

import java.util.Arrays;
import java.util.List;

/**
 * Service implementation for managing Employee operations.
 */
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    /**
     * Creates a new employee.
     * @param employeeRequestDto the EmployeeRequestDto containing employee details
     * @return the EmployeeResponseDto of the created employee
     */
    @Override
    public EmployeeResponseDto createEmployee(EmployeeRequestDto employeeRequestDto) {
        Employee employee = employeeMapper.toEntity(employeeRequestDto);
        validateEmployee(employee);  // Perform validations
        employee = employeeRepository.save(employee);
        return employeeMapper.toDto(employee);
    }

    /**
     * Validates the properties of an {@link Employee} entity.
     * This method ensures that the employee's first name, last name, email,
     * date of birth, and position meet the required criteria and constraints.
     *
     * @param employee the {@link Employee} entity to be validated
     * @throws ValidationException if any of the validation rules are violated
     */
    private void validateEmployee(Employee employee) {
        // Validate first name
        if (!StringUtils.hasText(employee.getFirstName())) {
            throw new ValidationException("First name is required and cannot be blank.");
        }
        if (employee.getFirstName().length() < 3) {
            throw new ValidationException("First name must be at least 3 characters long.");
        }

        // Validate last name
        if (!StringUtils.hasText(employee.getLastName())) {
            throw new ValidationException("Last name is required and cannot be blank.");
        }
        if (employee.getLastName().length() < 3) {
            throw new ValidationException("Last name must be at least 3 characters long.");
        }

        // Validate email
        if (!StringUtils.hasText(employee.getEmail())) {
            throw new ValidationException("Email is required and cannot be blank.");
        }
        if (!employee.getEmail().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$")) {
            throw new ValidationException("Email should be valid.");
        }
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new ValidationException("An employee with this email already exists.");
        }

        // Validate date of birth
        if (employee.getDateOfBirth() == null) {
            throw new ValidationException("Date of birth is required.");
        }
        if (employee.getAge() < 20) {
            throw new ValidationException("Employee must be at least 20 years old.");
        }

        // Validate position
        List<String> validPositions = Arrays.asList("Manager", "Engineer", "Developer", "Analyst", "Tester", "Designer", "Architect","Consultant", "Advisor");
        if (!validPositions.contains(employee.getPosition())) {
            throw new ValidationException("Invalid position. Allowed values are: " + String.join(", ", validPositions));
        }
    }

    /**
     * Updates an existing employee.
     * @param id the employee ID
     * @param employeeRequestDto the EmployeeRequestDto containing updated employee details
     * @return the EmployeeResponseDto of the updated employee
     */
    @Override
    public EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto employeeRequestDto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
        employee.setFirstName(employeeRequestDto.getFirstName());
        employee.setLastName(employeeRequestDto.getLastName());
        employee.setEmail(employeeRequestDto.getEmail());
        employee.setDateOfBirth(employeeRequestDto.getDateOfBirth());
        employee.setPosition(employeeRequestDto.getPosition());
        validateEmployee(employee);  // Perform validations
        employee = employeeRepository.save(employee);
        return employeeMapper.toDto(employee);
    }

    /**
     * Deletes an employee by ID.
     * @param id the employee ID
     */
    @Override
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }

    /**
     * Retrieves an employee by ID.
     * @param id the employee ID
     * @return the EmployeeResponseDto of the retrieved employee
     */
    @Override
    public EmployeeResponseDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
        return employeeMapper.toDto(employee);
    }

    /**
     * Retrieves all employees without pagination.
     * @return the list of EmployeeResponseDto
     */
    @Override
    public List<EmployeeResponseDto> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        return employeeMapper.toDtoList(employees);
    }

    /**
     * Retrieves all employees with pagination.
     * @param pageable the Pageable object containing pagination information
     * @return the PaginatedResponseDto containing the list of EmployeeResponseDto
     */
    @Override
    public PaginatedResponseDto<EmployeeResponseDto> getAllEmployees(Pageable pageable) {
        Page<Employee> employees = employeeRepository.findAll(pageable);
        return employeeMapper.toDtoPage(employees);
    }

    /**
     * Retrieves employees by first name without pagination.
     * @param firstName the first name of the employees
     * @return the list of EmployeeResponseDto
     */
    @Override
    public List<EmployeeResponseDto> getEmployeesByFirstName(String firstName) {
        List<Employee> employees = employeeRepository.findByFirstName(firstName);
        return employeeMapper.toDtoList(employees);
    }

    /**
     * Retrieves employees by first name with pagination.
     * @param firstName the first name of the employees
     * @param pageable the Pageable object containing pagination information
     * @return the PaginatedResponseDto containing the list of EmployeeResponseDto
     */
    @Override
    public PaginatedResponseDto<EmployeeResponseDto> getEmployeesByFirstName(String firstName, Pageable pageable) {
        Page<Employee> employees = employeeRepository.findByFirstName(firstName, pageable);
        return employeeMapper.toDtoPage(employees);
    }

    /**
     * Retrieves employees by last name without pagination.
     * @param lastName the last name of the employees
     * @return the list of EmployeeResponseDto
     */
    @Override
    public List<EmployeeResponseDto> getEmployeesByLastName(String lastName) {
        List<Employee> employees = employeeRepository.findByLastName(lastName);
        return employeeMapper.toDtoList(employees);
    }

    /**
     * Retrieves employees by last name with pagination.
     * @param lastName the last name of the employees
     * @param pageable the Pageable object containing pagination information
     * @return the PaginatedResponseDto containing the list of EmployeeResponseDto
     */
    @Override
    public PaginatedResponseDto<EmployeeResponseDto> getEmployeesByLastName(String lastName, Pageable pageable) {
        Page<Employee> employees = employeeRepository.findByLastName(lastName, pageable);
        return employeeMapper.toDtoPage(employees);
    }

    /**
     * Retrieves employees by email without pagination.
     * @param email the email of the employees
     * @return the list of EmployeeResponseDto
     */
    @Override
    public List<EmployeeResponseDto> getEmployeesByEmail(String email) {
        List<Employee> employees = employeeRepository.findByEmail(email);
        return employeeMapper.toDtoList(employees);
    }

    /**
     * Retrieves employees by email with pagination.
     * @param email the email of the employees
     * @param pageable the Pageable object containing pagination information
     * @return the PaginatedResponseDto containing the list of EmployeeResponseDto
     */
    @Override
    public PaginatedResponseDto<EmployeeResponseDto> getEmployeesByEmail(String email, Pageable pageable) {
        Page<Employee> employees = employeeRepository.findByEmail(email, pageable);
        return employeeMapper.toDtoPage(employees);
    }
}
