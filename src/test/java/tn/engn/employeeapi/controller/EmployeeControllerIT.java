package tn.engn.employeeapi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.web.reactive.server.WebTestClient;
import tn.engn.HierarchicalEntityApiApplication;
import tn.engn.employeeapi.dto.EmployeeRequestDto;
import tn.engn.employeeapi.dto.EmployeeResponseDto;
import tn.engn.employeeapi.repository.EmployeeRepository;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HierarchicalEntityApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class})
@ActiveProfiles("test-real-db")
class EmployeeControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Clean up the database after each test to ensure isolation.
     */
    @AfterEach
    public void cleanUp() {
        employeeRepository.deleteAll();
    }

    /**
     * Test creating a new employee successfully.
     * This test verifies that a POST request to the /api/v1/employees endpoint
     * with valid employee data creates a new employee and returns the expected response.
     */
    @Test
    void createEmployee_Success() {
        // Arrange
        EmployeeRequestDto employeeRequestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/employees")
                .bodyValue(employeeRequestDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(EmployeeResponseDto.class)
                .value(response -> {
                    assertThat(response.getFirstName()).isEqualTo("John");
                    assertThat(response.getLastName()).isEqualTo("Doe");
                    assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
                });
    }

    /**
     * Test creating a new employee with missing required fields.
     * This test verifies that a POST request to the /api/v1/employees endpoint
     * with missing required fields returns a Bad Request status and appropriate error messages.
     */
    @Test
    void createEmployee_Failure_MissingFields() {
        // Arrange
        EmployeeRequestDto employeeRequestDto = EmployeeRequestDto.builder()
                .firstName("John")
                // Missing last name and email
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Act & Assert
        webTestClient.post()
                .uri("/api/v1/employees")
                .bodyValue(employeeRequestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists()
                .jsonPath("$.message").value(message -> {
                    assertThat(message).asString().matches(".*(Last name is required|Email is required).*");
                });
    }


    /**
     * Test updating an existing employee successfully.
     * This test verifies that a PUT request to the /api/v1/employees/{id} endpoint
     * with valid employee data updates the employee and returns the expected response.
     */
    @Test
    void updateEmployee_Success() {
        // Arrange
        EmployeeRequestDto createRequestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto createdEmployee = createEmployee(createRequestDto);

        EmployeeRequestDto updateRequestDto = EmployeeRequestDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Act & Assert
        webTestClient.put()
                .uri("/api/v1/employees/{id}", createdEmployee.getId())
                .bodyValue(updateRequestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(EmployeeResponseDto.class)
                .value(response -> {
                    assertThat(response.getFirstName()).isEqualTo("Jane");
                    assertThat(response.getLastName()).isEqualTo("Doe");
                    assertThat(response.getEmail()).isEqualTo("jane.doe@example.com");
                });
    }

    /**
     * Test updating a non-existent employee.
     * This test verifies that a PUT request to the /api/v1/employees/{id} endpoint
     * with a non-existent employee ID returns a Not Found status.
     */
    @Test
    void updateEmployee_NotFound() {
        // Arrange
        Long nonExistentId = -1L;
        EmployeeRequestDto updateRequestDto = EmployeeRequestDto.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        // Act & Assert
        webTestClient.put()
                .uri("/api/v1/employees/{id}", nonExistentId)
                .bodyValue(updateRequestDto)
                .exchange()
                .expectStatus().isNotFound();
    }

    /**
     * Test deleting an existing employee successfully.
     * This test verifies that a DELETE request to the /api/v1/employees/{id} endpoint
     * with a valid employee ID deletes the employee and returns the expected response.
     */
    @Test
    void deleteEmployee_Success() {
        // Arrange
        EmployeeRequestDto createRequestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto createdEmployee = createEmployee(createRequestDto);

        // Act & Assert
        webTestClient.delete()
                .uri("/api/v1/employees/{id}", createdEmployee.getId())
                .exchange()
                .expectStatus().isNoContent();
    }

    /**
     * Test deleting a non-existent employee.
     * This test verifies that a DELETE request to the /api/v1/employees/{id} endpoint
     * with a non-existent employee ID returns a Not Found status.
     */
    @Test
    void deleteEmployee_NotFound() {
        // Act & Assert
        webTestClient.delete()
                .uri("/api/v1/employees/{id}", -1L)
                .exchange()
                .expectStatus().isNotFound();
    }

    /**
     * Test retrieving all employees without pagination.
     * This test verifies that a GET request to the /api/v1/employees endpoint
     * returns a list of all employees in the database.
     */
    @Test
    void getAllEmployees_Success() {
        // Arrange
        EmployeeRequestDto employeeRequestDto1 = EmployeeRequestDto.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice.johnson@example.com")
                .dateOfBirth(LocalDate.of(1988, 3, 25))
                .position("Designer")
                .build();

        EmployeeRequestDto employeeRequestDto2 = EmployeeRequestDto.builder()
                .firstName("Bob")
                .lastName("Brown")
                .email("bob.brown@example.com")
                .dateOfBirth(LocalDate.of(1977, 7, 12))
                .position("Architect")
                .build();

        // Create employees in the database
        createEmployee(employeeRequestDto1);
        createEmployee(employeeRequestDto2);

        // Act & Assert
        webTestClient.get()
                .uri("/api/v1/employees")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EmployeeResponseDto.class)
                .value(employees -> assertThat(employees).hasSize(2));
    }

    /**
     * Test retrieving all employees with pagination.
     * This test verifies that a GET request to the /api/v1/employees endpoint
     * with pagination parameters returns a paginated list of employees.
     */
    @Test
    void getAllEmployeesWithPagination_Success() {
        // Arrange
        EmployeeRequestDto employeeRequestDto1 = EmployeeRequestDto.builder()
                .firstName("Charlie")
                .lastName("Daniels")
                .email("charlie.daniels@example.com")
                .dateOfBirth(LocalDate.of(1982, 11, 5))
                .position("Developer")
                .build();

        EmployeeRequestDto employeeRequestDto2 = EmployeeRequestDto.builder()
                .firstName("David")
                .lastName("Evans")
                .email("david.evans@example.com")
                .dateOfBirth(LocalDate.of(1992, 9, 22))
                .position("Analyst")
                .build();

        // Create employees in the database
        createEmployee(employeeRequestDto1);
        createEmployee(employeeRequestDto2);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/employees/paginated")
                        .queryParam("page", 0)
                        .queryParam("size", 1)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaginatedResponseDto.class)
                .value(response -> {
                    assertThat(response.getContent()).hasSize(1);
                    assertThat(response.getTotalElements()).isEqualTo(2);
                });
    }

    /**
     * Integration test for retrieving an employee by ID.
     * This test verifies that a GET request to the /api/v1/employees/{id} endpoint
     * returns the employee details for the given ID.
     */
    @Test
    void getEmployeeById_Success() {
        // Arrange: Create an employee.
        EmployeeRequestDto createRequestDto = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeResponseDto createdEmployee = createEmployee(createRequestDto);

        // Act & Assert: Send a GET request for the employee and verify the response.
        webTestClient.get()
                .uri("/api/v1/employees/{id}", createdEmployee.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(EmployeeResponseDto.class)
                .value(response -> {
                    assertThat(response.getFirstName()).isEqualTo("John");
                    assertThat(response.getLastName()).isEqualTo("Doe");
                    assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
                });
    }

    /**
     * Helper method to create an employee using the API.
     * This method sends a POST request to the /api/v1/employees endpoint with the provided EmployeeRequestDto
     * and returns the created EmployeeResponseDto.
     *
     * @param createRequestDto the EmployeeRequestDto containing employee details
     * @return the EmployeeResponseDto of the created employee
     */
    private EmployeeResponseDto createEmployee(EmployeeRequestDto createRequestDto) {
        return webTestClient.post()
                .uri("/api/v1/employees")
                .bodyValue(createRequestDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(EmployeeResponseDto.class)
                .returnResult()
                .getResponseBody();
    }

    /**
     * Integration test for retrieving an employee by ID when the ID does not exist.
     * This test verifies that a GET request to the /api/v1/employees/{id} endpoint
     * returns a 404 Not Found status when the employee does not exist.
     */
    @Test
    void getEmployeeById_NotFound() {
        // Act & Assert: Send a GET request for a non-existent employee ID and verify the response.
        webTestClient.get()
                .uri("/api/v1/employees/{id}", -1L)
                .exchange()
                .expectStatus().isNotFound();
    }

    /**
     * Integration test for retrieving employees by first name without pagination.
     * This test verifies that a GET request to the /api/v1/employees/first-name endpoint
     * returns a list of employees with the specified first name.
     */
    @Test
    void searchEmployeesByFirstName_Success() {
        // Arrange
        EmployeeRequestDto employeeRequestDto1 = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeRequestDto employeeRequestDto2 = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .position("Manager")
                .build();

        // Create employees in the database
        createEmployee(employeeRequestDto1);
        createEmployee(employeeRequestDto2);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/employees/first-name")
                        .queryParam("firstName", "John")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EmployeeResponseDto.class)
                .value(employees -> {
                    assertThat(employees).hasSize(2);
                    assertThat(employees).extracting(EmployeeResponseDto::getFirstName).containsOnly("John");
                });
    }

    /**
     * Integration test for retrieving employees by first name with pagination.
     * This test verifies that a GET request to the /api/v1/employees/first-name/paginated endpoint
     * returns a paginated list of employees with the specified first name.
     */
    @Test
    void searchEmployeesByFirstNameWithPagination_Success() {
        // Arrange
        EmployeeRequestDto employeeRequestDto1 = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .position("Developer")
                .build();

        EmployeeRequestDto employeeRequestDto2 = EmployeeRequestDto.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .position("Manager")
                .build();

        // Create employees in the database
        createEmployee(employeeRequestDto1);
        createEmployee(employeeRequestDto2);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/employees/first-name/paginated")
                        .queryParam("firstName", "John")
                        .queryParam("page", 0)
                        .queryParam("size", 1)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaginatedResponseDto.class)
                .value(response -> {
                    assertThat(response.getContent()).hasSize(1);
                    assertThat(response.getTotalElements()).isEqualTo(2);
                });
    }

    /**
     * Integration test for retrieving employees by first name when no employees match the criteria.
     * This test verifies that a GET request to the /api/v1/employees/first-name endpoint
     * returns an empty list when no employees match the specified first name.
     */
    @Test
    void searchEmployeesByFirstName_NoResults() {
        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/employees/first-name")
                        .queryParam("firstName", "Unknown")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EmployeeResponseDto.class)
                .value(employees -> assertThat(employees).isEmpty());
    }

    /**
     * Integration test for retrieving employees by last name without pagination.
     * This test verifies that a GET request to the /api/v1/employees/last-name endpoint
     * returns a list of employees with the specified last name.
     */
    @Test
    void searchEmployeesByLastName_Success() {
        // Arrange
        EmployeeRequestDto employeeRequestDto1 = EmployeeRequestDto.builder()
                .firstName("Emily")
                .lastName("Green")
                .email("emily.green@example.com")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .position("Consultant")
                .build();

        EmployeeRequestDto employeeRequestDto2 = EmployeeRequestDto.builder()
                .firstName("Frank")
                .lastName("Green")
                .email("frank.green@example.com")
                .dateOfBirth(LocalDate.of(1980, 4, 10))
                .position("Advisor")
                .build();

        // Create employees in the database
        createEmployee(employeeRequestDto1);
        createEmployee(employeeRequestDto2);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/employees/last-name")
                        .queryParam("lastName", "Green")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EmployeeResponseDto.class)
                .value(employees -> {
                    assertThat(employees).hasSize(2);
                    assertThat(employees).extracting(EmployeeResponseDto::getLastName).containsOnly("Green");
                });
    }

    /**
     * Integration test for retrieving employees by last name with pagination.
     * This test verifies that a GET request to the /api/v1/employees/last-name/paginated endpoint
     * returns a paginated list of employees with the specified last name.
     */
    @Test
    void searchEmployeesByLastNameWithPagination_Success() {
        // Arrange
        EmployeeRequestDto employeeRequestDto1 = EmployeeRequestDto.builder()
                .firstName("George")
                .lastName("Harrison")
                .email("george.harrison@example.com")
                .dateOfBirth(LocalDate.of(1975, 10, 13))
                .position("Consultant")
                .build();

        EmployeeRequestDto employeeRequestDto2 = EmployeeRequestDto.builder()
                .firstName("Henry")
                .lastName("Harrison")
                .email("henry.harrison@example.com")
                .dateOfBirth(LocalDate.of(1983, 8, 29))
                .position("Engineer")
                .build();

        // Create employees in the database
        createEmployee(employeeRequestDto1);
        createEmployee(employeeRequestDto2);

        // Act & Assert
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/employees/last-name/paginated")
                        .queryParam("lastName", "Harrison")
                        .queryParam("page", 0)
                        .queryParam("size", 1)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaginatedResponseDto.class)
                .value(response -> {
                    assertThat(response.getContent()).hasSize(1);
                    assertThat(response.getTotalElements()).isEqualTo(2);
                });
    }
}
