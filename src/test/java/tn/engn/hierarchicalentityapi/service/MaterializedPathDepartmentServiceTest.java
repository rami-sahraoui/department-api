package tn.engn.hierarchicalentityapi.service;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.DataIntegrityException;
import tn.engn.hierarchicalentityapi.exception.EntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ParentEntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.mapper.DepartmentMapper;
import tn.engn.hierarchicalentityapi.model.Department;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.model.QHierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaterializedPathDepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @InjectMocks
    private MaterializedPathDepartmentService departmentService;

    private int maxNameLength = 50; // Mocked value for maxNameLength

    @Captor
    ArgumentCaptor<Department> departmentArgumentCaptor;

    @BeforeEach
    void setUp() {
        // Initialize mocks before each test
        MockitoAnnotations.openMocks(this);

        // Mock the value of maxNameLength directly
        departmentService.setMaxNameLength(maxNameLength);

    }

    /**
     * Helper method to mock and stub a select entity from query.
     *
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<HierarchyBaseEntity<?>> mockSelectFromQuery(Department entity) {
        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetchOne()).thenReturn((HierarchyBaseEntity) entity);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QHierarchyBaseEntity.class))).thenReturn(mockQuery);
        return mockQuery;
    }

    /**
     * Helper method to mock and stub a select list of departments from query.
     *
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<HierarchyBaseEntity<?>> mockSelectFromQueryList(List<Department> departments) {
        List<HierarchyBaseEntity<?>> entities = departments.stream().map(
                entity -> (HierarchyBaseEntity<?>) entity
        ).collect(Collectors.toList());

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mock(JPAQuery.class);
        when(mockQuery.orderBy(any(OrderSpecifier.class))).thenReturn(mockQuery);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(entities);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QHierarchyBaseEntity.class))).thenReturn(mockQuery);

        return mockQuery;

    }

    /**
     * Test creating a root entity without a parent.
     */
    @Test
    void testCreateRootDepartment_Success() throws ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto without a parentDepartmentId.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("RootDepartment");

        Department entity = new Department();
        entity.setName("RootDepartment");
        entity.setId(1L); // Simulating the ID assignment upon saving
        entity.setPath("/1/");

        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(entity);
        when(departmentRepository.save(any(Department.class))).thenReturn(entity);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(new HierarchyResponseDto());

        // When: The createDepartment method is called.
        HierarchyResponseDto responseDto = departmentService.createEntity(requestDto);

        // Then: A HierarchyResponseDto should be returned, and the path should be correct.
        assertNotNull(responseDto);
        assertEquals("/1/", entity.getPath());
        verify(departmentRepository, times(2)).save(entity);
        verify(departmentMapper, times(1)).toDto(any(Department.class));
    }

    /**
     * Test creating a child entity with a parent and assert paths.
     */
    @Test
    void testCreateChildDepartment_Success() throws ValidationException, DataIntegrityException, ParentEntityNotFoundException {
        // Given: A valid HierarchyRequestDto with a valid parentDepartmentId.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("ChildDepartment");
        requestDto.setParentEntityId(1L);

        Department parentDepartment = new Department();
        parentDepartment.setId(1L);
        parentDepartment.setName("ParentDepartment");
        parentDepartment.setPath("/1/");

        Department childDepartment = new Department();
        childDepartment.setName("ChildDepartment");
        childDepartment.setParentId(1L);
        childDepartment.setId(2L); // Simulating the ID assignment upon saving
        childDepartment.setPath("/1/2/");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parentDepartment));
        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(childDepartment);
        when(departmentRepository.save(any(Department.class))).thenReturn(childDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(new HierarchyResponseDto());

        // When: The createDepartment method is called.
        HierarchyResponseDto responseDto = departmentService.createEntity(requestDto);

        // Then: A HierarchyResponseDto should be returned, and the path should be correct.
        assertNotNull(responseDto);
        assertEquals("/1/2/", childDepartment.getPath());
        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentRepository, times(2)).save(childDepartment);
        verify(departmentMapper, times(1)).toDto(any(Department.class));
    }

    /**
     * Test creating a real subtree of departments and assert paths.
     */
    @Test
    void testCreateRealSubtree_Success() throws ValidationException, DataIntegrityException, ParentEntityNotFoundException {
        // Given: Multiple valid HierarchyRequestDto objects representing a subtree structure.
        HierarchyRequestDto rootDto = new HierarchyRequestDto();
        rootDto.setName("Root");

        HierarchyRequestDto anotherRootDto = new HierarchyRequestDto();
        anotherRootDto.setName("Another Root");

        HierarchyRequestDto childDto = new HierarchyRequestDto();
        childDto.setName("Child");
        childDto.setParentEntityId(1L);

        HierarchyRequestDto grandChildDto = new HierarchyRequestDto();
        grandChildDto.setName("Grand Child");
        grandChildDto.setParentEntityId(3L);

        Department rootDepartment = new Department();
        rootDepartment.setName("Root");
        rootDepartment.setId(1L);
        rootDepartment.setPath("/1/");

        Department anotherRootDepartment = new Department();
        anotherRootDepartment.setName("Another Root");
        anotherRootDepartment.setPath("/2/");
        anotherRootDepartment.setId(2L);

        Department childDepartment = new Department();
        childDepartment.setName("Child");
        childDepartment.setId(3L);
        childDepartment.setParentId(1L);
        childDepartment.setPath("/1/3/");

        Department grandChildDepartment = new Department();
        grandChildDepartment.setName("Grand Child");
        grandChildDepartment.setId(4L);
        grandChildDepartment.setParentId(3L);
        grandChildDepartment.setPath("/1/3/4/");

        when(departmentMapper.toEntity(any(HierarchyRequestDto.class)))
                .thenReturn(rootDepartment)
                .thenReturn(anotherRootDepartment)
                .thenReturn(childDepartment)
                .thenReturn(grandChildDepartment);

        when(departmentRepository.save(any(Department.class)))
                .thenReturn(rootDepartment)
                .thenReturn(rootDepartment)
                .thenReturn(anotherRootDepartment)
                .thenReturn(anotherRootDepartment)
                .thenReturn(childDepartment)
                .thenReturn(childDepartment)
                .thenReturn(grandChildDepartment)
                .thenReturn(grandChildDepartment);

        when(departmentMapper.toDto(any(Department.class))).thenReturn(new HierarchyResponseDto());

        // When: The createDepartment method is called for each DTO.
        HierarchyResponseDto rootResponse = departmentService.createEntity(rootDto);

        HierarchyResponseDto anotherRootResponse = departmentService.createEntity(anotherRootDto);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(rootDepartment));
        HierarchyResponseDto childResponse = departmentService.createEntity(childDto);

        when(departmentRepository.findById(3L)).thenReturn(Optional.of(childDepartment));
        HierarchyResponseDto grandChildResponse = departmentService.createEntity(grandChildDto);

        // Then: Each HierarchyResponseDto should be returned, and the paths should be correct.
        assertNotNull(rootResponse);
        assertEquals("/1/", rootDepartment.getPath());

        assertNotNull(anotherRootResponse);
        assertEquals("/2/", anotherRootDepartment.getPath());

        assertNotNull(childResponse);
        assertEquals("/1/3/", childDepartment.getPath());

        assertNotNull(grandChildResponse);
        assertEquals("/1/3/4/", grandChildDepartment.getPath());

        verify(departmentRepository, times(8)).save(any(Department.class));
        verify(departmentRepository, times(2)).findById(anyLong());
        verify(departmentMapper, times(4)).toDto(any(Department.class));
    }

    /**
     * Test creating a entity with an invalid name.
     */
    @Test
    void testCreateDepartment_InvalidName() {
        // Given: A HierarchyRequestDto with an invalid name.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("");

        // When & Then: Creating the entity should throw a ValidationException
        assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));
    }

    /**
     * Test creating a entity with a non-existing parent.
     */
    @Test
    void testCreateDepartment_ParentNotFound() {
        // Given: A HierarchyRequestDto with a non-existing parentDepartmentId.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("ChildDepartment");
        requestDto.setParentEntityId(1L);

        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then: Creating the entity should throw a ParentEntityNotFoundException
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.createEntity(requestDto));
    }

    /**
     * Test creating a entity with a name equal to the maximum length.
     */
    @Test
    void testCreateDepartment_NameMaxLength() throws ValidationException, DataIntegrityException {
        // Given: A HierarchyRequestDto with a name equal to the maximum length.
        String maxLengthName = "A".repeat(maxNameLength);
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName(maxLengthName);

        Department entity = new Department();
        entity.setName(maxLengthName);
        entity.setId(1L);
        entity.setPath("/1/");

        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(entity);
        when(departmentRepository.save(any(Department.class))).thenReturn(entity).thenReturn(entity);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(new HierarchyResponseDto());

        // When: The createDepartment method is called.
        HierarchyResponseDto responseDto = departmentService.createEntity(requestDto);

        // Then: A HierarchyResponseDto should be returned successfully.
        assertNotNull(responseDto);
        assertEquals(maxLengthName, entity.getName());
        assertEquals("/1/", entity.getPath());
        verify(departmentRepository, times(2)).save(entity);
        verify(departmentMapper, times(1)).toDto(any(Department.class));
    }

    /**
     * Test creating a entity with a name exceeding the maximum length.
     */
    @Test
    void testCreateDepartment_NameTooLong() {
        // Given: A HierarchyRequestDto with a name exceeding the maximum length.
        String tooLongName = "A".repeat(maxNameLength + 1);
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName(tooLongName);

        // When & Then: Creating the entity should throw a ValidationException
        assertThrows(ValidationException.class, () -> departmentService.createEntity(requestDto));
    }

    /**
     * Unit test for updating entity name only.
     */
    @Test
    public void testUpdateDepartment_NameOnly() {
        // Given: An existing entity with a specified ID and name "Engineering".
        Long departmentId = 1L;
        String name = "Engineering";
        String updatedName = "IT";

        Department existingDepartment = Department
                .builder()
                .id(departmentId)
                .name(name)
                .path("/1/")
                .build();

        Department updatedDepartment = Department
                .builder()
                .id(departmentId)
                .name(updatedName)
                .path("/1/")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        // When: Updating the entity name to "IT".
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName(updatedName);
        HierarchyResponseDto updatedDepartmentResponse = departmentService.updateEntity(departmentId, updatedDto);

        // Then: Verify that the entity's name is updated correctly to "IT".
        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDto.getName(), departmentArgumentCaptor.getValue().getName());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(existingDepartment);
    }

    /**
     * Unit test for updating entity name only in a subtree.
     */
    @Test
    public void testUpdateDepartment_NameOnly_InSubtree() {
        // Given: An existing entity with child departments.
        Long departmentId = 1L;
        String name = "Engineering";
        String updatedName = "IT";
        Department existingDepartment = Department
                .builder()
                .id(departmentId)
                .name(name)
                .path("/1/")
                .build();

        Department childDepartment = Department
                .builder()
                .id(2L)
                .name("Child")
                .path("/1/2/")
                .parentId(departmentId)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);

        // When: Updating the entity name to "IT".
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName(updatedName);
        HierarchyResponseDto updatedDepartmentResponse = departmentService.updateEntity(departmentId, updatedDto);

        // Then: Verify that the entity's name is updated correctly to "IT".
        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDto.getName(), departmentArgumentCaptor.getValue().getName());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(existingDepartment);
    }

    /**
     * Unit test for updating entity by changing its parent.
     */
    @Test
    public void testUpdateDepartment_ChangeParent() {
        // Given: An existing entity with a specified IDs and names.
        Long departmentId = 1L;
        String departmentName = "Software";
        String departmentPath = "/1/";


        Long newParentId = 2L;
        String newParentName = "Engineering";
        String newParentPath = "/2/";

        Department existingDepartment = Department
                .builder()
                .id(departmentId)
                .name(departmentName)
                .path(departmentPath)
                .build();

        Department newParentDepartment = Department
                .builder()
                .id(newParentId)
                .name(newParentName)
                .path(newParentPath)
                .build();

        String updatedDepartmentPath = "/2/1/";

        Department updatedDepartment = Department
                .builder()
                .id(departmentId)
                .name(departmentName)
                .path(updatedDepartmentPath)
                .parentId(newParentId)
                .build();

        List<Department> departmentEntities = Arrays.asList(existingDepartment);

        HierarchyResponseDto responseDto = HierarchyResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentEntityId(newParentId)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(departmentEntities);

        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the entity's parent.
        HierarchyRequestDto updateRequestDto = new HierarchyRequestDto();
        updateRequestDto.setName(departmentName);
        updateRequestDto.setParentEntityId(newParentId);

        HierarchyResponseDto updatedDepartmentResponse = departmentService.updateEntity(departmentId, updateRequestDto);

        // Then: Verify that the entity's parent ID is updated correctly.
        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updateRequestDto.getParentEntityId(), departmentArgumentCaptor.getValue().getParentId());
        assertEquals(updatedDepartment.getPath(), departmentArgumentCaptor.getValue().getPath());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(any(Department.class));

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();
    }

    /**
     * Unit test for updating entity parent within a subtree.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree() {
        // Given: An existing entity with a specified ID and a parent entity.
        Long departmentId = 1L;
        String departmentName = "Software";
        String departmentPath = "/1/";


        Long newParentId = 2L;
        String newParentName = "Engineering";
        String newParentPath = "/2/";

        Long child1DepartmentId = 3L;
        String child1DepartmentName = "Backend";
        String child1DepartmentPath = "/1/3/";
        String updatedChild1DepartmentPath = "/2/3/";

        Long child2DepartmentId = 4L;
        String child2DepartmentName = "Frontend";
        String child2DepartmentPath = "/1/4/";

        Department existingDepartment = Department
                .builder()
                .id(departmentId)
                .name(departmentName)
                .path(departmentPath)
                .build();

        Department newParentDepartment = Department
                .builder()
                .id(newParentId)
                .name(newParentName)
                .path(newParentPath)
                .build();

        Department child1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(child1DepartmentPath)
                .parentId(departmentId)
                .build();

        Department child2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(child2DepartmentPath)
                .parentId(departmentId)
                .build();

        Department updatedChild1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(updatedChild1DepartmentPath)
                .parentId(newParentId)
                .build();


        List<Department> departmentEntities = List.of(child1Department);

        HierarchyResponseDto responseDto = HierarchyResponseDto
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .parentEntityId(newParentId)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));
        when(departmentRepository.findById(child1DepartmentId)).thenReturn(Optional.of(child1Department));
        when(departmentRepository.findById(child2DepartmentId)).thenReturn(Optional.of(child2Department));

        when(departmentRepository.save(any(Department.class))).thenReturn(updatedChild1Department);

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(departmentEntities);

        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the entity's parent.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setId(child1DepartmentId);
        requestDto.setName(child1DepartmentName);
        requestDto.setParentEntityId(newParentId);

        HierarchyResponseDto updatedDepartmentResponse = departmentService.updateEntity(child1DepartmentId, requestDto);

        // Then: Verify that the entity's parent ID is updated correctly.
        assertEquals(requestDto.getName(), updatedDepartmentResponse.getName());
        assertEquals(requestDto.getParentEntityId(), updatedDepartmentResponse.getParentEntityId());

        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedChild1Department.getParentId(), departmentArgumentCaptor.getValue().getParentId());
        assertEquals(updatedChild1Department.getPath(), departmentArgumentCaptor.getValue().getPath());
        verify(departmentRepository, times(1)).findById(child1DepartmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(updatedChild1Department);

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();
    }

    /**
     * Unit test for updating a root entity to become a child of another root entity.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree_RootToChildOfRoot() {
        // Given: An existing entity with a specified ID and a parent entity.
        Long departmentId = 1L;
        String departmentName = "Software";
        String departmentPath = "/1/";

        String updatedDepartmentPath = "/2/1/";


        Long newParentId = 2L;
        String newParentName = "Engineering";
        String newParentPath = "/2/";

        Long child1DepartmentId = 3L;
        String child1DepartmentName = "Backend";
        String child1DepartmentPath = "/1/3/";

        String updatedChild1DepartmentPath = "/2/1/3/";

        Long child2DepartmentId = 4L;
        String child2DepartmentName = "Frontend";
        String child2DepartmentPath = "/1/4/";

        String updatedChild2DepartmentPath = "/2/1/4/";

        Department existingDepartment = Department
                .builder()
                .id(departmentId)
                .name(departmentName)
                .path(departmentPath)
                .build();

        Department updatedDepartment = Department
                .builder()
                .id(departmentId)
                .parentId(newParentId)
                .name(departmentName)
                .path(updatedDepartmentPath)
                .build();

        Department newParentDepartment = Department
                .builder()
                .id(newParentId)
                .name(newParentName)
                .path(newParentPath)
                .build();

        Department child1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(child1DepartmentPath)
                .parentId(departmentId)
                .build();

        Department child2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(child2DepartmentPath)
                .parentId(departmentId)
                .build();

        Department updatedChild1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(updatedChild1DepartmentPath)
                .parentId(departmentId)
                .build();

        Department updatedChild2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(updatedChild2DepartmentPath)
                .parentId(departmentId)
                .build();

        List<Department> departmentEntities = Arrays.asList(existingDepartment, child1Department, child2Department);

        HierarchyResponseDto responseDto = HierarchyResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentEntityId(newParentId)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));
        when(departmentRepository.findById(child1DepartmentId)).thenReturn(Optional.of(child1Department));
        when(departmentRepository.findById(child2DepartmentId)).thenReturn(Optional.of(child2Department));

        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(departmentEntities);

        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the entity's parent.
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setId(departmentId);
        updatedDto.setName(departmentName);
        updatedDto.setParentEntityId(newParentId);

        HierarchyResponseDto updatedDepartmentResponse = departmentService.updateEntity(departmentId, updatedDto);

        // Then: Verify that the entity's parent ID is updated correctly.
        assertEquals(updatedDto.getName(), updatedDepartmentResponse.getName());
        assertEquals(updatedDto.getParentEntityId(), updatedDepartmentResponse.getParentEntityId());

        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDepartment.getParentId(), departmentArgumentCaptor.getValue().getParentId());
        assertEquals(updatedDepartment.getPath(), departmentArgumentCaptor.getValue().getPath());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(existingDepartment);

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();
    }

    /**
     * Unit test for updating a child entity with subtree to be a root entity.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree_ChildToRoot() {
        // Given: An existing entity with a specified ID and a parent entity.
        Long parentId = 1L;
        String parentName = "Engineering";
        String parentPath = "/1/";

        Long departmentId = 2L;
        String departmentName = "Software";
        String departmentPath = "/1/2/";
        String updatedDepartmentPath = "/2/";

        Long child1DepartmentId = 3L;
        String child1DepartmentName = "Backend";
        String child1DepartmentPath = "/1/2/3/";
        String updatedChild1DepartmentPath = "/2/3/";

        Long child2DepartmentId = 4L;
        String child2DepartmentName = "Frontend";
        String child2DepartmentPath = "/1/2/4/";
        String updatedChild2DepartmentPath = "/2/4/";

        Department existingDepartment = Department
                .builder()
                .id(departmentId)
                .name(departmentName)
                .path(departmentPath)
                .parentId(parentId)
                .build();

        Department updatedDepartment = Department
                .builder()
                .id(departmentId)
                .name(departmentName)
                .path(updatedDepartmentPath)
                .parentId(null)
                .build();

        Department parentDepartment = Department
                .builder()
                .id(parentId)
                .name(parentName)
                .path(parentPath)
                .build();

        Department child1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(child1DepartmentPath)
                .parentId(departmentId)
                .build();

        Department child2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(child2DepartmentPath)
                .parentId(departmentId)
                .build();

        Department updatedChild1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(updatedChild1DepartmentPath)
                .parentId(departmentId)
                .build();

        Department updatedChild2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(updatedChild2DepartmentPath)
                .parentId(departmentId)
                .build();

        List<Department> departmentEntities = List.of(existingDepartment, child1Department, child2Department);

        HierarchyResponseDto responseDto = HierarchyResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentEntityId(null)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(child1DepartmentId)).thenReturn(Optional.of(child1Department));
        when(departmentRepository.findById(child2DepartmentId)).thenReturn(Optional.of(child2Department));

        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(departmentEntities);

        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the entity's parent.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setId(departmentId);
        requestDto.setName(departmentName);
        requestDto.setParentEntityId(null);

        HierarchyResponseDto updatedDepartmentResponse = departmentService.updateEntity(departmentId, requestDto);

        // Then: Verify that the entity's parent ID is updated correctly.
        assertEquals(requestDto.getName(), updatedDepartmentResponse.getName());
        assertEquals(requestDto.getParentEntityId(), updatedDepartmentResponse.getParentEntityId());

        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDepartment.getParentId(), departmentArgumentCaptor.getValue().getParentId());
        assertEquals(updatedDepartment.getPath(), departmentArgumentCaptor.getValue().getPath());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(updatedDepartment);

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();
    }

    /**
     * Unit test for updating a entity and verifying sub-departments are correctly mapped.
     */
    @Test
    public void testUpdateDepartment_ParentDepartment_SubDepartments() {
        // Given: An existing entity with a specified ID and a parent entity.
        Long departmentId = 1L;
        String departmentName = "Software";
        String departmentPath = "/1/";

        String updatedDepartmentPath = "/2/1/";


        Long newParentId = 2L;
        String newParentName = "Engineering";
        String newParentPath = "/2/";

        Long child1DepartmentId = 3L;
        String child1DepartmentName = "Backend";
        String child1DepartmentPath = "/1/3/";

        String updatedChild1DepartmentPath = "/2/1/3/";

        Long child2DepartmentId = 4L;
        String child2DepartmentName = "Frontend";
        String child2DepartmentPath = "/1/4/";

        String updatedChild2DepartmentPath = "/2/1/4/";

        Department existingDepartment = Department
                .builder()
                .id(departmentId)
                .name(departmentName)
                .path(departmentPath)
                .build();

        Department updatedDepartment = Department
                .builder()
                .id(departmentId)
                .parentId(newParentId)
                .name(departmentName)
                .path(updatedDepartmentPath)
                .build();

        Department newParentDepartment = Department
                .builder()
                .id(newParentId)
                .name(newParentName)
                .path(newParentPath)
                .build();

        Department child1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(child1DepartmentPath)
                .parentId(departmentId)
                .build();

        Department child2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(child2DepartmentPath)
                .parentId(departmentId)
                .build();

        Department updatedChild1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(updatedChild1DepartmentPath)
                .parentId(departmentId)
                .build();

        Department updatedChild2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(updatedChild2DepartmentPath)
                .parentId(departmentId)
                .build();

        List<Department> departmentEntities = Arrays.asList(existingDepartment, child1Department, child2Department);

        HierarchyResponseDto responseDto = HierarchyResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentEntityId(newParentId)
                .subEntities(
                        departmentEntities.stream()
                                .map(
                                        d ->  HierarchyResponseDto.builder()
                                                .id(d.getId())
                                                .name(d.getName())
                                                .parentEntityId(d.getParentId())
                                                .build()
                                )
                                .collect(Collectors.toList())
                )
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));
        when(departmentRepository.findById(child1DepartmentId)).thenReturn(Optional.of(child1Department));
        when(departmentRepository.findById(child2DepartmentId)).thenReturn(Optional.of(child2Department));

        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(departmentEntities);

        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the entity's parent.
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setId(departmentId);
        updatedDto.setName(departmentName);
        updatedDto.setParentEntityId(newParentId);

        HierarchyResponseDto updatedDepartmentResponse = departmentService.updateEntity(departmentId, updatedDto);

        // Then: Verify that the entity's parent ID is updated correctly.
        assertEquals(updatedDto.getName(), updatedDepartmentResponse.getName());
        assertEquals(updatedDto.getParentEntityId(), updatedDepartmentResponse.getParentEntityId());

        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDepartment.getParentId(), departmentArgumentCaptor.getValue().getParentId());
        assertEquals(updatedDepartment.getPath(), departmentArgumentCaptor.getValue().getPath());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(existingDepartment);

        // Verify sub-departments
        assertNotNull( updatedDepartmentResponse.getSubEntities());
        assertEquals(3, updatedDepartmentResponse.getSubEntities().size());
        List<String> subDepartmentsNames = updatedDepartmentResponse.getSubEntities().stream().map(d -> d.getName()).collect(Collectors.toList());
        assertTrue(subDepartmentsNames.contains(existingDepartment.getName()));
        assertTrue(subDepartmentsNames.contains(child1Department.getName()));
        assertTrue(subDepartmentsNames.contains(child2Department.getName()));

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

    }

    /**
     * Unit test for handling validation exception when updating a entity.
     */
    @Test
    public void testUpdateDepartment_ValidationException() {
        // Given: An invalid entity update request (e.g., empty name).
        Long departmentId = 1L;
        HierarchyRequestDto invalidDto = new HierarchyRequestDto();
        invalidDto.setName(""); // Invalid name

        // When: Attempting to update the entity with an invalid DTO.
        ValidationException thrownException = assertThrows(
                ValidationException.class,
                () -> departmentService.updateEntity(departmentId, invalidDto)
        );

        // Then: Verify that the ValidationException is thrown with the correct message.
        assertEquals("Entity name cannot be null or empty.", thrownException.getMessage());
        verify(departmentRepository, never()).findById(anyLong());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for handling parent not found exception when updating a entity.
     */
    @Test
    public void testUpdateDepartment_ParentDepartmentNotFoundException() {
        // Given: An existing entity and a non-existent parent entity.
        Long departmentId = 1L;
        Long nonExistentParentId = -1L;

        Department existingDepartment = Department
                .builder()
                .id(departmentId)
                .name("Engineering")
                .path("/1/")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(nonExistentParentId)).thenReturn(Optional.empty());

        // When: Attempting to update the entity with a non-existent parent ID.
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Engineering");
        updatedDto.setParentEntityId(nonExistentParentId);

        ParentEntityNotFoundException thrownException = assertThrows(
                ParentEntityNotFoundException.class,
                () -> departmentService.updateEntity(departmentId, updatedDto)
        );

        // Then: Verify that the ParentNotFoundException is thrown with the correct message.
        assertEquals("Parent entity not found", thrownException.getMessage());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(nonExistentParentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for handling circular reference exception when updating a entity.
     */
    @Test
    public void testUpdateDepartment_CircularReferences() {
        // Given: An existing entity and an update request that causes a circular reference.
        Long departmentId = 1L;
        String departmentName = "Engineering";
        String departmentPath = "/1/";


        Long newParentId = 2L;
        String newParentName = "Software";
        String newParentPath = "/1/2/";

        Department existingDepartment = Department
                .builder()
                .id(departmentId)
                .name(departmentName)
                .path(departmentPath)
                .build();

        Department newParentDepartment = Department
                .builder()
                .id(newParentId)
                .name(newParentName)
                .path(newParentPath)
                .parentId(departmentId)
                .build();

        List<Department> departmentEntities = Arrays.asList(existingDepartment, newParentDepartment);

        HierarchyResponseDto responseDto = HierarchyResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentEntityId(newParentId)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(departmentEntities);

        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Attempting to update the entity's parent to a circular reference.
        HierarchyRequestDto updatedDto = new HierarchyRequestDto();
        updatedDto.setName("Engineering");
        updatedDto.setParentEntityId(newParentId);

        DataIntegrityException thrownException = assertThrows(
                DataIntegrityException.class,
                () -> departmentService.updateEntity(departmentId, updatedDto)
        );

        // Then: Verify that the CircularReferenceException is thrown with the correct message.
        assertEquals("Circular reference detected: "
                + "Moving entity " + existingDepartment.getName() + " under entity with ID " + newParentId
                + " would create a circular reference.", thrownException.getMessage());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, never()).save(any(Department.class));

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper,never()).toDto(any(Department.class));
    }

    /**
     * Unit test for deleting a entity with an existent id and with descendants.
     */
    @Test
    public void testDeleteDepartment_ExistingId_WithDescendants() {
        // Given: An existing entity with descendants
        Long departmentId = 1L;
        String departmentPath = "/1/";
        Department entity = Department.builder()
                .id(departmentId)
                .name("Software")
                .path(departmentPath)
                .build();

        Department child1 = Department.builder()
                .id(2L)
                .name("Backend")
                .path("/1/2/")
                .parentId(departmentId)
                .build();

        Department child2 = Department.builder()
                .id(3L)
                .name("Frontend")
                .path("/1/3/")
                .parentId(departmentId)
                .build();

        List<Department> descendants = Arrays.asList(child1, child2);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(entity));
        // Mocking the repository and JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(descendants);

        // When: Deleting the entity
        departmentService.deleteEntity(departmentId);

        // Then: Verify the entity and its descendants are deleted
        verify(departmentRepository, times(1)).findById(departmentId);

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, never()).toDtoList(anyList());

        verify(departmentRepository, times(1)).deleteAll(descendants);
        verify(departmentRepository, times(1)).delete(entity);
    }

    @Test()
    public void testDeleteDepartment_DepartmentNotFound() {
        // Given: No entity found with the given ID
        Long departmentId = 1L;
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Deleting the entity
        EntityNotFoundException thrownException = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.deleteEntity(departmentId)
        );

        // Then: Verify that the EntityNotFoundException is thrown with the correct message.
        assertEquals("Entity not found with ID: " + departmentId, thrownException.getMessage());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).deleteAll(any(List.class));
        verify(departmentRepository, never()).delete(any(Department.class));
    }

    /**
     * Unit test for circular reference detection during entity delete.
     */
    @Test
    public void testDeleteDepartment_CircularReference() {
        // Given: A circular reference in the entity path
        Long departmentId = 1L;
        String departmentPath = "/1/";
        Department entity = Department.builder()
                .id(departmentId)
                .name("Software")
                .path(departmentPath)
                .build();

        Department child = Department.builder()
                .id(2L)
                .name("Backend")
                .path("/1/2/")
                .parentId(departmentId)
                .build();

        Department grandChild = Department.builder()
                .id(3L)
                .name("Frontend")
                .path("/1/2/3/")
                .parentId(2L)
                .build();

        Department circularDescendant = Department.builder()
                .id(4L)
                .name("Circular")
                .path("/1/2/3/4/2/") // Circular reference
                .parentId(3L)
                .build();

        List<Department> descendants = Arrays.asList(child, grandChild, circularDescendant);

        // Mocking the repository and JPAQuery behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(entity));
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(descendants);

        // When: Deleting the entity
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteEntity(departmentId);
        });

        // Then: Verify the exception message
        assertEquals("Circular reference detected in entity path: " + circularDescendant.getPath(), exception.getMessage());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, never()).toDtoList(anyList(), anyBoolean());
    }

    /**
     * Unit test for retrieving all departments when there are no departments.
     */
    @Test
    public void testGetAllDepartments_noDepartments() {
        // Given: No departments in the repository
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());

        // When: Retrieving all departments
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then: Verify that the result is an empty list
        assertTrue(result.isEmpty());
        verify(departmentRepository, times(1)).findAll();
    }

    /**
     * Unit test for retrieving all departments when there are existing departments.
     */
    @Test
    public void testGetAllDepartments_withDepartments() {
        // Given: Departments in the repository
        Department parent = Department.builder().id(1L).name("Parent").path("/1/").build();
        Department child = Department.builder().id(2L).name("Child").path("/1/2/").parentId(1L).build();
        List<Department> departments = Arrays.asList(parent, child);

        HierarchyResponseDto parentDto = HierarchyResponseDto.builder().id(1L).name("Parent").build();
        HierarchyResponseDto childDto = HierarchyResponseDto.builder().id(2L).name("Child").parentEntityId(1L).build();

        when(departmentRepository.findAll()).thenReturn(departments);
        when(departmentMapper.toDtoList(eq(departments), anyBoolean())).thenReturn(Arrays.asList(parentDto, childDto));

        // When: Retrieving all departments
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then: Verify the result
        assertEquals(2, result.size());
        assertEquals("Parent", result.get(0).getName());
        assertEquals("Child", result.get(1).getName());

        verify(departmentRepository, times(1)).findAll();
        verify(departmentMapper, times(1)).toDtoList(anyList(), anyBoolean());
    }

    /**
     * Test case for the getAllEntities method with pageable.
     * Verifies that the method retrieves all departments and maps them to DTOs correctly.
     */
    @Test
    public void testGetAllEntities_Pageable() {
        // Given: Mock pageable and entities
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        List<Department> entities = createMockEntities(); // Mock entity list

        // Given: Mock JPAQueryFactory behavior
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryListPageable(entities);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getAllEntities(pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(jpaQueryFactory, times(2)).selectFrom(any(QHierarchyBaseEntity.class));
        verify(jpaQuery, times(2)).where(any(Predicate.class));
        verify(jpaQuery).orderBy(any(OrderSpecifier[].class));
        verify(jpaQuery).offset(anyLong());
        verify(jpaQuery).limit(anyLong());
        verify(jpaQuery).fetch();
        verify(jpaQuery).fetchCount();
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Helper method to create mock entities with a deep hierarchy.
     *
     * @return a list of mock Department entities.
     */
    private List<Department> createMockEntities() {
        // Mock entity creation logic with a deep hierarchy
        Department dept1 = Department.builder().id(1L).name("Department 1").parentId(null)
                .path("/1/").build();
        Department dept2 = Department.builder().id(2L).name("Department 2").parentId(dept1.getId())
                .path("/1/2/").build();
        Department dept3 = Department.builder().id(3L).name("Department 3").parentId(dept2.getId())
                .path("/1/2/3/").build();
        Department dept4 = Department.builder().id(4L).name("Department 4").parentId(dept2.getId())
                .path("/1/2/4/").build();
        Department dept5 = Department.builder().id(5L).name("Department 5").parentId(dept1.getId())
                .path("/1/5/").build();

        return Arrays.asList(dept1, dept2, dept3, dept4, dept5);
    }

    /**
     * Helper method to create a mock PaginatedResponseDto.
     *
     * @return a mock PaginatedResponseDto.
     */
    private PaginatedResponseDto<HierarchyResponseDto> createMockPaginatedResponseDto() {
        // Mock PaginatedResponseDto creation logic
        List<HierarchyResponseDto> mockDtoList = Arrays.asList(
                HierarchyResponseDto.builder().id(1L).name("Department 1").build(),
                HierarchyResponseDto.builder().id(2L).name("Department 2").build(),
                HierarchyResponseDto.builder().id(3L).name("Department 3").build()
                // Add corresponding DTOs as needed
        );

        return PaginatedResponseDto.<HierarchyResponseDto>builder()
                .content(mockDtoList)
                .page(0)
                .size(10)
                .totalElements(3)
                .totalPages(1)
                .build();
    }

    /**
     * Helper method to mock and stub a select pageable list of departments from query.
     *
     * @param departments the list of mock departments to return from the query.
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<HierarchyBaseEntity<?>> mockSelectFromQueryListPageable(List<Department> departments) {
        // Mocking the JPAQuery behavior
        JPAQuery jpaQuery = mock(JPAQuery.class);
        when(jpaQuery.where(any(Predicate.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(OrderSpecifier[].class))).thenReturn(jpaQuery); // Mock orderBy with the array
        when(jpaQuery.offset(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.limit(anyLong())).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(departments);
        when(jpaQuery.fetchCount()).thenReturn((long) departments.size());
        when(jpaQuery.fetchOne()).thenReturn(departments.get(0));

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QHierarchyBaseEntity.class))).thenReturn(jpaQuery);

        return jpaQuery;
    }

    /**
     * Unit test for retrieving an existing entity by ID.
     */
    @Test
    public void testGetDepartmentById_ExistingDepartment() {
        // Given: An existing entity ID
        Long departmentId = 1L;
        Department entity = Department.builder().id(departmentId).name("Parent").path("/1/").build();
        HierarchyResponseDto responseDto = HierarchyResponseDto.builder().id(departmentId).name("Parent").build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(entity));
        when(departmentMapper.toDto(eq(entity), anyBoolean())).thenReturn(responseDto);

        // When: Retrieving the entity by ID
        HierarchyResponseDto result = departmentService.getEntityById(departmentId);

        // Then: Verify the result
        assertNotNull(result);
        assertEquals(departmentId, result.getId());
        assertEquals("Parent", result.getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentMapper, times(1)).toDto(eq(entity), anyBoolean());
    }

    /**
     * Unit test for retrieving a non-existent entity by ID.
     */
    @Test
    public void testGetDepartmentById_NonExistingDepartment() {
        // Given: A non-existing entity ID
        Long departmentId = -1L;

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Retrieving the entity by ID
        EntityNotFoundException thrown = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getEntityById(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Entity not found with ID: " + departmentId, thrown.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);
    }

    /**
     * Unit test for searching departments by an existing name.
     */
    @Test
    public void testSearchDepartmentsByName_ExistingName() {
        // Given: Departments with matching name
        String departmentName = "Engineering";
        Department department1 = Department.builder().id(1L).name(departmentName).path("/1/").build();
        Department department2 = Department.builder().id(2L).name(departmentName).path("/1/2/").build();

        HierarchyResponseDto responseDto1 = HierarchyResponseDto.builder().id(1L).name(departmentName).build();
        HierarchyResponseDto responseDto2 = HierarchyResponseDto.builder().id(2L).name(departmentName).build();

        List<Department> departments = Arrays.asList(department1, department2);


        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(departments);

        when(departmentMapper.toDtoList(eq(departments), anyBoolean())).thenReturn(Arrays.asList(responseDto1, responseDto2));

        // When: Searching departments by name
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(departmentName);

        // Then: Verify the result
        assertNotNull( result);
        assertEquals(2, result.size());
        assertEquals(departmentName, result.get(0).getName());
        assertEquals(departmentName, result.get(1).getName());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();
        
        verify(departmentMapper, times(1)).toDtoList(eq(departments), anyBoolean());
    }

    /**
     * Unit test for searching departments by a non-existing name.
     */
    @Test
    public void testSearchDepartmentsByName_NonExistingName() {
        // Given: No departments with matching name
        String departmentName = "NonExistingDepartment_" + UUID.randomUUID(); // Generate a unique non-existing name

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(Collections.emptyList());


        // When: Searching departments by name
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(departmentName);

        // Then: Verify the result is an empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();
    }

    /**
     * Test case for the searchEntitiesByName method with pageable.
     * Verifies that the method searches entities by name with pagination, sorting, and optionally fetching sub-entities.
     */
    @Test
    public void testSearchEntitiesByName_Pageable() {
        // Given: Mock pageable and search criteria
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        String name = "Department";
        List<Department> entities = createMockEntities(); // Mock entity list

        // Given: Mock JPAQueryFactory behavior for searching entities by name
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryListPageable(entities);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.searchEntitiesByName(name, pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(jpaQueryFactory, times(2)).selectFrom(any(QHierarchyBaseEntity.class));
        verify(jpaQuery, times(2)).where(any(Predicate.class));
        verify(jpaQuery).orderBy(any(OrderSpecifier[].class));
        verify(jpaQuery).offset(anyLong());
        verify(jpaQuery).limit(anyLong());
        verify(jpaQuery).fetch();
        verify(jpaQuery).fetchCount();
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Unit test for {@link DepartmentService#getSubEntities(Long)} when parent entity exists.
     * Verifies that sub-departments are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetSubDepartments_existingParent() {
        // Given: Existing parent entity
        Long parentId = 1L;
        Department parent = Department.builder().id(parentId).name("Parent").build();
        Department child1 = Department.builder().id(2L).name("Child1").parentId(parentId).build();
        Department child2 = Department.builder().id(3L).name("Child2").parentId(parentId).build();
        List<Department> subDepartments = Arrays.asList(child1, child2);

        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(subDepartments);

        HierarchyResponseDto child1Dto = HierarchyResponseDto.builder().id(2L).name("Child1").build();
        HierarchyResponseDto child2Dto = HierarchyResponseDto.builder().id(3L).name("Child2").build();

        when(departmentMapper.toDtoList(eq(subDepartments), anyBoolean())).thenReturn(Arrays.asList(child1Dto, child2Dto));

        // When: Getting sub-departments
        List<HierarchyResponseDto> result = departmentService.getSubEntities(parentId);

        // Then: Verify the result
        assertEquals(2, result.size());
        assertEquals("Child1", result.get(0).getName());
        assertEquals("Child2", result.get(1).getName());

        verify(departmentRepository, times(1)).findById(parentId);
        
        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, times(1)).toDtoList(eq(subDepartments), anyBoolean());
    }

    /**
     * Unit test for {@link DepartmentService#getSubEntities(Long)} when parent entity does not exist.
     * Verifies that {@link ParentEntityNotFoundException} is thrown.
     */
    @Test
    public void testGetSubDepartments_nonExistingParent() {
        // Given: Non-existing parent entity ID
        Long parentId = -1L;
        when(departmentRepository.findById(parentId)).thenReturn(Optional.empty());

        // When: Getting sub-departments
        ParentEntityNotFoundException thrown = assertThrows(
                ParentEntityNotFoundException.class,
                () -> departmentService.getSubEntities(parentId)
        );

        // Then: Verify the exception
        assertEquals("Parent entity not found with id: " + parentId, thrown.getMessage());

        verify(departmentRepository, times(1)).findById(parentId);

        verify(departmentMapper, never()).toDtoList(anyList(), anyBoolean());
    }

    /**
     * Test case for the getSubEntities method with pageable.
     * Verifies that the method retrieves sub-entities (children) of a given parent entity
     * with pagination, sorting, and optionally fetching sub-entities.
     */
    @Test
    public void testGetSubEntities_Pageable() {
        // Given: Mock pageable, parent entity ID, and sub-entities
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Long parentId = 1L;
        List<Department> subEntities = createMockEntities(); // Mock sub-entity list

        // Given: Mock parent entity
        Department parentEntity = subEntities.get(0);

        // Given: Mock repository behavior for finding parent entity
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentEntity));

        // Given: Mock JPAQueryFactory behavior for fetching sub-entities
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryListPageable(subEntities);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getSubEntities(parentId, pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(departmentRepository).findById(parentId);
        verify(jpaQueryFactory, times(2)).selectFrom(any(QHierarchyBaseEntity.class));
        verify(jpaQuery, times(2)).where(any(Predicate.class));
        verify(jpaQuery).orderBy(any(OrderSpecifier[].class));
        verify(jpaQuery).offset(anyLong());
        verify(jpaQuery).limit(anyLong());
        verify(jpaQuery).fetch();
        verify(jpaQuery).fetchCount();
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Unit test for {@link DepartmentService#getDescendants(Long)} when entity exists.
     * Verifies that descendants are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetDescendants_existingDepartment() {
        // Given: Existing entity
        Long departmentId = 1L;
        Department entity = Department.builder().id(departmentId).name("Department").path("/1/").build();
        Department child1 = Department.builder().id(2L).name("Child1").path("/1/2/").build();
        Department child2 = Department.builder().id(3L).name("Child2").path("/1/3/").build();
        List<Department> descendants = Arrays.asList(entity, child1, child2);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(entity));
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(descendants);

        HierarchyResponseDto departmentDto = HierarchyResponseDto.builder().id(departmentId).name("Department").build();
        HierarchyResponseDto child1Dto = HierarchyResponseDto.builder().id(2L).name("Child1").build();
        HierarchyResponseDto child2Dto = HierarchyResponseDto.builder().id(3L).name("Child2").build();

        when(departmentMapper.toDtoList(eq(descendants), anyBoolean())).thenReturn(Arrays.asList(child1Dto, child2Dto));

        // When: Getting descendants
        List<HierarchyResponseDto> result = departmentService.getDescendants(departmentId);

        // Then: Verify the result
        assertEquals(2, result.size());
        assertEquals("Child1", result.get(0).getName());
        assertEquals("Child2", result.get(1).getName());

        verify(departmentRepository, times(1)).findById(departmentId);

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, times(1)).toDtoList(eq(descendants), anyBoolean());
    }

    /**
     * Unit test for {@link DepartmentService#getDescendants(Long)} when entity does not exist.
     * Verifies that {@link EntityNotFoundException} is thrown.
     */
    @Test
    public void testGetDescendants_nonExistingDepartment() {
        // Given: Non-existing entity ID
        Long departmentId = -1L;
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Getting descendants
        EntityNotFoundException thrown = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getDescendants(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Entity not found with ID: " + departmentId, thrown.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);

        verify(departmentMapper, never()).toDtoList(anyList(), anyBoolean());
    }

    /**
     * Test case for the getDescendants method with pageable.
     * Verifies that the method retrieves all descendants of a given entity with pagination, sorting, and optionally fetching sub-entities.
     */
    @Test
    public void testGetDescendants_Pageable() {
        // Given: Mock pageable and entity ID
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Long entityId = 1L;

        // Given: Mock entities and descendants
        List<Department> entities = createMockEntities(); // Mock entity list
        List<Department> descendants = createMockDescendants(); // Mock descendants list

        // Given: Mock entity retrieval
        when(departmentRepository.findById(entityId))
                .thenReturn(Optional.of(entities.get(0))); // Return the root entity

        // Given: Mock JPAQueryFactory behavior for retrieving entity and its descendants
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryListPageable(descendants);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getDescendants(entityId, pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(departmentRepository).findById(entityId);
        verify(jpaQueryFactory, times(1)).selectFrom(any(QHierarchyBaseEntity.class));
        verify(jpaQuery, times(1)).where(any(Predicate.class));
        verify(jpaQuery, times(1)).fetch();
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Helper method to create mock descendants.
     *
     * @return a list of mock descendant entities.
     */
    private List<Department> createMockDescendants() {
        // Replace with your mock descendant creation logic
        return Arrays.asList(
                Department.builder().id(1L).name("Department 1").parentId(null).build(),
                Department.builder().id(2L).name("Department 2").parentId(null).build(),
                Department.builder().id(3L).name("Department 3").parentId(null).build()
                // Add more descendants as needed
        );
    }

    /**
     * Unit test for {@link DepartmentService#getAncestors(Long)} when entity exists.
     * Verifies that ancestors are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetAncestors_existingDepartment() {
        // Given: Existing entity
        Long parentId = 1L;
        Long departmentId = 2L;
        Department parent = Department.builder().id(parentId).name("Department").path("/1/").build();
        Department entity = Department.builder().id(departmentId).name("Department").path("/1/2/").parentId(parentId).build();
        List<Department> ancestors = Arrays.asList(parent);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(entity));
        when(departmentRepository.findAllById(Arrays.asList(1L))).thenReturn(ancestors);

        HierarchyResponseDto parentDto = HierarchyResponseDto.builder().id(departmentId).name("Parent").build();
        when(departmentMapper.toDtoList(eq(ancestors), anyBoolean())).thenReturn(Arrays.asList(parentDto));

        // When: Getting ancestors
        List<HierarchyResponseDto> result = departmentService.getAncestors(departmentId);

        // Then: Verify the result
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Parent", result.get(0).getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findAllById(Arrays.asList(1L));
        verify(departmentMapper, times(1)).toDtoList(anyList(), anyBoolean());
    }

    /**
     * Unit test for {@link DepartmentService#getAncestors(Long)} when entity does not exist.
     * Verifies that {@link EntityNotFoundException} is thrown.
     */
    @Test
    public void testGetAncestors_nonExistingDepartment() {
        // Given: Non-existing entity ID
        Long departmentId = -1L;
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Getting ancestors
        EntityNotFoundException thrown = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getAncestors(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Entity not found with ID: " + departmentId, thrown.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).findAllById(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Test case for the getAncestors method with pageable.
     * Verifies that the method retrieves all ancestors of a given entity with pagination, sorting, and optionally fetching sub-entities.
     */
    @Test
    public void testGetAncestors_Pageable() {
        // Given: Mock pageable and entity ID
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Long entityId = 1L;

        // Given: Mock entities and ancestors
        List<Department> entities = createMockEntities(); // Mock entity list
        List<Department> ancestors = createMockAncestors(); // Mock ancestors list

        // Given: Mock entity retrieval
        when(departmentRepository.findById(entityId))
                .thenReturn(Optional.of(entities.get(0))); // Return the root entity

        // Given: Mock retrieving ancestors
        when(departmentRepository.findAllById(anyList()))
                .thenReturn(ancestors);

        // Given: Mock mapper behavior
        when(departmentMapper.toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true)))
                .thenReturn(createMockPaginatedResponseDto()); // Expected PaginatedResponseDto

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getAncestors(entityId, pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(departmentRepository).findById(entityId);
        verify(departmentRepository).findAllById(anyList());
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Helper method to create mock ancestors.
     *
     * @return a list of mock ancestor entities.
     */
    private List<Department> createMockAncestors() {
        // Replace with your mock ancestor creation logic
        return Arrays.asList(
                Department.builder().id(1L).name("Department 1").parentId(null).build(),
                Department.builder().id(2L).name("Department 2").parentId(null).build(),
                Department.builder().id(3L).name("Department 3").parentId(null).build()
                // Add more ancestors as needed
        );
    }

    /**
     * Unit test for {@link DepartmentService#getParentEntity(Long)} when parent entity exists.
     * Verifies that parent entity is correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetParentDepartment_existingParent() {
        // Given: Existing entity with parent
        Long departmentId = 2L;
        Long parentId = 1L;
        Department entity = Department.builder().id(departmentId).name("Department").parentId(parentId).build();
        Department parent = Department.builder().id(parentId).name("Parent").build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(entity));
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parent));

        HierarchyResponseDto parentDto = HierarchyResponseDto.builder().id(parentId).name("Parent").build();
        when(departmentMapper.toDto(eq(parent), anyBoolean())).thenReturn(parentDto);

        // When: Getting parent entity
        HierarchyResponseDto result = departmentService.getParentEntity(departmentId);

        // Then: Verify the result
        assertEquals(parentId, result.getId());
        assertEquals("Parent", result.getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentMapper, times(1)).toDto(eq(parent), anyBoolean());
    }

    /**
     * Unit test for {@link DepartmentService#getParentEntity(Long)} when entity does not exist.
     * Verifies that {@link EntityNotFoundException} is thrown.
     */
    @Test
    public void testGetParentDepartment_nonExistingDepartment() {
        // Given: Non-existing entity ID
        Long departmentId = -1L;
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Getting parent entity
        EntityNotFoundException thrown = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getParentEntity(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Entity not found with ID: " + departmentId, thrown.getMessage());

        verify(departmentRepository, times(1)).findById(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Unit test for {@link DepartmentService#getParentEntity(Long)} when entity parent does not exist.
     * Verifies that {@link ParentEntityNotFoundException} is thrown.
     */
    @Test
    public void testGetParentDepartment_noParent() {
        // Given: Department with no parent
        Long departmentId = 1L;
        Department entity = Department.builder().id(departmentId).name("Department").build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(entity));

        // When: Getting parent entity
        ParentEntityNotFoundException thrown = assertThrows(
                ParentEntityNotFoundException.class,
                () -> departmentService.getParentEntity(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Entity with ID " + departmentId + " has no parent entity", thrown.getMessage());

        verify(departmentRepository, times(1)).findById(any());
        verify(departmentMapper, never()).toDto(any());
    }
}
