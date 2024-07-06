package tn.engn.departmentapi.service;

import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import tn.engn.departmentapi.dto.DepartmentRequestDto;
import tn.engn.departmentapi.dto.DepartmentResponseDto;
import tn.engn.departmentapi.exception.DataIntegrityException;
import tn.engn.departmentapi.exception.DepartmentNotFoundException;
import tn.engn.departmentapi.exception.ParentDepartmentNotFoundException;
import tn.engn.departmentapi.exception.ValidationException;
import tn.engn.departmentapi.mapper.DepartmentMapper;
import tn.engn.departmentapi.model.Department;
import tn.engn.departmentapi.model.QDepartment;
import tn.engn.departmentapi.repository.DepartmentRepository;

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
     * Test creating a root department without a parent.
     */
    @Test
    void testCreateRootDepartment_Success() throws ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto without a parentDepartmentId.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("RootDepartment");

        Department department = new Department();
        department.setName("RootDepartment");
        department.setId(1L); // Simulating the ID assignment upon saving
        department.setPath("/1/");

        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(department);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(new DepartmentResponseDto());

        // When: The createDepartment method is called.
        DepartmentResponseDto responseDto = departmentService.createDepartment(requestDto);

        // Then: A DepartmentResponseDto should be returned, and the path should be correct.
        assertNotNull(responseDto);
        assertEquals("/1/", department.getPath());
        verify(departmentRepository, times(2)).save(department);
    }

    /**
     * Test creating a child department with a parent and assert paths.
     */
    @Test
    void testCreateChildDepartment_Success() throws ValidationException, DataIntegrityException, ParentDepartmentNotFoundException {
        // Given: A valid DepartmentRequestDto with a valid parentDepartmentId.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("ChildDepartment");
        requestDto.setParentDepartmentId(1L);

        Department parentDepartment = new Department();
        parentDepartment.setId(1L);
        parentDepartment.setName("ParentDepartment");
        parentDepartment.setPath("/1/");

        Department childDepartment = new Department();
        childDepartment.setName("ChildDepartment");
        childDepartment.setParentDepartmentId(1L);
        childDepartment.setId(2L); // Simulating the ID assignment upon saving
        childDepartment.setPath("/1/2/");

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(parentDepartment));
        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(childDepartment);
        when(departmentRepository.save(any(Department.class))).thenReturn(childDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(new DepartmentResponseDto());

        // When: The createDepartment method is called.
        DepartmentResponseDto responseDto = departmentService.createDepartment(requestDto);

        // Then: A DepartmentResponseDto should be returned, and the path should be correct.
        assertNotNull(responseDto);
        assertEquals("/1/2/", childDepartment.getPath());
        verify(departmentRepository, times(1)).findById(1L);
        verify(departmentRepository, times(2)).save(childDepartment);
    }

    /**
     * Test creating a real subtree of departments and assert paths.
     */
    @Test
    void testCreateRealSubtree_Success() throws ValidationException, DataIntegrityException, ParentDepartmentNotFoundException {
        // Given: Multiple valid DepartmentRequestDto objects representing a subtree structure.
        DepartmentRequestDto rootDto = new DepartmentRequestDto();
        rootDto.setName("Root");

        DepartmentRequestDto anotherRootDto = new DepartmentRequestDto();
        anotherRootDto.setName("Another Root");

        DepartmentRequestDto childDto = new DepartmentRequestDto();
        childDto.setName("Child");
        childDto.setParentDepartmentId(1L);

        DepartmentRequestDto grandChildDto = new DepartmentRequestDto();
        grandChildDto.setName("Grand Child");
        grandChildDto.setParentDepartmentId(3L);

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
        childDepartment.setParentDepartmentId(1L);
        childDepartment.setPath("/1/3/");

        Department grandChildDepartment = new Department();
        grandChildDepartment.setName("Grand Child");
        grandChildDepartment.setId(4L);
        grandChildDepartment.setParentDepartmentId(3L);
        grandChildDepartment.setPath("/1/3/4/");

        when(departmentMapper.toEntity(any(DepartmentRequestDto.class)))
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

        when(departmentMapper.toDto(any(Department.class))).thenReturn(new DepartmentResponseDto());

        // When: The createDepartment method is called for each DTO.
        DepartmentResponseDto rootResponse = departmentService.createDepartment(rootDto);

        DepartmentResponseDto anotherRootResponse = departmentService.createDepartment(anotherRootDto);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(rootDepartment));
        DepartmentResponseDto childResponse = departmentService.createDepartment(childDto);

        when(departmentRepository.findById(3L)).thenReturn(Optional.of(childDepartment));
        DepartmentResponseDto grandChildResponse = departmentService.createDepartment(grandChildDto);

        // Then: Each DepartmentResponseDto should be returned, and the paths should be correct.
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
    }

    /**
     * Test creating a department with an invalid name.
     */
    @Test
    void testCreateDepartment_InvalidName() {
        // Given: A DepartmentRequestDto with an invalid name.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("");

        // When & Then: Creating the department should throw a ValidationException
        assertThrows(ValidationException.class, () -> departmentService.createDepartment(requestDto));
    }

    /**
     * Test creating a department with a non-existing parent.
     */
    @Test
    void testCreateDepartment_ParentNotFound() {
        // Given: A DepartmentRequestDto with a non-existing parentDepartmentId.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("ChildDepartment");
        requestDto.setParentDepartmentId(1L);

        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then: Creating the department should throw a ParentDepartmentNotFoundException
        assertThrows(ParentDepartmentNotFoundException.class, () -> departmentService.createDepartment(requestDto));
    }

    /**
     * Test creating a department with a name equal to the maximum length.
     */
    @Test
    void testCreateDepartment_NameMaxLength() throws ValidationException, DataIntegrityException {
        // Given: A DepartmentRequestDto with a name equal to the maximum length.
        String maxLengthName = "A".repeat(maxNameLength);
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName(maxLengthName);

        Department department = new Department();
        department.setName(maxLengthName);
        department.setId(1L);
        department.setPath("/1/");

        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(department);
        when(departmentRepository.save(any(Department.class))).thenReturn(department).thenReturn(department);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(new DepartmentResponseDto());

        // When: The createDepartment method is called.
        DepartmentResponseDto responseDto = departmentService.createDepartment(requestDto);

        // Then: A DepartmentResponseDto should be returned successfully.
        assertNotNull(responseDto);
        assertEquals(maxLengthName, department.getName());
        assertEquals("/1/", department.getPath());
        verify(departmentRepository, times(2)).save(department);
    }

    /**
     * Test creating a department with a name exceeding the maximum length.
     */
    @Test
    void testCreateDepartment_NameTooLong() {
        // Given: A DepartmentRequestDto with a name exceeding the maximum length.
        String tooLongName = "A".repeat(maxNameLength + 1);
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName(tooLongName);

        // When & Then: Creating the department should throw a ValidationException
        assertThrows(ValidationException.class, () -> departmentService.createDepartment(requestDto));
    }

    /**
     * Unit test for updating department name only.
     */
    @Test
    public void testUpdateDepartment_NameOnly() {
        // Given: An existing department with a specified ID and name "Engineering".
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

        // When: Updating the department name to "IT".
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName(updatedName);
        DepartmentResponseDto updatedDepartmentResponse = departmentService.updateDepartment(departmentId, updatedDto);

        // Then: Verify that the department's name is updated correctly to "IT".
        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDto.getName(), departmentArgumentCaptor.getValue().getName());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(existingDepartment);
    }

    /**
     * Unit test for updating department name only in a subtree.
     */
    @Test
    public void testUpdateDepartment_NameOnly_InSubtree() {
        // Given: An existing department with child departments.
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
                .parentDepartmentId(departmentId)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);

        // When: Updating the department name to "IT".
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName(updatedName);
        DepartmentResponseDto updatedDepartmentResponse = departmentService.updateDepartment(departmentId, updatedDto);

        // Then: Verify that the department's name is updated correctly to "IT".
        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDto.getName(), departmentArgumentCaptor.getValue().getName());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(existingDepartment);
    }

    /**
     * Unit test for updating department by changing its parent.
     */
    @Test
    public void testUpdateDepartment_ChangeParent() {
        // Given: An existing department with a specified IDs and names.
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
                .parentDepartmentId(newParentId)
                .build();

        List<Department> departmentEntities = Arrays.asList(existingDepartment);

        DepartmentResponseDto responseDto = DepartmentResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentDepartmentId(newParentId)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        // Mocking the JPAQuery behavior
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(departmentEntities);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the department's parent.
        DepartmentRequestDto updateRequestDto = new DepartmentRequestDto();
        updateRequestDto.setName(departmentName);
        updateRequestDto.setParentDepartmentId(newParentId);

        DepartmentResponseDto updatedDepartmentResponse = departmentService.updateDepartment(departmentId, updateRequestDto);

        // Then: Verify that the department's parent ID is updated correctly.
        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updateRequestDto.getParentDepartmentId(), departmentArgumentCaptor.getValue().getParentDepartmentId());
        assertEquals(updatedDepartment.getPath(), departmentArgumentCaptor.getValue().getPath());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    /**
     * Unit test for updating department parent within a subtree.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree() {
        // Given: An existing department with a specified ID and a parent department.
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
                .parentDepartmentId(departmentId)
                .build();

        Department child2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(child2DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        Department updatedChild1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(updatedChild1DepartmentPath)
                .parentDepartmentId(newParentId)
                .build();


        List<Department> departmentEntities = List.of(child1Department);

        DepartmentResponseDto responseDto = DepartmentResponseDto
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .parentDepartmentId(newParentId)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));
        when(departmentRepository.findById(child1DepartmentId)).thenReturn(Optional.of(child1Department));
        when(departmentRepository.findById(child2DepartmentId)).thenReturn(Optional.of(child2Department));

        when(departmentRepository.save(any(Department.class))).thenReturn(updatedChild1Department);

        // Mocking the JPAQuery behavior
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(departmentEntities);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the department's parent.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setId(child1DepartmentId);
        requestDto.setName(child1DepartmentName);
        requestDto.setParentDepartmentId(newParentId);

        DepartmentResponseDto updatedDepartmentResponse = departmentService.updateDepartment(child1DepartmentId, requestDto);

        // Then: Verify that the department's parent ID is updated correctly.
        assertEquals(requestDto.getName(), updatedDepartmentResponse.getName());
        assertEquals(requestDto.getParentDepartmentId(), updatedDepartmentResponse.getParentDepartmentId());

        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedChild1Department.getParentDepartmentId(), departmentArgumentCaptor.getValue().getParentDepartmentId());
        assertEquals(updatedChild1Department.getPath(), departmentArgumentCaptor.getValue().getPath());
        verify(departmentRepository, times(1)).findById(child1DepartmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(updatedChild1Department);
    }

    /**
     * Unit test for updating a root department to become a child of another root department.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree_RootToChildOfRoot() {
        // Given: An existing department with a specified ID and a parent department.
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
                .parentDepartmentId(newParentId)
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
                .parentDepartmentId(departmentId)
                .build();

        Department child2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(child2DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        Department updatedChild1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(updatedChild1DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        Department updatedChild2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(updatedChild2DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        List<Department> departmentEntities = Arrays.asList(existingDepartment, child1Department, child2Department);

        DepartmentResponseDto responseDto = DepartmentResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentDepartmentId(newParentId)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));
        when(departmentRepository.findById(child1DepartmentId)).thenReturn(Optional.of(child1Department));
        when(departmentRepository.findById(child2DepartmentId)).thenReturn(Optional.of(child2Department));

        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        // Mocking the JPAQuery behavior
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(departmentEntities);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the department's parent.
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setId(departmentId);
        updatedDto.setName(departmentName);
        updatedDto.setParentDepartmentId(newParentId);

        DepartmentResponseDto updatedDepartmentResponse = departmentService.updateDepartment(departmentId, updatedDto);

        // Then: Verify that the department's parent ID is updated correctly.
        assertEquals(updatedDto.getName(), updatedDepartmentResponse.getName());
        assertEquals(updatedDto.getParentDepartmentId(), updatedDepartmentResponse.getParentDepartmentId());

        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDepartment.getParentDepartmentId(), departmentArgumentCaptor.getValue().getParentDepartmentId());
        assertEquals(updatedDepartment.getPath(), departmentArgumentCaptor.getValue().getPath());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(existingDepartment);
    }

    /**
     * Unit test for updating a child department with subtree to be a root department.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree_ChildToRoot() {
        // Given: An existing department with a specified ID and a parent department.
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
                .parentDepartmentId(parentId)
                .build();

        Department updatedDepartment = Department
                .builder()
                .id(departmentId)
                .name(departmentName)
                .path(updatedDepartmentPath)
                .parentDepartmentId(null)
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
                .parentDepartmentId(departmentId)
                .build();

        Department child2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(child2DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        Department updatedChild1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(updatedChild1DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        Department updatedChild2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(updatedChild2DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        List<Department> departmentEntities = List.of(existingDepartment, child1Department, child2Department);

        DepartmentResponseDto responseDto = DepartmentResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentDepartmentId(null)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(child1DepartmentId)).thenReturn(Optional.of(child1Department));
        when(departmentRepository.findById(child2DepartmentId)).thenReturn(Optional.of(child2Department));

        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);

        // Mocking the JPAQuery behavior
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(departmentEntities);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the department's parent.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setId(departmentId);
        requestDto.setName(departmentName);
        requestDto.setParentDepartmentId(null);

        DepartmentResponseDto updatedDepartmentResponse = departmentService.updateDepartment(departmentId, requestDto);

        // Then: Verify that the department's parent ID is updated correctly.
        assertEquals(requestDto.getName(), updatedDepartmentResponse.getName());
        assertEquals(requestDto.getParentDepartmentId(), updatedDepartmentResponse.getParentDepartmentId());

        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDepartment.getParentDepartmentId(), departmentArgumentCaptor.getValue().getParentDepartmentId());
        assertEquals(updatedDepartment.getPath(), departmentArgumentCaptor.getValue().getPath());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(updatedDepartment);
    }

    /**
     * Unit test for updating a department and verifying sub-departments are correctly mapped.
     */
    @Test
    public void testUpdateDepartment_ParentDepartment_SubDepartments() {
        // Given: An existing department with a specified ID and a parent department.
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
                .parentDepartmentId(newParentId)
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
                .parentDepartmentId(departmentId)
                .build();

        Department child2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(child2DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        Department updatedChild1Department = Department
                .builder()
                .id(child1DepartmentId)
                .name(child1DepartmentName)
                .path(updatedChild1DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        Department updatedChild2Department = Department
                .builder()
                .id(child2DepartmentId)
                .name(child2DepartmentName)
                .path(updatedChild2DepartmentPath)
                .parentDepartmentId(departmentId)
                .build();

        List<Department> departmentEntities = Arrays.asList(existingDepartment, child1Department, child2Department);

        DepartmentResponseDto responseDto = DepartmentResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentDepartmentId(newParentId)
                .subDepartments(
                        departmentEntities.stream()
                                .map(
                                        d ->  DepartmentResponseDto.builder()
                                                .id(d.getId())
                                                .name(d.getName())
                                                .parentDepartmentId(d.getParentDepartmentId())
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
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(departmentEntities);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Updating the department's parent.
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setId(departmentId);
        updatedDto.setName(departmentName);
        updatedDto.setParentDepartmentId(newParentId);

        DepartmentResponseDto updatedDepartmentResponse = departmentService.updateDepartment(departmentId, updatedDto);

        // Then: Verify that the department's parent ID is updated correctly.
        assertEquals(updatedDto.getName(), updatedDepartmentResponse.getName());
        assertEquals(updatedDto.getParentDepartmentId(), updatedDepartmentResponse.getParentDepartmentId());

        verify(departmentMapper).toDto(departmentArgumentCaptor.capture());
        assertEquals(updatedDepartment.getParentDepartmentId(), departmentArgumentCaptor.getValue().getParentDepartmentId());
        assertEquals(updatedDepartment.getPath(), departmentArgumentCaptor.getValue().getPath());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, times(1)).save(existingDepartment);

        // Verify sub-departments
        assertNotNull( updatedDepartmentResponse.getSubDepartments());
        assertEquals(3, updatedDepartmentResponse.getSubDepartments().size());
        List<String> subDepartmentsNames = updatedDepartmentResponse.getSubDepartments().stream().map(d -> d.getName()).collect(Collectors.toList());
        assertTrue(subDepartmentsNames.contains(existingDepartment.getName()));
        assertTrue(subDepartmentsNames.contains(child1Department.getName()));
        assertTrue(subDepartmentsNames.contains(child2Department.getName()));

    }

    /**
     * Unit test for handling validation exception when updating a department.
     */
    @Test
    public void testUpdateDepartment_ValidationException() {
        // Given: An invalid department update request (e.g., empty name).
        Long departmentId = 1L;
        DepartmentRequestDto invalidDto = new DepartmentRequestDto();
        invalidDto.setName(""); // Invalid name

        // When: Attempting to update the department with an invalid DTO.
        ValidationException thrownException = assertThrows(
                ValidationException.class,
                () -> departmentService.updateDepartment(departmentId, invalidDto)
        );

        // Then: Verify that the ValidationException is thrown with the correct message.
        assertEquals("Department name cannot be null or empty.", thrownException.getMessage());
        verify(departmentRepository, never()).findById(anyLong());
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for handling parent not found exception when updating a department.
     */
    @Test
    public void testUpdateDepartment_ParentDepartmentNotFoundException() {
        // Given: An existing department and a non-existent parent department.
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

        // When: Attempting to update the department with a non-existent parent ID.
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Engineering");
        updatedDto.setParentDepartmentId(nonExistentParentId);

        ParentDepartmentNotFoundException thrownException = assertThrows(
                ParentDepartmentNotFoundException.class,
                () -> departmentService.updateDepartment(departmentId, updatedDto)
        );

        // Then: Verify that the ParentNotFoundException is thrown with the correct message.
        assertEquals("Parent department not found", thrownException.getMessage());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(nonExistentParentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for handling circular reference exception when updating a department.
     */
    @Test
    public void testUpdateDepartment_CircularReferences() {
        // Given: An existing department and an update request that causes a circular reference.
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
                .parentDepartmentId(departmentId)
                .build();

        List<Department> departmentEntities = Arrays.asList(existingDepartment, newParentDepartment);

        DepartmentResponseDto responseDto = DepartmentResponseDto
                .builder()
                .id(departmentId)
                .name(departmentName)
                .parentDepartmentId(newParentId)
                .build();

        // Mocking the Department Repository behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(newParentId)).thenReturn(Optional.of(newParentDepartment));

        // Mocking the JPAQuery behavior
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(departmentEntities);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Attempting to update the department's parent to a circular reference.
        DepartmentRequestDto updatedDto = new DepartmentRequestDto();
        updatedDto.setName("Engineering");
        updatedDto.setParentDepartmentId(newParentId);

        DataIntegrityException thrownException = assertThrows(
                DataIntegrityException.class,
                () -> departmentService.updateDepartment(departmentId, updatedDto)
        );

        // Then: Verify that the CircularReferenceException is thrown with the correct message.
        assertEquals("Circular reference detected: "
                + "Moving department " + existingDepartment.getName() + " under department with ID " + newParentId
                + " would create a circular reference.", thrownException.getMessage());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(newParentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for deleting a department with an existent id and with descendants.
     */
    @Test
    public void testDeleteDepartment_ExistingId_WithDescendants() {
        // Given: An existing department with descendants
        Long departmentId = 1L;
        String departmentPath = "/1/";
        Department department = Department.builder()
                .id(departmentId)
                .name("Software")
                .path(departmentPath)
                .build();

        Department child1 = Department.builder()
                .id(2L)
                .name("Backend")
                .path("/1/2/")
                .parentDepartmentId(departmentId)
                .build();

        Department child2 = Department.builder()
                .id(3L)
                .name("Frontend")
                .path("/1/3/")
                .parentDepartmentId(departmentId)
                .build();

        List<Department> descendants = Arrays.asList(child1, child2);

        // Mocking the repository and JPAQuery behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(descendants);
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);

        // When: Deleting the department
        departmentService.deleteDepartment(departmentId);

        // Then: Verify the department and its descendants are deleted
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).deleteAll(descendants);
        verify(departmentRepository, times(1)).delete(department);
    }

    @Test()
    public void testDeleteDepartment_DepartmentNotFound() {
        // Given: No department found with the given ID
        Long departmentId = 1L;
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Deleting the department
        DepartmentNotFoundException thrownException = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.deleteDepartment(departmentId)
        );

        // Then: Verify that the DepartmentNotFoundException is thrown with the correct message.
        assertEquals("Department not found with ID: " + departmentId, thrownException.getMessage());
        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).deleteAll(any(List.class));
        verify(departmentRepository, never()).delete(any(Department.class));
    }

    /**
     * Unit test for circular reference detection during department delete.
     */
    @Test
    public void testDeleteDepartment_CircularReference() {
        // Given: A circular reference in the department path
        Long departmentId = 1L;
        String departmentPath = "/1/";
        Department department = Department.builder()
                .id(departmentId)
                .name("Software")
                .path(departmentPath)
                .build();

        Department child = Department.builder()
                .id(2L)
                .name("Backend")
                .path("/1/2/")
                .parentDepartmentId(departmentId)
                .build();

        Department grandChild = Department.builder()
                .id(3L)
                .name("Frontend")
                .path("/1/2/3/")
                .parentDepartmentId(2L)
                .build();

        Department circularDescendant = Department.builder()
                .id(4L)
                .name("Circular")
                .path("/1/2/3/4/2/") // Circular reference
                .parentDepartmentId(3L)
                .build();

        List<Department> descendants = Arrays.asList(child, grandChild, circularDescendant);

        // Mocking the repository and JPAQuery behavior
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(descendants);
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);

        // When: Deleting the department
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteDepartment(departmentId);
        });

        // Then: Verify the exception message
        assertEquals("Circular reference detected in department path: " + circularDescendant.getPath(), exception.getMessage());
    }

    /**
     * Unit test for retrieving all departments when there are no departments.
     */
    @Test
    public void testGetAllDepartments_noDepartments() {
        // Given: No departments in the repository
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());

        // When: Retrieving all departments
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

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
        Department child = Department.builder().id(2L).name("Child").path("/1/2/").parentDepartmentId(1L).build();
        List<Department> departments = Arrays.asList(parent, child);

        DepartmentResponseDto parentDto = DepartmentResponseDto.builder().id(1L).name("Parent").build();
        DepartmentResponseDto childDto = DepartmentResponseDto.builder().id(2L).name("Child").parentDepartmentId(1L).build();

        when(departmentRepository.findAll()).thenReturn(departments);
        when(departmentMapper.toDto(parent)).thenReturn(parentDto);
        when(departmentMapper.toDto(child)).thenReturn(childDto);

        // When: Retrieving all departments
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Then: Verify the result
        assertEquals(2, result.size());
        assertEquals("Parent", result.get(0).getName());
        assertEquals("Child", result.get(1).getName());

        verify(departmentRepository, times(1)).findAll();
        verify(departmentMapper, times(1)).toDto(parent);
        verify(departmentMapper, times(1)).toDto(child);
    }

    /**
     * Unit test for retrieving an existing department by ID.
     */
    @Test
    public void testGetDepartmentById_ExistingDepartment() {
        // Given: An existing department ID
        Long departmentId = 1L;
        Department department = Department.builder().id(departmentId).name("Parent").path("/1/").build();
        DepartmentResponseDto responseDto = DepartmentResponseDto.builder().id(departmentId).name("Parent").build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentMapper.toDto(department)).thenReturn(responseDto);

        // When: Retrieving the department by ID
        DepartmentResponseDto result = departmentService.getDepartmentById(departmentId);

        // Then: Verify the result
        assertEquals(departmentId, result.getId());
        assertEquals("Parent", result.getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentMapper, times(1)).toDto(department);
    }

    /**
     * Unit test for retrieving a non-existent department by ID.
     */
    @Test
    public void testGetDepartmentById_NonExistingDepartment() {
        // Given: A non-existing department ID
        Long departmentId = -1L;

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Retrieving the department by ID
        DepartmentNotFoundException thrown = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.getDepartmentById(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Department not found with ID: " + departmentId, thrown.getMessage());

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

        DepartmentResponseDto responseDto1 = DepartmentResponseDto.builder().id(1L).name(departmentName).build();
        DepartmentResponseDto responseDto2 = DepartmentResponseDto.builder().id(2L).name(departmentName).build();

        List<Department> departments = Arrays.asList(department1, department2);


        when(departmentRepository.findByNameContaining(departmentName)).thenReturn(departments);

        when(departmentMapper.toDto(department1)).thenReturn(responseDto1);
        when(departmentMapper.toDto(department2)).thenReturn(responseDto2);

        // When: Searching departments by name
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName(departmentName);

        // Then: Verify the result
        assertEquals(2, result.size());
        assertEquals(departmentName, result.get(0).getName());
        assertEquals(departmentName, result.get(1).getName());

        verify(departmentRepository, times(1)).findByNameContaining(departmentName);
        verify(departmentMapper, times(1)).toDto(department1);
        verify(departmentMapper, times(1)).toDto(department2);
    }

    /**
     * Unit test for searching departments by a non-existing name.
     */
    @Test
    public void testSearchDepartmentsByName_NonExistingName() {
        // Given: No departments with matching name
        String departmentName = "NonExistingDepartment_" + UUID.randomUUID(); // Generate a unique non-existing name

        when(departmentRepository.findByNameContaining(departmentName)).thenReturn(Collections.emptyList());

        // When: Searching departments by name
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName(departmentName);

        // Then: Verify the result is an empty list
        assertTrue(result.isEmpty());
        verify(departmentRepository, times(1)).findByNameContaining(departmentName);
    }

    /**
     * Unit test for {@link DepartmentService#getSubDepartments(Long)} when parent department exists.
     * Verifies that sub-departments are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetSubDepartments_existingParent() {
        // Given: Existing parent department
        Long parentId = 1L;
        Department parent = Department.builder().id(parentId).name("Parent").build();
        Department child1 = Department.builder().id(2L).name("Child1").parentDepartmentId(parentId).build();
        Department child2 = Department.builder().id(3L).name("Child2").parentDepartmentId(parentId).build();
        List<Department> subDepartments = Arrays.asList(child1, child2);

        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(departmentRepository.findByParentDepartmentId(parentId)).thenReturn(subDepartments);

        DepartmentResponseDto child1Dto = DepartmentResponseDto.builder().id(2L).name("Child1").build();
        DepartmentResponseDto child2Dto = DepartmentResponseDto.builder().id(3L).name("Child2").build();
        when(departmentMapper.toDto(child1)).thenReturn(child1Dto);
        when(departmentMapper.toDto(child2)).thenReturn(child2Dto);

        // When: Getting sub-departments
        List<DepartmentResponseDto> result = departmentService.getSubDepartments(parentId);

        // Then: Verify the result
        assertEquals(2, result.size());
        assertEquals("Child1", result.get(0).getName());
        assertEquals("Child2", result.get(1).getName());

        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentRepository, times(1)).findByParentDepartmentId(parentId);
        verify(departmentMapper, times(1)).toDto(child1);
        verify(departmentMapper, times(1)).toDto(child2);
    }

    /**
     * Unit test for {@link DepartmentService#getSubDepartments(Long)} when parent department does not exist.
     * Verifies that {@link ParentDepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetSubDepartments_nonExistingParent() {
        // Given: Non-existing parent department ID
        Long parentId = -1L;
        when(departmentRepository.findById(parentId)).thenReturn(Optional.empty());

        // When: Getting sub-departments
        ParentDepartmentNotFoundException thrown = assertThrows(
                ParentDepartmentNotFoundException.class,
                () -> departmentService.getSubDepartments(parentId)
        );

        // Then: Verify the exception
        assertEquals("Parent department not found with id: " + parentId, thrown.getMessage());

        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentRepository, never()).findByParentDepartmentId(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Unit test for {@link DepartmentService#getDescendants(Long)} when department exists.
     * Verifies that descendants are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetDescendants_existingDepartment() {
        // Given: Existing department
        Long departmentId = 1L;
        Department department = Department.builder().id(departmentId).name("Department").path("/1/").build();
        Department child1 = Department.builder().id(2L).name("Child1").path("/1/2/").build();
        Department child2 = Department.builder().id(3L).name("Child2").path("/1/3/").build();
        List<Department> descendants = Arrays.asList(department, child1, child2);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.findByPathStartingWith("/1/")).thenReturn(descendants);

        DepartmentResponseDto departmentDto = DepartmentResponseDto.builder().id(departmentId).name("Department").build();
        DepartmentResponseDto child1Dto = DepartmentResponseDto.builder().id(2L).name("Child1").build();
        DepartmentResponseDto child2Dto = DepartmentResponseDto.builder().id(3L).name("Child2").build();
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);
        when(departmentMapper.toDto(child1)).thenReturn(child1Dto);
        when(departmentMapper.toDto(child2)).thenReturn(child2Dto);

        // When: Getting descendants
        List<DepartmentResponseDto> result = departmentService.getDescendants(departmentId);

        // Then: Verify the result
        assertEquals(2, result.size());
        assertEquals("Child1", result.get(0).getName());
        assertEquals("Child2", result.get(1).getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findByPathStartingWith("/1/");
        verify(departmentMapper, times(1)).toDto(child1);
        verify(departmentMapper, times(1)).toDto(child2);
    }

    /**
     * Unit test for {@link DepartmentService#getDescendants(Long)} when department does not exist.
     * Verifies that {@link DepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetDescendants_nonExistingDepartment() {
        // Given: Non-existing department ID
        Long departmentId = -1L;
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Getting descendants
        DepartmentNotFoundException thrown = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.getDescendants(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Department not found with ID: " + departmentId, thrown.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).findByPathStartingWith(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Unit test for {@link DepartmentService#getAncestors(Long)} when department exists.
     * Verifies that ancestors are correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetAncestors_existingDepartment() {
        // Given: Existing department
        Long parentId = 1L;
        Long departmentId = 2L;
        Department parent = Department.builder().id(parentId).name("Department").path("/1/").build();
        Department department = Department.builder().id(departmentId).name("Department").path("/1/2/").parentDepartmentId(parentId).build();
        List<Department> ancestors = Arrays.asList(parent, department);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.findAllById(Arrays.asList(1L, 2L))).thenReturn(ancestors);

        DepartmentResponseDto parentDto = DepartmentResponseDto.builder().id(departmentId).name("Parent").build();
        when(departmentMapper.toDto(parent)).thenReturn(parentDto);

        // When: Getting ancestors
        List<DepartmentResponseDto> result = departmentService.getAncestors(departmentId);

        // Then: Verify the result
        assertEquals(1, result.size());
        assertEquals("Parent", result.get(0).getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findAllById(Arrays.asList(1L, 2L));
        verify(departmentMapper, times(1)).toDto(parent);
    }

    /**
     * Unit test for {@link DepartmentService#getAncestors(Long)} when department does not exist.
     * Verifies that {@link DepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetAncestors_nonExistingDepartment() {
        // Given: Non-existing department ID
        Long departmentId = -1L;
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Getting ancestors
        DepartmentNotFoundException thrown = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.getAncestors(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Department not found with ID: " + departmentId, thrown.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).findAllById(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Unit test for {@link DepartmentService#getParentDepartment(Long)} when parent department exists.
     * Verifies that parent department is correctly fetched and mapped to DTOs.
     */
    @Test
    public void testGetParentDepartment_existingParent() {
        // Given: Existing department with parent
        Long departmentId = 2L;
        Long parentId = 1L;
        Department department = Department.builder().id(departmentId).name("Department").parentDepartmentId(parentId).build();
        Department parent = Department.builder().id(parentId).name("Parent").build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parent));

        DepartmentResponseDto parentDto = DepartmentResponseDto.builder().id(parentId).name("Parent").build();
        when(departmentMapper.toDto(parent)).thenReturn(parentDto);

        // When: Getting parent department
        DepartmentResponseDto result = departmentService.getParentDepartment(departmentId);

        // Then: Verify the result
        assertEquals(parentId, result.getId());
        assertEquals("Parent", result.getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentMapper, times(1)).toDto(parent);
    }

    /**
     * Unit test for {@link DepartmentService#getParentDepartment(Long)} when department does not exist.
     * Verifies that {@link DepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetParentDepartment_nonExistingDepartment() {
        // Given: Non-existing department ID
        Long departmentId = -1L;
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When: Getting parent department
        DepartmentNotFoundException thrown = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.getParentDepartment(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Department not found with ID: " + departmentId, thrown.getMessage());

        verify(departmentRepository, times(1)).findById(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Unit test for {@link DepartmentService#getParentDepartment(Long)} when department parent does not exist.
     * Verifies that {@link ParentDepartmentNotFoundException} is thrown.
     */
    @Test
    public void testGetParentDepartment_noParent() {
        // Given: Department with no parent
        Long departmentId = 1L;
        Department department = Department.builder().id(departmentId).name("Department").build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        // When: Getting parent department
        ParentDepartmentNotFoundException thrown = assertThrows(
                ParentDepartmentNotFoundException.class,
                () -> departmentService.getParentDepartment(departmentId)
        );

        // Then: Verify the exception
        assertEquals("Department with id: " + departmentId + " has no parent.", thrown.getMessage());

        verify(departmentRepository, times(1)).findById(any());
        verify(departmentMapper, never()).toDto(any());
    }
}
