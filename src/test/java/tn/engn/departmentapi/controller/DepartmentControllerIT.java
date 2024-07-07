package tn.engn.departmentapi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.exception.ErrorResponse;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.model.DepartmentClosure;
import tn.engn.departmentapi.repository.DepartmentClosureRepository;
import tn.engn.departmentapi.repository.DepartmentRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DepartmentController.
 * Uses WebTestClient to simulate HTTP requests and verify responses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class})
@ActiveProfiles("test-real-db")
public class DepartmentControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DepartmentClosureRepository departmentClosureRepository;

    /**
     * Clean up the database after each test to ensure isolation.
     */
    @AfterEach
    public void cleanUp() {
        departmentRepository.deleteAll();
        departmentClosureRepository.deleteAll();
    }

    /**
     * Tests the successful creation of a new department.
     * Sends a POST request with valid DepartmentRequestDto and expects HTTP status 201 (Created).
     * Verifies that the response contains the correct department details.
     */
    @Transactional
    @Test
    void createDepartment_Success() {
        // Arrange: Create a valid DepartmentRequestDto
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Engineering");
        requestDto.setParentDepartmentId(null);

        // Act and Assert: Send a POST request and verify the response
        webTestClient.post()
                .uri("/api/v1/departments")
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DepartmentResponseDto.class)
                .value(response -> assertThat(response.getName()).isEqualTo("Engineering"));
    }

    /**
     * Tests the creation of a new department with missing required fields, triggering a validation exception.
     * Sends a POST request with an invalid DepartmentRequestDto and expects HTTP status 400 (Bad Request).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void createDepartment_ValidationException() {
        // Arrange: Create an invalid DepartmentRequestDto (missing name)
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        // No name set to trigger validation error

        // Act and Assert: Send a POST request and verify the response
        webTestClient.post()
                .uri("/api/v1/departments")
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(400);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests the creation of a new department with a non-existing parent department, triggering a not found exception.
     * Sends a POST request with a DepartmentRequestDto containing a non-existing parentDepartmentId and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void createDepartment_ParentDepartmentNotFoundException() {
        // Arrange: Create a DepartmentRequestDto with a non-existing parent department ID
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Engineering");
        requestDto.setParentDepartmentId(-1L); // Non-existing parent ID to trigger not found error

        // Act and Assert: Send a POST request and verify the response
        webTestClient.post()
                .uri("/api/v1/departments")
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(404);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests the successful update of an existing department.
     * Sends a PUT request with valid DepartmentRequestDto and expects HTTP status 200 (OK).
     * Verifies that the response contains the updated department details.
     */
    @Transactional
    @Test
    void updateDepartment_Success() {

        // Arrange: Create a departments with valid name and get his ID
        Long engineeringId = createDepartment("Engineering Department", null);
        Long softwareId = createDepartment("Software Department", engineeringId);
        Long backendTeamId = createDepartment("Backend Team", softwareId);
        Long frontendTeamId = createDepartment("Frontend Team", softwareId);

        Long hrId = createDepartment("HR Department", null);
        // Arrange: Create a valid DepartmentRequestDto
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Updated Engineering");
        requestDto.setParentDepartmentId(hrId);

        // Act: Send a PUT request to update department with ID 1
        webTestClient.put()
                .uri("/api/v1/departments/{id}", engineeringId)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DepartmentResponseDto.class)
                .value(response -> assertThat(response.getName()).isEqualTo("Updated Engineering"));
    }

    /**
     * Tests the update of a department with missing required fields, triggering a validation exception.
     * Sends a PUT request with an invalid DepartmentRequestDto and expects HTTP status 400 (Bad Request).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void updateDepartment_ValidationException() {
        // Arrange: Create a department with valid name and get his ID
        Long engineeringId = createDepartment("Engineering Department", null);

        // Arrange: Create an invalid DepartmentRequestDto (missing name)
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        // No name set to trigger validation error

        // Act and Assert: Send a PUT request and verify the response
        webTestClient.put()
                .uri("/api/v1/departments/{id}", engineeringId)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(400);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests the update of a department with a non-existing parent department, triggering a not found exception.
     * Sends a PUT request with a DepartmentRequestDto containing a non-existing parentDepartmentId and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void updateDepartment_ParentDepartmentNotFoundException() {
        // Arrange: Create a DepartmentRequestDto with a non-existing parent department ID
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Updated Engineering");
        requestDto.setParentDepartmentId(-1L); // Non-existing parent ID to trigger not found error

        // Act and Assert: Send a PUT request and verify the response
        webTestClient.put()
                .uri("/api/v1/departments/{id}", 1)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(404);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests the update of a non-existing department, triggering a not found exception.
     * Sends a PUT request with a valid DepartmentRequestDto and a non-existing department ID and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void updateDepartment_DepartmentNotFoundException() {
        // Arrange: Create a valid DepartmentRequestDto
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Updated Engineering");
        requestDto.setParentDepartmentId(null);

        // Act and Assert: Send a PUT request with a non-existing department ID and verify the response
        webTestClient.put()
                .uri("/api/v1/departments/{id}", 999)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(404);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests the update of a department with a circular reference issue, triggering a data integrity exception.
     * Sends a PUT request with a DepartmentRequestDto causing circular reference and expects HTTP status 409 (Conflict).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void updateDepartment_DataIntegrityException() {
        // Arrange: Create two departments with valid names and get their IDs
        Long department1Id = createDepartment("Engineering", null);
        Long department2Id = createDepartment("IT", null);

        // Update department2 to have department1 as its parent
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Updated IT");
        requestDto.setParentDepartmentId(department1Id);

        webTestClient.put()
                .uri("/api/v1/departments/{id}", department2Id)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DepartmentResponseDto.class)
                .value(response -> assertThat(response.getName()).isEqualTo("Updated IT"));

        // Attempt to update department1 to have department2 as its parent (creating a circular reference)
        requestDto.setName("Updated Engineering");
        requestDto.setParentDepartmentId(department2Id);

        // Act and Assert: Send a PUT request and verify the response
        webTestClient.put()
                .uri("/api/v1/departments/{id}", department1Id)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(409);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests the deletion of an existing department.
     * Sends a DELETE request with an existing department ID and expects HTTP status 204 (No Content).
     */
    @Transactional
    @Test
    void deleteDepartment_Success() {
        // Arrange: Create a department to delete
        Long departmentId = createDepartment("Test Department", null);

        // Act and Assert: Send a DELETE request and verify the response
        webTestClient.delete()
                .uri("/api/v1/departments/{id}", departmentId)
                .exchange()
                .expectStatus().isNoContent();
    }

    /**
     * Tests the deletion of a non-existing department, triggering a not found exception.
     * Sends a DELETE request with a non-existing department ID and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void deleteDepartment_DepartmentNotFoundException() {
        // Act and Assert: Send a DELETE request with a non-existing department ID and verify the response
        webTestClient.delete()
                .uri("/api/v1/departments/{id}", 999)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(404);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Integration test for deleting a department with a data integrity issue,
     * which triggers a data integrity exception due to circular dependencies.
     * <p>
     * Scenario:
     * Departments 'Engineering' and 'IT' are created with valid names and IDs.
     * 'Engineering' is set as the parent department of 'IT', and vice versa,
     * creating a circular dependency that prevents deletion due to constraints.
     * <p>
     * Test Steps:
     * 1. Arrange:
     * - Create 'Engineering' and 'IT' departments in the database.
     * - Establish circular dependency where 'Engineering' is parent of 'IT' and vice versa.
     * 2. Act:
     * - Send a DELETE request to delete 'Engineering' department.
     * 3. Assert:
     * - Expect HTTP status 409 (Conflict) due to data integrity violation.
     * - Verify that the response body contains a valid ErrorResponse.
     * - Check that ErrorResponse includes a status code, non-blank message, and timestamp.
     * 4. Cleanup:
     * - Remove circular dependency and delete both 'Engineering' and 'IT' departments from the database.
     */
    @Transactional
    @Test
    void deleteDepartment_DataIntegrityException() {
        // Arrange: Create two departments with valid names and get their IDs
        Long department1Id = createDepartment("Engineering", null);
        Long department2Id = createDepartment("IT", department1Id);

        // Get department1 and department2 from the repository
        Department department1 = departmentRepository.findById(department1Id).orElseThrow();
        Department department2 = departmentRepository.findById(department2Id).orElseThrow();

        // Create circular dependency: department1 is parent of department2 and vice versa
        department1.setParentDepartmentId(department2.getId());

        DepartmentClosure closure = departmentClosureRepository.save(
                DepartmentClosure.builder()
                        .ancestorId(department2Id)
                        .descendantId(department1Id)
                        .level(1)
                        .build()
        );

        // Save changes to update relationships in the database
        departmentRepository.save(department1);
        departmentRepository.save(department2);

        // Act and Assert: Send a DELETE request and verify the response
        webTestClient.delete()
                .uri("/api/v1/departments/{id}", department1Id)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(409);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });

        // Clean up: Remove circular dependency and delete departments from the database
        department1.setParentDepartmentId(null);
        department2.setParentDepartmentId(null);

        departmentRepository.delete(department1);
        departmentRepository.delete(department2);
        departmentClosureRepository.delete(closure);
    }


    /**
     * Tests the retrieval of all departments.
     * Sends a GET request and expects HTTP status 200 (OK).
     * Verifies that the response contains a list of all departments.
     */
    @Transactional
    @Test
    void getAllDepartments() {
        // Arrange: Create departments "Engineering" and "IT" if they don't exist
        Long departmentId1 = createDepartment("Engineering", null);
        Long departmentId2 = createDepartment("IT", null);

        // Act and Assert: Send a GET request and verify the response
        webTestClient.get()
                .uri("/api/v1/departments")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DepartmentResponseDto.class)
                .value(departments -> {
                    assertThat(departments).isNotNull();
                    assertThat(departments).extracting("name").contains("Engineering", "IT");
                });
    }

    /**
     * Tests retrieving a department by ID.
     * Sends a GET request and expects HTTP status 200 (OK).
     * Verifies that the response contains the expected DepartmentResponseDto.
     */
    @Test
    void getDepartmentById_ExistingDepartment() {
        // Arrange: Create a department and get its ID
        Long departmentId = createDepartment("Engineering", null);

        // Act and Assert: Send a GET request and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/{id}", departmentId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(DepartmentResponseDto.class)
                .value(response -> {
                    assertThat(response.getId()).isEqualTo(departmentId);
                    assertThat(response.getName()).isEqualTo("Engineering");
                    assertThat(response.getParentDepartmentId()).isNull();
                    assertThat(response.getSubDepartments()).isEmpty();
                });
    }

    /**
     * Tests the scenario where a department is not found, triggering a DepartmentNotFoundException.
     * Sends a GET request and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void getDepartmentById_DepartmentNotFound() {
        // Arrange: Define a non-existent department ID
        Long nonExistentDepartmentId = -1L;

        // Act and Assert: Send a GET request and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/{id}", nonExistentDepartmentId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                    assertThat(response.getMessage()).contains("Department not found");
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Integration test to verify behavior when searching departments by an existing name via the controller.
     * Expectation: The method should return a list containing the departments with the specified name.
     */
    @Test
    @Transactional
    public void testSearchDepartmentsByName() {
        // Given
        String searchName = "Engineering";
        createDepartment("Engineering"); // Create a department with the name "Engineering"

        // When
        List<DepartmentResponseDto> departments = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/departments/search")
                        .queryParam("name", searchName)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DepartmentResponseDto.class)
                .returnResult()
                .getResponseBody();

        // Then
        assertThat(departments).isNotNull();
        assertThat(departments).hasSize(1); // Assuming only one department is created with the name "Engineering"
        assertThat(departments.get(0).getName()).isEqualTo("Engineering");
    }

    /**
     * Integration test to verify behavior when searching departments by a non-existing name via the controller.
     * Expectation: The method should return an empty list.
     */
    @Test
    @Transactional
    public void testSearchDepartmentsByNonExistingName() {
        // Given
        String randomName = "NonExistingDepartment_" + UUID.randomUUID(); // Generate a unique non-existing name

        // When
        List<DepartmentResponseDto> departments = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/departments/search")
                        .queryParam("name", randomName)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DepartmentResponseDto.class)
                .returnResult()
                .getResponseBody();

        // Then
        assertThat(departments).isNotNull();
        assertThat(departments).isEmpty();
    }

    /**
     * Tests retrieving sub-departments by parent ID.
     * Sends a GET request and expects HTTP status 200 (OK).
     * Verifies that the response contains the expected list of sub-departments.
     */
    @Test
    void getSubDepartments_ExistingParent() {
        // Arrange: Create a parent department and get its ID
        Long parentDepartmentId = createDepartment("Engineering", null);

        // Create sub-departments and assign them to the parent department
        Long subDepartment1Id = createDepartment("Software", parentDepartmentId);
        Long subDepartment2Id = createDepartment("Hardware", parentDepartmentId);

        // Act and Assert: Send a GET request and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/{id}/sub-departments", parentDepartmentId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBodyList(DepartmentResponseDto.class)
                .value(response -> {
                    assertThat(response).hasSize(2);

                    // Verify the first sub-department
                    DepartmentResponseDto subDepartment1 = response.get(0);
                    assertThat(subDepartment1.getId()).isEqualTo(subDepartment1Id);
                    assertThat(subDepartment1.getName()).isEqualTo("Software");
                    assertThat(subDepartment1.getParentDepartmentId()).isEqualTo(parentDepartmentId);
                    assertThat(subDepartment1.getSubDepartments()).isEmpty();

                    // Verify the second sub-department
                    DepartmentResponseDto subDepartment2 = response.get(1);
                    assertThat(subDepartment2.getId()).isEqualTo(subDepartment2Id);
                    assertThat(subDepartment2.getName()).isEqualTo("Hardware");
                    assertThat(subDepartment2.getParentDepartmentId()).isEqualTo(parentDepartmentId);
                    assertThat(subDepartment2.getSubDepartments()).isEmpty();
                });
    }

    /**
     * Tests retrieving sub-departments by parent ID when the parent department is not found.
     * Sends a GET request and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Test
    void getSubDepartments_ParentNotFound() {
        // Arrange: Define a non-existent parent department ID
        Long nonExistentParentId = -1L;

        // Act and Assert: Send a GET request and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/{id}/sub-departments", nonExistentParentId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(404);
                    assertThat(response.getMessage()).contains("Parent department not found");
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests retrieving the parent department by department ID when both exist.
     * Sends a GET request and expects HTTP status 200 (OK).
     * Verifies that the response contains the correct parent department details,
     * including the sub-departments.
     */
    @Test
    void getParentDepartment_ExistingDepartment() {
        // Arrange: Create a parent department and a child department
        Long parentDepartmentId = createDepartment("Parent Department", null);
        Long childDepartmentId = createDepartment("Child Department", parentDepartmentId);

        // Act and Assert: Send a GET request and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/{id}/parent-department", childDepartmentId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(DepartmentResponseDto.class)
                .value(response -> {
                    assertThat(response.getId()).isEqualTo(parentDepartmentId);
                    assertThat(response.getName()).isEqualTo("Parent Department");
                    assertThat(response.getParentDepartmentId()).isNull();
                    assertThat(response.getSubDepartments())
                            .extracting(DepartmentResponseDto::getId)
                            .containsExactly(childDepartmentId);
                    assertThat(response.getSubDepartments())
                            .extracting(DepartmentResponseDto::getName)
                            .containsExactly("Child Department");
                });
    }

    /**
     * Tests retrieving the parent department by department ID when the parent department is not found.
     * Sends a GET request and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Test
    void getParentDepartment_ParentNotFound() {
        // Arrange: Create a department without a parent and get its ID
        Long departmentId = createDepartment("Engineering", null);

        // Act and Assert: Send a GET request and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/{id}/parent-department", departmentId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(404);
                    assertThat(response.getMessage()).contains("has no parent");
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests retrieving the parent department by department ID when the department does not exist.
     * Sends a GET request and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Test
    void getParentDepartment_DepartmentNotFoundException() {
        // Arrange: Use a non-existent department ID
        Long nonExistentDepartmentId = -1L;

        // Act and Assert: Send a GET request and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/{id}/parent-department", nonExistentDepartmentId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(404);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests retrieving all descendants of a department by ID.
     * Verifies successful retrieval of descendants.
     */
    @Test
    void getDescendants_Success() {
        // Arrange: Create a parent department and its child department
        Long parentId = createDepartment("Parent Department", null);
        Long childId = createDepartment("Child Department", parentId);

        // Act: Send a GET request to retrieve descendants of the parent department
        List<DepartmentResponseDto> descendants = webTestClient.get()
                .uri("/api/v1/departments/{id}/descendants", parentId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DepartmentResponseDto.class)
                .returnResult().getResponseBody();

        // Assert: Verify the response contains the expected descendants
        assertThat(descendants).isNotNull();
        assertThat(descendants).hasSize(1); // Assuming only one child for simplicity
        assertThat(descendants.get(0).getId()).isEqualTo(childId);
        assertThat(descendants.get(0).getName()).isEqualTo("Child Department");
        assertThat(descendants.get(0).getParentDepartmentId()).isEqualTo(parentId);
        assertThat(descendants.get(0).getSubDepartments()).isEmpty(); // Assuming no sub-departments

        // Clean up: Delete the created departments
        departmentRepository.deleteById(parentId);
        departmentRepository.deleteById(childId);
    }

    /**
     * Tests retrieving all descendants of a department by ID.
     * Verifies handling of DepartmentNotFoundException when department is not found.
     */
    @Test
    void getDescendants_DepartmentNotFound() {
        // Arrange: Prepare a non-existent department ID
        Long nonExistentId = -1L; // Using -1L as a placeholder for non-existent ID

        // Act and Assert: Send a GET request and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/{id}/descendants", nonExistentId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(404);
                    assertThat(response.getMessage()).isNotBlank();
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Tests retrieving all ancestors (parent departments recursively) of a department by ID.
     * Verifies successful retrieval of ancestors.
     */
    @Test
    void getAncestors_Success() {
        // Arrange: Create parent departments recursively
        Long grandparentId = createDepartment("Grandparent Department", null);
        Long parentId = createDepartment("Parent Department", grandparentId);
        Long departmentId = createDepartment("Child Department", parentId);

        // Act: Send a GET request to retrieve ancestors of the child department
        List<DepartmentResponseDto> ancestors = webTestClient.get()
                .uri("/api/v1/departments/{id}/ancestors", departmentId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(DepartmentResponseDto.class)
                .returnResult().getResponseBody();

        // Assert: Verify the response contains the expected ancestors
        assertThat(ancestors).isNotNull();
        assertThat(ancestors).hasSize(2); // Expecting two ancestors: Parent and Grandparent

        ancestors.sort((o1, o2) -> (int) (o2.getId() - o1.getId()));
        // Asserting ancestor 1 (Parent Department)
        assertThat(ancestors.get(0).getId()).isEqualTo(parentId);
        assertThat(ancestors.get(0).getName()).isEqualTo("Parent Department");
        assertThat(ancestors.get(0).getParentDepartmentId()).isEqualTo(grandparentId); // Assuming parentId is child of grandparentId
        //assertThat(ancestors.get(0).getSubDepartments()).contains(new DepartmentResponseDto(departmentId, "Child Department", parentId, new ArrayList<>()));

        // Asserting ancestor 2 (Grandparent Department)
        assertThat(ancestors.get(1).getId()).isEqualTo(grandparentId);
        assertThat(ancestors.get(1).getName()).isEqualTo("Grandparent Department");
        assertThat(ancestors.get(1).getParentDepartmentId()).isNull(); // Assuming grandparentId is root department
        //assertThat(ancestors.get(1).getSubDepartments()).contains(new DepartmentResponseDto(parentId, "Parent Department", grandparentId, Collections.singletonList(new DepartmentResponseDto(departmentId, "Child Department", parentId, new ArrayList<>()))));
    }

    /**
     * Helper method to create a department via HTTP POST request and return its ID.
     * Used for setting up test scenarios where a department needs to be created independently of other tests.
     *
     * @param name               Name of the department to create.
     * @param parentDepartmentId ID of the parent department (can be null if no parent).
     * @return ID of the created department.
     */
    private Long createDepartment(String name, Long parentDepartmentId) {
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName(name);
        requestDto.setParentDepartmentId(parentDepartmentId);

        return webTestClient.post()
                .uri("/api/v1/departments")
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DepartmentResponseDto.class)
                .returnResult()
                .getResponseBody()
                .getId();
    }

    /**
     * Helper method to create a department with the specified name.
     *
     * @param name Name of the department to create.
     */
    private void createDepartment(String name) {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name(name)
                .build();

        webTestClient.post()
                .uri("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isCreated();
    }
}
