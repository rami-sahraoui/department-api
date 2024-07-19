package tn.engn.hierarchicalentityapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.transaction.annotation.Transactional;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.ErrorResponse;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DepartmentController.
 * Uses WebTestClient to simulate HTTP requests and verify responses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class})
@ActiveProfiles("test-real-db")
@Slf4j
public class DepartmentControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private DepartmentRepository departmentRepository;

    /**
     * Clean up the database after each test to ensure isolation.
     */
    @AfterEach
    public void cleanUp() {
        departmentRepository.deleteAll();
    }

    /**
     * Tests the successful creation of a new department.
     * Sends a POST request with valid HierarchyRequestDto and expects HTTP status 201 (Created).
     * Verifies that the response contains the correct department details.
     */
    @Transactional
    @Test
    void createDepartment_Success() {
        // Arrange: Create a valid HierarchyRequestDto
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Engineering");
        requestDto.setParentEntityId(null);

        // Act and Assert: Send a POST request and verify the response
        webTestClient.post()
                .uri("/api/v1/departments")
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(HierarchyResponseDto.class)
                .value(response -> assertThat(response.getName()).isEqualTo("Engineering"));
    }

    /**
     * Tests the creation of a new department with missing required fields, triggering a validation exception.
     * Sends a POST request with an invalid HierarchyRequestDto and expects HTTP status 400 (Bad Request).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void createDepartment_ValidationException() {
        // Arrange: Create an invalid HierarchyRequestDto (missing name)
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
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
     * Sends a POST request with a HierarchyRequestDto containing a non-existing parentDepartmentId and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void createDepartment_ParentDepartmentNotFoundException() {
        // Arrange: Create a HierarchyRequestDto with a non-existing parent department ID
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Engineering");
        requestDto.setParentEntityId(-1L); // Non-existing parent ID to trigger not found error

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
     * Sends a PUT request with valid HierarchyRequestDto and expects HTTP status 200 (OK).
     * Verifies that the response contains the updated department details.
     */
    @Transactional
    @Test
    void updateDepartment_Success() {

        // Arrange: Create a departments with valid name and get his ID
        Long departmentId = createDepartment("Engineering", null);

        // Arrange: Create a valid HierarchyRequestDto
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Updated Engineering");
        requestDto.setParentEntityId(null);

        // Act: Send a PUT request to update department with ID 1
        webTestClient.put()
                .uri("/api/v1/departments/{id}", departmentId)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(HierarchyResponseDto.class)
                .value(response -> assertThat(response.getName()).isEqualTo("Updated Engineering"));
    }

    /**
     * Tests the update of a department with missing required fields, triggering a validation exception.
     * Sends a PUT request with an invalid HierarchyRequestDto and expects HTTP status 400 (Bad Request).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void updateDepartment_ValidationException() {
        // Arrange: Create an invalid HierarchyRequestDto (missing name)
        Long departmentId = createDepartment("Engineering", null);
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        // No name set to trigger validation error

        // Act and Assert: Send a PUT request and verify the response
        webTestClient.put()
                .uri("/api/v1/departments/{id}", departmentId)
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
     * Sends a PUT request with a HierarchyRequestDto containing a non-existing parentDepartmentId and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void updateDepartment_ParentDepartmentNotFoundException() {
        // Arrange: Create a HierarchyRequestDto with a non-existing parent department ID
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Updated Engineering");
        requestDto.setParentEntityId(-1L); // Non-existing parent ID to trigger not found error

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
     * Sends a PUT request with a valid HierarchyRequestDto and a non-existing department ID and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void updateDepartment_DepartmentNotFoundException() {
        // Arrange: Create a valid HierarchyRequestDto
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Updated Engineering");
        requestDto.setParentEntityId(null);

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
     * Sends a PUT request with a HierarchyRequestDto causing circular reference and expects HTTP status 409 (Conflict).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Transactional
    @Test
    void updateDepartment_DataIntegrityException() {
        // Arrange: Create two departments with valid names and get their IDs
        Long department1Id = createDepartment("Engineering", null);
        Long department2Id = createDepartment("IT", null);

        // Update department2 to have department1 as its parent
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Updated IT");
        requestDto.setParentEntityId(department1Id);

        webTestClient.put()
                .uri("/api/v1/departments/{id}", department2Id)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(HierarchyResponseDto.class)
                .value(response -> assertThat(response.getName()).isEqualTo("Updated IT"));

        // Attempt to update department1 to have department2 as its parent (creating a circular reference)
        requestDto.setName("Updated Engineering");
        requestDto.setParentEntityId(department2Id);

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
        Long department2Id = createDepartment("IT", null);

        // Get department1 and department2 from the repository
        Department department1 = departmentRepository.findById(department1Id).orElseThrow();
        Department department2 = departmentRepository.findById(department2Id).orElseThrow();

        // Create circular dependency: department1 is parent of department2 and vice versa
        department1.setParentId(department2.getId());
        department1.setPath("/" + department1.getId() + "/");

        department2.setParentId(department1.getId());
        department2.setPath("/" + department1.getId()  + "/" + department2.getId() + "/" + department1.getId()  + "/");


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
        department1.setParentId(null);
        department2.setParentId(null);

        departmentRepository.delete(department1);
        departmentRepository.delete(department2);
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
                .expectBodyList(HierarchyResponseDto.class)
                .value(departments -> {
                    assertThat(departments).isNotNull();
                    assertThat(departments).extracting("name").contains("Engineering", "IT");
                });
    }

    /**
     * Tests the retrieval of all departments with an option to fetch sub-entities.
     * Sends a GET request with fetchSubEntities=true and expects HTTP status 200 (OK).
     * Verifies that the response contains a list of all departments and their sub-entities.
     */
    @Transactional
    @Test
    void getAllDepartmentsWithSubEntities() {
        // Arrange: Create departments "Engineering" and "IT" with sub-departments
        Long departmentId1 = createDepartment("Engineering", null);
        Long departmentId2 = createDepartment("IT", null);

        Long subDepartmentId1 = createDepartment("Software", departmentId1);
        Long subDepartmentId2 = createDepartment("Hardware", departmentId1);
        Long subDepartmentId3 = createDepartment("Support", departmentId2);

        // Act and Assert: Send a GET request with fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .value(departments -> {
                    assertThat(departments).isNotNull();
                    assertThat(departments).extracting("name").contains("Engineering", "IT");
                    // Verify sub-entities are fetched when fetchSubEntities=true
                    for (HierarchyResponseDto department : departments) {
                        if ("Engineering".equals(department.getName())) {
                            assertThat(department.getSubEntities()).hasSize(2);
                            assertThat(department.getSubEntities())
                                    .extracting("name")
                                    .contains("Software", "Hardware");
                        } else if ("IT".equals(department.getName())) {
                            assertThat(department.getSubEntities()).hasSize(1);
                            assertThat(department.getSubEntities())
                                    .extracting("name")
                                    .contains("Support");
                        }
                    }
                });
    }

    /**
     * Tests the retrieval of paginated departments with an option to fetch sub-entities.
     * Sends a GET request with fetchSubEntities=true and expects HTTP status 200 (OK).
     * Verifies that the response contains a paginated list of departments and their sub-entities.
     */
    @Transactional
    @Test
    void getAllDepartmentsPaginatedWithSubEntities() {
        // Arrange: Create departments "Engineering" and "IT" with sub-departments
        Long departmentId1 = createDepartment("Engineering", null);
        Long departmentId2 = createDepartment("IT", null);

        Long subDepartmentId1 = createDepartment("Software", departmentId1);
        Long subDepartmentId2 = createDepartment("Hardware", departmentId1);
        Long subDepartmentId3 = createDepartment("Support", departmentId2);

        // Act and Assert: Send a GET request with fetchSubEntities=true and verify the paginated response
        ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>> responseType =
                new ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>>() {
                };

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/with-sub-entities/paginated")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", 0) // Page number (0-based)
                        .queryParam("size", 10) // Page size
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(responseType)
                .value(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getContent()).isNotNull().hasSize(5); // Assuming 5 departments per page

                    List<HierarchyResponseDto> departments = (List<HierarchyResponseDto>) response.getContent().stream().map(r -> (HierarchyResponseDto) r).collect(Collectors.toList());
                    for (HierarchyResponseDto department : departments) {
                        assertThat(department.getName()).isNotNull();
                        assertThat(department.getSubEntities()).isNotNull(); // Adjusted to getSubEntities
                        // Additional assertions as per your requirement
                        if ("Engineering".equals(department.getName())) {
                            assertThat(department.getSubEntities()).hasSize(2);
                            assertThat(department.getSubEntities())
                                    .extracting("name")
                                    .contains("Software", "Hardware");
                        } else if ("IT".equals(department.getName())) {
                            assertThat(department.getSubEntities()).hasSize(1);
                            assertThat(department.getSubEntities())
                                    .extracting("name")
                                    .contains("Support");
                        }
                    }

                    // Verify pagination metadata
                    assertThat(response.getPage()).isEqualTo(0);
                    assertThat(response.getSize()).isEqualTo(10);
                    assertThat(response.getTotalElements()).isEqualTo(5); // Total number of departments
                });
    }

    /**
     * Tests retrieving a department by ID.
     * Sends a GET request and expects HTTP status 200 (OK).
     * Verifies that the response contains the expected HierarchyResponseDto.
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
                .expectBody(HierarchyResponseDto.class)
                .value(response -> {
                    assertThat(response.getId()).isEqualTo(departmentId);
                    assertThat(response.getName()).isEqualTo("Engineering");
                    assertThat(response.getParentEntityId()).isNull();
                    assertThat(response.getSubEntities()).isEmpty();
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
                    assertThat(response.getMessage()).contains("Entity not found");
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Integration test for retrieving an entity by its ID with an option to fetch sub-entities.
     * Sends a GET request with fetchSubEntities=true and expects HTTP status 200 (OK).
     * Verifies that the response contains the entity with its sub-entities if fetchSubEntities=true.
     */
    @Transactional
    @Test
    void getEntityByIdWithSubEntities() {
        // Arrange: Create departments "Engineering" and "IT" with sub-departments
        Long departmentId1 = createDepartment("Engineering", null);
        Long departmentId2 = createDepartment("IT", null);

        Long subDepartmentId1 = createDepartment("Software", departmentId1);
        Long subDepartmentId2 = createDepartment("Hardware", departmentId1);
        Long subDepartmentId3 = createDepartment("Support", departmentId2);

        // Act and Assert: Send a GET request and verify the response for departmentId1
        webTestClient.get()
                .uri("/api/v1/departments/" + departmentId1 + "/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(HierarchyResponseDto.class)
                .value(entity -> {
                    // Assert the returned entity matches the expected entity
                    assertThat(entity).isNotNull();
                    assertThat(entity.getId()).isEqualTo(departmentId1);
                    assertThat(entity.getName()).isEqualTo("Engineering");
                    assertThat(entity.getSubEntities()).isNotNull();
                    assertThat(entity.getSubEntities()).hasSize(2);
                    assertThat(entity.getSubEntities())
                            .extracting("name")
                            .contains("Software", "Hardware");
                });

        // Act and Assert: Send a GET request and verify the response for departmentId2
        webTestClient.get()
                .uri("/api/v1/departments/" + departmentId2 + "/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(HierarchyResponseDto.class)
                .value(entity -> {
                    // Assert the returned entity matches the expected entity
                    assertThat(entity).isNotNull();
                    assertThat(entity.getId()).isEqualTo(departmentId2);
                    assertThat(entity.getName()).isEqualTo("IT");
                    assertThat(entity.getSubEntities()).isNotNull();
                    assertThat(entity.getSubEntities()).hasSize(1);
                    assertThat(entity.getSubEntities())
                            .extracting("name")
                            .contains("Support");
                });

        // Act and Assert: Send a GET request and verify the response for subDepartmentId1
        webTestClient.get()
                .uri("/api/v1/departments/" + subDepartmentId1 + "/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(HierarchyResponseDto.class)
                .value(entity -> {
                    // Assert the returned entity matches the expected entity
                    assertThat(entity).isNotNull();
                    assertThat(entity.getId()).isEqualTo(subDepartmentId1);
                    assertThat(entity.getName()).isEqualTo("Software");
                    assertThat(entity.getSubEntities()).isNotNull();
                    assertThat(entity.getSubEntities()).isEmpty();
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
        List<HierarchyResponseDto> departments = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/departments/search")
                        .queryParam("name", searchName)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
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
        List<HierarchyResponseDto> departments = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/departments/search")
                        .queryParam("name", randomName)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .returnResult()
                .getResponseBody();

        // Then
        assertThat(departments).isNotNull();
        assertThat(departments).isEmpty();
    }

    /**
     * Integration test for searching entities by name with an option to fetch sub-entities.
     * Sends a GET request with fetchSubEntities=true and expects HTTP status 200 (OK).
     * Verifies that the response contains the entities matching the search name with their sub-entities if fetchSubEntities=true.
     */
    @Transactional
    @Test
    void searchEntitiesByNameWithSubEntities() {
        // Arrange: Create departments "Engineering" and "IT" with sub-departments
        Long departmentId1 = createDepartment("Engineering", null);
        Long departmentId2 = createDepartment("IT", null);

        Long subDepartmentId1 = createDepartment("Software", departmentId1);
        Long subDepartmentId2 = createDepartment("Hardware", departmentId1);
        Long subDepartmentId3 = createDepartment("Support", departmentId2);

        // Act and Assert: Send a GET request to search for "Engineering" with fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/search/with-sub-entities")
                        .queryParam("name", "Engineering")
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .value(entities -> {
                    // Assert the returned entities match the expected entities
                    assertThat(entities).isNotNull();
                    assertThat(entities).hasSize(1);
                    HierarchyResponseDto entity = entities.get(0);
                    assertThat(entity.getName()).isEqualTo("Engineering");
                    assertThat(entity.getSubEntities()).isNotNull();
                    assertThat(entity.getSubEntities()).hasSize(2);
                    assertThat(entity.getSubEntities())
                            .extracting("name")
                            .contains("Software", "Hardware");
                });

        // Act and Assert: Send a GET request to search for "IT" with fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/search/with-sub-entities")
                        .queryParam("name", "IT")
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .value(entities -> {
                    // Assert the returned entities match the expected entities
                    assertThat(entities).isNotNull();
                    assertThat(entities).hasSize(1);
                    HierarchyResponseDto entity = entities.get(0);
                    assertThat(entity.getName()).isEqualTo("IT");
                    assertThat(entity.getSubEntities()).isNotNull();
                    assertThat(entity.getSubEntities()).hasSize(1);
                    assertThat(entity.getSubEntities())
                            .extracting("name")
                            .contains("Support");
                });

        // Act and Assert: Send a GET request to search for a sub-department "Software" with fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/search/with-sub-entities")
                        .queryParam("name", "Software")
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .value(entities -> {
                    // Assert the returned entities match the expected entities
                    assertThat(entities).isNotNull();
                    assertThat(entities).hasSize(1);
                    HierarchyResponseDto entity = entities.get(0);
                    assertThat(entity.getName()).isEqualTo("Software");
                    assertThat(entity.getSubEntities()).isNotNull();
                    assertThat(entity.getSubEntities()).isEmpty();
                });

        // Act and Assert: Send a GET request to search for a non-existent department with fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/search/with-sub-entities")
                        .queryParam("name", "NonExistent")
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .value(entities -> {
                    // Assert the returned entities are empty
                    assertThat(entities).isNotNull();
                    assertThat(entities).isEmpty();
                });
    }

    /**
     * Tests retrieving sub-departments by parent ID.
     * Sends a GET request and expects HTTP status 200 (OK).
     * Verifies that the response contains the expected list of sub-departments.
     */
    @Test
    void getSubEntities_ExistingParent() {
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
                .expectBodyList(HierarchyResponseDto.class)
                .value(response -> {
                    assertThat(response).hasSize(2);

                    // Verify the first sub-department
                    HierarchyResponseDto subDepartment1 = response.get(0);
                    assertThat(subDepartment1.getId()).isEqualTo(subDepartment1Id);
                    assertThat(subDepartment1.getName()).isEqualTo("Software");
                    assertThat(subDepartment1.getParentEntityId()).isEqualTo(parentDepartmentId);
                    assertThat(subDepartment1.getSubEntities()).isEmpty();

                    // Verify the second sub-department
                    HierarchyResponseDto subDepartment2 = response.get(1);
                    assertThat(subDepartment2.getId()).isEqualTo(subDepartment2Id);
                    assertThat(subDepartment2.getName()).isEqualTo("Hardware");
                    assertThat(subDepartment2.getParentEntityId()).isEqualTo(parentDepartmentId);
                    assertThat(subDepartment2.getSubEntities()).isEmpty();
                });
    }

    /**
     * Tests retrieving sub-departments by parent ID when the parent department is not found.
     * Sends a GET request and expects HTTP status 404 (Not Found).
     * Verifies that the response contains a valid ErrorResponse.
     */
    @Test
    void getSubEntities_ParentNotFound() {
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
                    assertThat(response.getMessage()).contains("Parent entity not found");
                    assertThat(response.getTimestamp()).isNotNull();
                });
    }

    /**
     * Integration test for retrieving sub-entities (children) of a given parent entity with an option to fetch sub-entities.
     * Sends a GET request with fetchSubEntities=true, expects HTTP status 200 (OK).
     * Verifies that the response contains sub-entities with their sub-entities if fetchSubEntities=true.
     * Handles the case when the parent entity is not found and expects HTTP status 404 (Not Found).
     */
    @Transactional
    @Test
    void getSubEntitiesWithSubEntities() {
        // Arrange: Create departments with hierarchical relationships
        Long parentDepartmentId = createDepartment("Parent Department", null);
        Long childDepartmentId1 = createDepartment("Child Department 1", parentDepartmentId);
        Long childDepartmentId2 = createDepartment("Child Department 2", parentDepartmentId);
        Long grandChildDepartmentId = createDepartment("Grandchild Department", childDepartmentId1);

        // Act and Assert: Send a GET request to retrieve the sub-entities of "Parent Department" with fetchSubEntities=true
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + parentDepartmentId + "/sub-entities/with-sub-entities")
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<HierarchyResponseDto>>() {
                })
                .value(subEntities -> {
                    // Assert the returned sub-entities match the expected hierarchy
                    assertThat(subEntities).isNotNull();
                    assertThat(subEntities).hasSize(2); // Two child departments

                    // Extract names of the sub-entities
                    List<String> subEntityNames = subEntities.stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the sub-entities
                    assertThat(subEntityNames).containsExactlyInAnyOrder("Child Department 1", "Child Department 2");

                    // Assert the sub-entities of the sub-entities
                    subEntities.forEach(subEntity -> {
                        if (subEntity.getName().equals("Child Department 1")) {
                            assertThat(subEntity.getSubEntities()).isNotNull();
                            assertThat(subEntity.getSubEntities()).hasSize(1);
                            assertThat(subEntity.getSubEntities())
                                    .extracting("name")
                                    .containsExactly("Grandchild Department");
                        } else if (subEntity.getName().equals("Child Department 2")) {
                            assertThat(subEntity.getSubEntities()).isEmpty();
                        }
                    });
                });

        // Act and Assert: Send a GET request to retrieve the sub-entities of "Parent Department" with fetchSubEntities=false
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + parentDepartmentId + "/sub-entities/with-sub-entities")
                        .queryParam("fetchSubEntities", false)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<HierarchyResponseDto>>() {
                })
                .value(subEntities -> {
                    // Assert the returned sub-entities match the expected hierarchy
                    assertThat(subEntities).isNotNull();
                    assertThat(subEntities).hasSize(2); // Two child departments

                    // Extract names of the sub-entities
                    List<String> subEntityNames = subEntities.stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the sub-entities
                    assertThat(subEntityNames).containsExactlyInAnyOrder("Child Department 1", "Child Department 2");

                    // Assert the sub-entities of the sub-entities (should be null since fetchSubEntities=false)
                    subEntities.forEach(subEntity -> {
                        subEntity.getSubEntities().forEach(subSubEntity -> {
                            assertThat(subSubEntity.getSubEntities()).isNull();
                        });
                    });
                });

        // Act and Assert: Send a GET request to retrieve the sub-entities of a non-existing parent entity and expect HTTP status 404
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + -1L + "/sub-entities/with-sub-entities")
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isNotFound();
    }

    /**
     * Integration test for retrieving paginated sub-entities (children) of a given parent entity with an option to fetch sub-entities.
     * Sends a GET request with fetchSubEntities=true and pageable parameters, expects HTTP status 200 (OK).
     * Verifies that the response contains paginated sub-entities with their sub-entities if fetchSubEntities=true.
     * Handles the case when the parent entity is not found and expects HTTP status 404 (Not Found).
     */
    @Transactional
    @Test
    void getSubEntitiesWithPaginationAndSubEntities() {
        // Arrange: Create departments with hierarchical relationships
        Long parentDepartmentId = createDepartment("Parent Department", null);
        Long childDepartmentId1 = createDepartment("Child Department 1", parentDepartmentId);
        Long childDepartmentId2 = createDepartment("Child Department 2", parentDepartmentId);
        Long grandChildDepartmentId = createDepartment("Grandchild Department", childDepartmentId1);

        // Define pagination parameters
        int page = 0;
        int size = 2;
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        // Act and Assert: Send a GET request to retrieve the paginated sub-entities of "Parent Department" with fetchSubEntities=true
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + parentDepartmentId + "/sub-entities/with-sub-entities/paginated")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParam("sort", pageable.getSort().toString().replace(": ", ","))
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>>() {
                })
                .value(paginatedResponse -> {
                    // Assert the returned paginated response matches the expected data
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).hasSize(2); // Two child departments

                    // Extract names of the sub-entities
                    List<String> subEntityNames = paginatedResponse.getContent().stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the sub-entities
                    assertThat(subEntityNames).containsExactlyInAnyOrder("Child Department 1", "Child Department 2");

                    // Assert the sub-entities of the sub-entities
                    paginatedResponse.getContent().forEach(subEntity -> {
                        if (subEntity.getName().equals("Child Department 1")) {
                            assertThat(subEntity.getSubEntities()).isNotNull();
                            assertThat(subEntity.getSubEntities()).hasSize(1);
                            assertThat(subEntity.getSubEntities())
                                    .extracting("name")
                                    .containsExactly("Grandchild Department");
                        } else if (subEntity.getName().equals("Child Department 2")) {
                            assertThat(subEntity.getSubEntities()).isEmpty();
                        }
                    });

                    // Assert pagination details
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getPage()).isEqualTo(page);
                    assertThat(paginatedResponse.getSize()).isEqualTo(size);
                    assertThat(paginatedResponse.getTotalElements()).isEqualTo(2);
                    assertThat(paginatedResponse.getTotalPages()).isEqualTo(1);
                });

        // Act and Assert: Send a GET request to retrieve the paginated sub-entities of "Parent Department" with fetchSubEntities=false
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + parentDepartmentId + "/sub-entities/with-sub-entities/paginated")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParam("sort", pageable.getSort().toString().replace(": ", ","))
                        .queryParam("fetchSubEntities", false)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>>() {
                })
                .value(paginatedResponse -> {
                    // Assert the returned paginated response matches the expected data
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).hasSize(2); // Two child departments

                    // Extract names of the sub-entities
                    List<String> subEntityNames = paginatedResponse.getContent().stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the sub-entities
                    assertThat(subEntityNames).containsExactlyInAnyOrder("Child Department 1", "Child Department 2");

                    // Assert the sub-entities of the sub-entities (should be null since fetchSubEntities=false)
                    paginatedResponse.getContent().forEach(subEntity -> {
                        subEntity.getSubEntities().forEach(subSubEntity -> {
                                    assertThat(subSubEntity.getSubEntities()).isNull();
                                }

                        );
                    });

                    // Assert pagination details
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getPage()).isEqualTo(page);
                    assertThat(paginatedResponse.getSize()).isEqualTo(size);
                    assertThat(paginatedResponse.getTotalElements()).isEqualTo(2);
                    assertThat(paginatedResponse.getTotalPages()).isEqualTo(1);
                });

        // Act and Assert: Send a GET request to retrieve the paginated sub-entities of a non-existing parent entity and expect HTTP status 404
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + -1L + "/sub-entities/with-sub-entities/paginated")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .queryParam("sort", pageable.getSort().toString().replace(": ", ","))
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isNotFound();
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
                .uri("/api/v1/departments/{id}/parent", childDepartmentId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody(HierarchyResponseDto.class)
                .value(response -> {
                    assertThat(response.getId()).isEqualTo(parentDepartmentId);
                    assertThat(response.getName()).isEqualTo("Parent Department");
                    assertThat(response.getParentEntityId()).isNull();
                    assertThat(response.getSubEntities())
                            .extracting(HierarchyResponseDto::getId)
                            .containsExactly(childDepartmentId);
                    assertThat(response.getSubEntities())
                            .extracting(HierarchyResponseDto::getName)
                            .containsExactly("Child Department");
                });
    }

    /**
     * Integration test for searching entities by name with pagination and an option to fetch sub-entities.
     * Sends a GET request with pageable and fetchSubEntities=true and expects HTTP status 200 (OK).
     * Verifies that the response contains the entities matching the search name with their sub-entities if fetchSubEntities=true.
     */
    @Transactional
    @Test
    void searchEntitiesByNameWithPaginationAndSubEntities() {
        // Arrange: Create departments "Engineering" and "IT" with sub-departments
        Long departmentId1 = createDepartment("Engineering", null);
        Long departmentId2 = createDepartment("IT", null);

        Long subDepartmentId1 = createDepartment("Software", departmentId1);
        Long subDepartmentId2 = createDepartment("Hardware", departmentId1);
        Long subDepartmentId3 = createDepartment("Support", departmentId2);

        // Act and Assert: Send a GET request to search for "Engineering" with pagination, fetchSubEntities=true and verify the response

        ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>> responseType =
                new ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>>() {
                };

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/search/with-sub-entities/paginated")
                        .queryParam("name", "Engineering")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .queryParam("sort", "name,asc")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(responseType)
                .value(paginatedResponse -> {
                    // Assert the returned paginated response matches the expected entities
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).hasSize(1);
                    HierarchyResponseDto entity = paginatedResponse.getContent().get(0);
                    assertThat(entity.getName()).isEqualTo("Engineering");
                    assertThat(entity.getSubEntities()).isNotNull();
                    assertThat(entity.getSubEntities()).hasSize(2);
                    assertThat(entity.getSubEntities())
                            .extracting("name")
                            .contains("Software", "Hardware");
                });

        // Act and Assert: Send a GET request to search for "IT" with pagination, fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/search/with-sub-entities/paginated")
                        .queryParam("name", "IT")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .queryParam("sort", "name,asc")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(responseType)
                .value(paginatedResponse -> {
                    // Assert the returned paginated response matches the expected entities
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).hasSize(1);
                    HierarchyResponseDto entity = paginatedResponse.getContent().get(0);
                    assertThat(entity.getName()).isEqualTo("IT");
                    assertThat(entity.getSubEntities()).isNotNull();
                    assertThat(entity.getSubEntities()).hasSize(1);
                    assertThat(entity.getSubEntities())
                            .extracting("name")
                            .contains("Support");
                });

        // Act and Assert: Send a GET request to search for a sub-department "Software" with pagination, fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/search/with-sub-entities/paginated")
                        .queryParam("name", "Software")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .queryParam("sort", "name,asc")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(responseType)
                .value(paginatedResponse -> {
                    // Assert the returned paginated response matches the expected entities
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).hasSize(1);
                    HierarchyResponseDto entity = paginatedResponse.getContent().get(0);
                    assertThat(entity.getName()).isEqualTo("Software");
                    assertThat(entity.getSubEntities()).isNotNull();
                    assertThat(entity.getSubEntities()).isEmpty();
                });

        // Act and Assert: Send a GET request to search for a non-existent department with pagination, fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/search/with-sub-entities/paginated")
                        .queryParam("name", "NonExistent")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .queryParam("sort", "name,asc")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaginatedResponseDto.class)
                .value(paginatedResponse -> {
                    // Assert the returned paginated response is empty
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).isEmpty();
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
                .uri("/api/v1/departments/{id}/parent", departmentId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertThat(response.getStatus()).isEqualTo(404);
                    assertThat(response.getMessage()).contains("has no parent entity");
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
     * Integration test for retrieving the parent entity of a given entity with an option to fetch sub-entities.
     * Sends a GET request with fetchSubEntities=true and expects HTTP status 200 (OK).
     * Verifies that the response contains the parent entity with its sub-entities if fetchSubEntities=true.
     * Handles the case when the parent entity is not found and expects HTTP status 404 (Not Found).
     */
    @Transactional
    @Test
    void getParentEntityWithSubEntities() {
        // Arrange: Create departments "Engineering" and "IT" with sub-departments
        Long departmentId1 = createDepartment("Engineering", null);
        Long departmentId2 = createDepartment("IT", null);

        Long subDepartmentId1 = createDepartment("Software", departmentId1);
        Long subDepartmentId2 = createDepartment("Hardware", departmentId1);
        Long subDepartmentId3 = createDepartment("Support", departmentId2);

        // Act and Assert: Send a GET request to retrieve the parent of "Software" with fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/" + subDepartmentId1 + "/parent/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(HierarchyResponseDto.class)
                .value(parentEntity -> {
                    // Assert the returned parent entity matches the expected entity
                    assertThat(parentEntity).isNotNull();
                    assertThat(parentEntity.getId()).isEqualTo(departmentId1);
                    assertThat(parentEntity.getName()).isEqualTo("Engineering");
                    assertThat(parentEntity.getSubEntities()).isNotNull();
                    assertThat(parentEntity.getSubEntities()).hasSize(2);
                    assertThat(parentEntity.getSubEntities())
                            .extracting("name")
                            .contains("Software", "Hardware");
                });

        // Act and Assert: Send a GET request to retrieve the parent of "Support" with fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/" + subDepartmentId3 + "/parent/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(HierarchyResponseDto.class)
                .value(parentEntity -> {
                    // Assert the returned parent entity matches the expected entity
                    assertThat(parentEntity).isNotNull();
                    assertThat(parentEntity.getId()).isEqualTo(departmentId2);
                    assertThat(parentEntity.getName()).isEqualTo("IT");
                    assertThat(parentEntity.getSubEntities()).isNotNull();
                    assertThat(parentEntity.getSubEntities()).hasSize(1);
                    assertThat(parentEntity.getSubEntities())
                            .extracting("name")
                            .contains("Support");
                });

        // Act and Assert: Send a GET request to retrieve the parent of "Engineering" which has no parent and expect HTTP status 404
        webTestClient.get()
                .uri("/api/v1/departments/" + departmentId1 + "/parent/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isNotFound();
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
        List<HierarchyResponseDto> descendants = webTestClient.get()
                .uri("/api/v1/departments/{id}/descendants", parentId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .returnResult().getResponseBody();

        // Assert: Verify the response contains the expected descendants
        assertThat(descendants).isNotNull();
        assertThat(descendants).hasSize(1); // Assuming only one child for simplicity
        assertThat(descendants.get(0).getId()).isEqualTo(childId);
        assertThat(descendants.get(0).getName()).isEqualTo("Child Department");
        assertThat(descendants.get(0).getParentEntityId()).isEqualTo(parentId);
        assertThat(descendants.get(0).getSubEntities()).isEmpty(); // Assuming no sub-departments

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
     * Integration test for retrieving all descendants of a given entity with an option to fetch sub-entities.
     * Sends a GET request with fetchSubEntities=true and expects HTTP status 200 (OK).
     * Verifies that the response contains the descendants with their sub-entities if fetchSubEntities=true.
     * Handles the case when the entity is not found and expects HTTP status 404 (Not Found).
     */
    @Transactional
    @Test
    void getDescendantsWithSubEntities() {
        // Arrange: Create departments "Engineering" and "IT" with sub-departments and sub-sub-departments
        Long departmentId1 = createDepartment("Engineering", null);
        Long departmentId2 = createDepartment("IT", null);

        Long subDepartmentId1 = createDepartment("Software", departmentId1);
        Long subDepartmentId2 = createDepartment("Hardware", departmentId1);
        Long subDepartmentId3 = createDepartment("Support", departmentId2);

        Long subSubDepartmentId1 = createDepartment("Backend", subDepartmentId1);
        Long subSubDepartmentId2 = createDepartment("Frontend", subDepartmentId1);

        // Act and Assert: Send a GET request to retrieve the descendants of "Engineering" with fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/" + departmentId1 + "/descendants/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .value(descendants -> {
                    // Assert the returned descendants match the expected descendants
                    assertThat(descendants).isNotNull();
                    assertThat(descendants).hasSize(4);

                    // Extract names of the descendants
                    List<String> descendantNames = descendants.stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the descendants
                    assertThat(descendantNames).containsExactlyInAnyOrder("Software", "Hardware", "Backend", "Frontend");

                    // Assert the sub-entities of the descendants
                    descendants.forEach(descendant -> {
                        if (descendant.getName().equals("Software")) {
                            assertThat(descendant.getSubEntities()).isNotNull();
                            assertThat(descendant.getSubEntities()).hasSize(2);
                            assertThat(descendant.getSubEntities())
                                    .extracting("name")
                                    .containsExactlyInAnyOrder("Backend", "Frontend");
                        } else {
                            assertThat(descendant.getSubEntities()).isEmpty();
                        }
                    });
                });

        // Act and Assert: Send a GET request to retrieve the descendants of "IT" with fetchSubEntities=true and verify the response
        webTestClient.get()
                .uri("/api/v1/departments/" + departmentId2 + "/descendants/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .value(descendants -> {
                    // Assert the returned descendants match the expected descendants
                    assertThat(descendants).isNotNull();
                    assertThat(descendants).hasSize(1);

                    // Extract names of the descendants
                    List<String> descendantNames = descendants.stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the descendants
                    assertThat(descendantNames).containsExactlyInAnyOrder("Support");

                    // Assert the sub-entities of the descendants
                    descendants.forEach(descendant -> {
                        assertThat(descendant.getSubEntities()).isEmpty();
                    });
                });

        // Act and Assert: Send a GET request to retrieve the descendants of a non-existing entity and expect HTTP status 404
        webTestClient.get()
                .uri("/api/v1/departments/" + -1L + "/descendants/with-sub-entities?fetchSubEntities=true")
                .exchange()
                .expectStatus().isNotFound();
    }

    /**
     * Integration test for retrieving all descendants of a given entity with an option to fetch sub-entities and apply pagination.
     * Sends a GET request with fetchSubEntities=true and pageable information, expects HTTP status 200 (OK).
     * Verifies that the response contains the paginated descendants with their sub-entities if fetchSubEntities=true.
     * Handles the case when the entity is not found and expects HTTP status 404 (Not Found).
     */
    @Transactional
    @Test
    void getDescendantsWithSubEntitiesAndPagination() {
        // Arrange: Create departments "Engineering" and "IT" with sub-departments and sub-sub-departments
        Long departmentId1 = createDepartment("Engineering", null);
        Long departmentId2 = createDepartment("IT", null);

        Long subDepartmentId1 = createDepartment("Software", departmentId1);
        Long subDepartmentId2 = createDepartment("Hardware", departmentId1);
        Long subDepartmentId3 = createDepartment("Support", departmentId2);

        Long subSubDepartmentId1 = createDepartment("Backend", subDepartmentId1);
        Long subSubDepartmentId2 = createDepartment("Frontend", subDepartmentId1);

        Pageable pageable = PageRequest.of(0, 2); // Page 0, size 2

        ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>> responseType =
                new ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>>() {
                };

        // Act and Assert: Send a GET request to retrieve the descendants of "Engineering" with fetchSubEntities=true and pagination, verify the response
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + departmentId1 + "/descendants/with-sub-entities/paginated")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(responseType)
                .value(paginatedResponse -> {
                    // Assert the returned paginated response matches the expected descendants
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).hasSize(2); // Page size is 2
                    assertThat(paginatedResponse.getTotalElements()).isEqualTo(4); // Total descendants

                    // Extract names of the descendants
                    List<String> descendantNames = paginatedResponse.getContent().stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the descendants in the first page
                    assertThat(descendantNames).containsExactlyInAnyOrder("Software", "Hardware");

                    // Assert the sub-entities of the descendants
                    paginatedResponse.getContent().forEach(descendant -> {
                        if (descendant.getName().equals("Software")) {
                            assertThat(descendant.getSubEntities()).isNotNull();
                            assertThat(descendant.getSubEntities()).hasSize(2);
                            assertThat(descendant.getSubEntities())
                                    .extracting("name")
                                    .containsExactlyInAnyOrder("Backend", "Frontend");
                        } else {
                            assertThat(descendant.getSubEntities()).isEmpty();
                        }
                    });
                });

        // Act and Assert: Send a GET request to retrieve the descendants of "IT" with fetchSubEntities=true and pagination, verify the response
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + departmentId2 + "/descendants/with-sub-entities/paginated")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(responseType)
                .value(paginatedResponse -> {
                    // Assert the returned paginated response matches the expected descendants
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).hasSize(1); // Page size is 2 but only 1 descendant

                    // Extract names of the descendants
                    List<String> descendantNames = paginatedResponse.getContent().stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the descendants
                    assertThat(descendantNames).containsExactlyInAnyOrder("Support");

                    // Assert the sub-entities of the descendants
                    paginatedResponse.getContent().forEach(descendant -> {
                        assertThat(descendant.getSubEntities()).isEmpty();
                    });
                });

        // Act and Assert: Send a GET request to retrieve the descendants of a non-existing entity and expect HTTP status 404
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + -1L + "/descendants/with-sub-entities/paginated")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .build())
                .exchange()
                .expectStatus().isNotFound();
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
        List<HierarchyResponseDto> ancestors = webTestClient.get()
                .uri("/api/v1/departments/{id}/ancestors", departmentId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .returnResult().getResponseBody();

        // Assert: Verify the response contains the expected ancestors
        assertThat(ancestors).isNotNull();
        assertThat(ancestors).hasSize(2); // Expecting two ancestors: Parent and Grandparent

        // Asserting ancestor 1 (Parent Department)
        assertThat(ancestors.get(1).getId()).isEqualTo(parentId);
        assertThat(ancestors.get(1).getName()).isEqualTo("Parent Department");
        assertThat(ancestors.get(1).getParentEntityId()).isEqualTo(grandparentId); // Assuming parentId is child of grandparentId
        assertThat(ancestors.get(1).getSubEntities().contains(
                HierarchyResponseDto.builder()
                        .id(departmentId)
                        .name("Child Department")
                        .parentEntityId(parentId)
                        .subEntities(new ArrayList<>())
                        .build())
        );

        // Asserting ancestor 2 (Grandparent Department)
        assertThat(ancestors.get(0).getId()).isEqualTo(grandparentId);
        assertThat(ancestors.get(0).getName()).isEqualTo("Grandparent Department");
        assertThat(ancestors.get(0).getParentEntityId()).isNull(); // Assuming grandparentId is root department
        assertThat(ancestors.get(0).getSubEntities().contains(
                        HierarchyResponseDto.builder()
                                .id(departmentId)
                                .name("Parent Department")
                                .parentEntityId(grandparentId)
                                .subEntities(
                                        Collections.singletonList(
                                                HierarchyResponseDto.builder()
                                                        .id(departmentId)
                                                        .name("Child Department")
                                                        .parentEntityId(parentId)
                                                        .subEntities(new ArrayList<>())
                                                        .build())
                                )
                                .build()
                )
        );

        // Clean up: Delete the created departments
        departmentRepository.deleteById(departmentId);
        departmentRepository.deleteById(parentId);
        departmentRepository.deleteById(grandparentId);
    }

    /**
     * Integration test for retrieving all ancestors of a given entity with an option to fetch sub-entities.
     * Sends a GET request with fetchSubEntities=true, expects HTTP status 200 (OK).
     * Verifies that the response contains the ancestors with their sub-entities if fetchSubEntities=true.
     * Handles the case when the entity is not found and expects HTTP status 404 (Not Found).
     */
    @Transactional
    @Test
    void getAncestorsWithSubEntities() {
        // Arrange: Create departments with hierarchical relationships
        Long parentDepartmentId = createDepartment("Parent Department", null);
        Long childDepartmentId = createDepartment("Child Department", parentDepartmentId);
        Long grandChildDepartmentId = createDepartment("Grandchild Department", childDepartmentId);

        // Act and Assert: Send a GET request to retrieve the ancestors of "Grandchild Department" with fetchSubEntities=true
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + grandChildDepartmentId + "/ancestors/with-sub-entities")
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .value(ancestors -> {
                    // Assert the returned ancestors match the expected hierarchy
                    assertThat(ancestors).isNotNull();
                    assertThat(ancestors).hasSize(2); // Parent and grandparent

                    // Extract names of the ancestors
                    List<String> ancestorNames = ancestors.stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the ancestors
                    assertThat(ancestorNames).containsExactly("Parent Department", "Child Department");

                    // Assert the sub-entities of the ancestors
                    ancestors.forEach(ancestor -> {
                        if (ancestor.getName().equals("Parent Department")) {
                            assertThat(ancestor.getSubEntities()).isNotNull();
                            assertThat(ancestor.getSubEntities()).hasSize(1);
                            assertThat(ancestor.getSubEntities())
                                    .extracting("name")
                                    .containsExactly("Child Department");
                        } else if (ancestor.getName().equals("Child Department")) {
                            assertThat(ancestor.getSubEntities()).isNotNull();
                            assertThat(ancestor.getSubEntities()).hasSize(1);
                            assertThat(ancestor.getSubEntities())
                                    .extracting("name")
                                    .containsExactly("Grandchild Department");
                        }
                    });
                });

        // Act and Assert: Send a GET request to retrieve the ancestors of "Child Department" with fetchSubEntities=false
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + childDepartmentId + "/ancestors/with-sub-entities")
                        .queryParam("fetchSubEntities", false)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(HierarchyResponseDto.class)
                .value(ancestors -> {
                    // Assert the returned ancestors match the expected hierarchy
                    assertThat(ancestors).isNotNull();
                    assertThat(ancestors).hasSize(1); // Only parent

                    // Extract names of the ancestors
                    List<String> ancestorNames = ancestors.stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the ancestors
                    assertThat(ancestorNames).containsExactly("Parent Department");

                    // Assert the sub-entities of the ancestors (should be empty since fetchSubEntities=false)
                    ancestors.forEach(ancestor -> {
                        ancestor.getSubEntities().forEach(subEntity -> {
                                    assertThat(subEntity.getSubEntities()).isNull();
                                }
                        );
                    });
                });

        // Act and Assert: Send a GET request to retrieve the ancestors of a non-existing entity and expect HTTP status 404
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + -1L + "/ancestors/with-sub-entities")
                        .queryParam("fetchSubEntities", true)
                        .build())
                .exchange()
                .expectStatus().isNotFound();
    }

    /**
     * Integration test for retrieving all ancestors of a given entity with an option to fetch sub-entities and apply pagination.
     * Sends a GET request with fetchSubEntities=true, expects HTTP status 200 (OK).
     * Verifies that the response contains paginated ancestors with their sub-entities if fetchSubEntities=true.
     * Handles the case when the entity is not found and expects HTTP status 404 (Not Found).
     */
    @Transactional
    @Test
    void getAncestorsWithSubEntitiesAndPagination() {
        // Arrange: Create departments with hierarchical relationships
        Long parentDepartmentId = createDepartment("Parent Department", null);
        Long childDepartmentId = createDepartment("Child Department", parentDepartmentId);
        Long grandChildDepartmentId = createDepartment("Grandchild Department", childDepartmentId);

        // Create Pageable object for pagination
        Pageable pageable = PageRequest.of(0, 2, Sort.by("name").ascending());

        // Act and Assert: Send a GET request to retrieve the paginated ancestors of "Grandchild Department" with fetchSubEntities=true
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + grandChildDepartmentId + "/ancestors/with-sub-entities/paginated")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", sortToQueryString(pageable.getSort()))
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>>() {
                })
                .value(paginatedResponse -> {
                    // Assert the returned ancestors match the expected hierarchy
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).hasSize(2); // Parent and grandparent

                    // Extract names of the ancestors
                    List<String> ancestorNames = paginatedResponse.getContent().stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the ancestors
                    assertThat(ancestorNames).containsExactly("Child Department", "Parent Department");

                    // Assert the sub-entities of the ancestors
                    paginatedResponse.getContent().forEach(ancestor -> {
                        if (ancestor.getName().equals("Parent Department")) {
                            assertThat(ancestor.getSubEntities()).isNotNull();
                            assertThat(ancestor.getSubEntities()).hasSize(1);
                            assertThat(ancestor.getSubEntities())
                                    .extracting("name")
                                    .containsExactly("Child Department");
                        } else if (ancestor.getName().equals("Child Department")) {
                            assertThat(ancestor.getSubEntities()).isNotNull();
                            assertThat(ancestor.getSubEntities()).hasSize(1);
                            assertThat(ancestor.getSubEntities())
                                    .extracting("name")
                                    .containsExactly("Grandchild Department");
                        }
                    });

                    // Assert pagination details
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getTotalElements()).isEqualTo(2);
                    assertThat(paginatedResponse.getTotalPages()).isEqualTo(1);
                    assertThat(paginatedResponse.getPage()).isEqualTo(0);
                    assertThat(paginatedResponse.getSize()).isEqualTo(2);
                });

        // Act and Assert: Send a GET request to retrieve the paginated ancestors of "Child Department" with fetchSubEntities=false
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + childDepartmentId + "/ancestors/with-sub-entities/paginated")
                        .queryParam("fetchSubEntities", false)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", sortToQueryString(pageable.getSort()))
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PaginatedResponseDto<HierarchyResponseDto>>() {
                })
                .value(paginatedResponse -> {
                    // Assert the returned ancestors match the expected hierarchy
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getContent()).hasSize(1); // Only parent

                    // Extract names of the ancestors
                    List<String> ancestorNames = paginatedResponse.getContent().stream()
                            .map(HierarchyResponseDto::getName)
                            .collect(Collectors.toList());

                    // Assert the names of the ancestors
                    assertThat(ancestorNames).containsExactly("Parent Department");

                    // Assert the sub-entities of the ancestors (should be null since fetchSubEntities=false)
                    paginatedResponse.getContent().forEach(ancestor -> {
                        ancestor.getSubEntities().forEach(subEntity -> {
                                    assertThat(subEntity.getSubEntities()).isNull();
                                }
                        );
                    });

                    // Assert pagination details
                    assertThat(paginatedResponse).isNotNull();
                    assertThat(paginatedResponse.getTotalElements()).isEqualTo(1);
                    assertThat(paginatedResponse.getTotalPages()).isEqualTo(1);
                    assertThat(paginatedResponse.getPage()).isEqualTo(0);
                    assertThat(paginatedResponse.getSize()).isEqualTo(2);
                });

        // Act and Assert: Send a GET request to retrieve the paginated ancestors of a non-existing entity and expect HTTP status 404
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/departments/" + -1L + "/ancestors/with-sub-entities/paginated")
                        .queryParam("fetchSubEntities", true)
                        .queryParam("page", pageable.getPageNumber())
                        .queryParam("size", pageable.getPageSize())
                        .queryParam("sort", sortToQueryString(pageable.getSort()))
                        .build())
                .exchange()
                .expectStatus().isNotFound();
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
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName(name);
        requestDto.setParentEntityId(parentDepartmentId);

        return webTestClient.post()
                .uri("/api/v1/departments")
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(HierarchyResponseDto.class)
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
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(name)
                .build();

        webTestClient.post()
                .uri("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestDto)
                .exchange()
                .expectStatus().isCreated();
    }

    /**
     * Utility method to convert Sort to a query string format.
     *
     * @param sort the Sort object
     * @return a string representation of the sort in the format "property,direction"
     */
    private String sortToQueryString(Sort sort) {
        return sort.stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .collect(Collectors.joining(","));
    }

}
