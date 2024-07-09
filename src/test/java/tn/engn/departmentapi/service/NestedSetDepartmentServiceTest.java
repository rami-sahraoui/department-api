package tn.engn.departmentapi.service;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
class NestedSetDepartmentServiceTest {
    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private EntityManager entityManager;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @InjectMocks
    private NestedSetDepartmentService departmentService;

    private int maxNameLength = 50; // Mocked value for maxNameLength

    @Captor
    ArgumentCaptor<Department> departmentArgumentCaptor;

    Long id;

    @BeforeEach
    void setUp() {
        // Initialize mocks before each test
        MockitoAnnotations.openMocks(this);

        // Mock the value of maxNameLength directly
        departmentService.setMaxNameLength(maxNameLength);

        // Mock ID
        id = 1L;
    }

    /**
     * Helper method to create a department with a parent and to stub the related repository methods.
     *
     * @param name the department name
     * @param parent the department parent
     * @return the created department
     */
    private Department createDepartment(String name, Department parent) {
        Department department = Department.builder()
                .id(id++) // Set an appropriate ID
                .name(name)
                .parentDepartmentId(parent != null ? parent.getId() : null)
                .build();

        // Mocking repository behavior
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);
        return department;
    }

    // Example method to create a department with a parent

    /**
     * Helper method to create a department with all the necessary properties and to stub the related repository methods.
     *
     * @param name the department name
     * @param parent the department parent
     * @param level the department level in the hierarchy
     * @param leftIndex the corresponding left index in the nested set model
     * @param rightIndex the corresponding right index in the nested set model
     * @param root the corresponding root department in the hierarchy
     * @return
     */
    private Department createDepartment(String name, Department parent, Integer level, Integer leftIndex, Integer rightIndex, Department root) {
        Department department = Department.builder()
                .id(id++) // Set an appropriate ID
                .name(name)
                .parentDepartmentId(parent != null ? parent.getId() : null)
                .level(level)
                .leftIndex(leftIndex)
                .rightIndex(rightIndex)
                .rootId(root != null ? root.getId() : id)
                .build();

        // Mocking repository behavior
        when(departmentRepository.findById(department.getId())).thenReturn(Optional.of(department));
        when(departmentRepository.save(department)).thenReturn(department);
        return department;
    }

    /**
     * Helper method to convert Department to DepartmentResponseDto and to stub the related mapper method.
     *
     * @param department the department to convert
     * @return
     */
    private DepartmentResponseDto toResponseDto(Department department) {
        DepartmentResponseDto responseDto = DepartmentResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .parentDepartmentId(department.getParentDepartmentId())
                .subDepartments(new ArrayList<>())
                .build();

        // Mocking mapper behavior
        when(departmentMapper.toDto(department)).thenReturn(responseDto);

        return responseDto;
    }

    // Example method to convert Department to DepartmentResponseDto With Subs

    /**
     * Helper method to convert Department to DepartmentResponseDto including sub-departments list,
     * and to stub the related mapper method.
     *
     * @param department the department to convert
     * @param subDepartments the corresponding sub-departments list
     * @return
     */
    private DepartmentResponseDto toResponseDtoWithSubs(Department department, List<Department> subDepartments) {
        DepartmentResponseDto responseDto = DepartmentResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .parentDepartmentId(department.getParentDepartmentId())
                .subDepartments(
                        subDepartments.stream()
                                .map(d -> toResponseDto(d))
                                .collect(Collectors.toList())
                )
                .build();

        // Mocking mapper behavior
        when(departmentMapper.toDto(department)).thenReturn(responseDto);

        return responseDto;
    }

    // Example method to convert Department to DepartmentRequestDto

    /**
     * Helper method to convert Department to DepartmentRequestDto and to stub the related mapper method.
     *
     * @param department the department to convert
     * @return
     */
    private DepartmentRequestDto toRequestDto(Department department) {
        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .id(department.getId())
                .name(department.getName())
                .parentDepartmentId(department.getParentDepartmentId())
                .build();

        // Mocking mapper behavior
        when(departmentMapper.toEntity(requestDto)).thenReturn(department);
        return requestDto;
    }

    /**
     * Helper method to mock and stub a select number query.
     *
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<Integer> mockSelectNumber() {
        // Mocking the JPAQuery behavior
        JPAQuery<Integer> mockQuery = mock(JPAQuery.class);
        when(mockQuery.select(any(NumberExpression.class))).thenReturn(mockQuery);
        when(mockQuery.from(any(QDepartment.class))).thenReturn(mockQuery);
        when(mockQuery.fetchOne()).thenReturn(0);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.select(any(NumberExpression.class))).thenReturn(mockQuery);

        return mockQuery;
    }

    /**
     * Helper method to mock and stub a select department from query.
     *
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<Department> mockSelectFromQuery(Department department) {
        // Mocking the JPAQuery behavior
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetchOne()).thenReturn(department);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);
        return mockQuery;
    }

    /**
     * Helper method to mock and stub a select list of departments from query.
     *
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<Department> mockSelectFromQueryList(List<Department> departments) {
        // Mocking the JPAQuery behavior
        JPAQuery<Department> mockQuery = mock(JPAQuery.class);
        when(mockQuery.orderBy(any(OrderSpecifier.class))).thenReturn(mockQuery);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(departments);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QDepartment.class))).thenReturn(mockQuery);

        return mockQuery;

    }

    /**
     * Helper method to verify the behaviors of success update operation.
     */

    private void verifyUpdateSuccessBehavior(Department department, UpdateQueryObjects updateQueryObjects, Boolean toRoot) {
        verify(departmentRepository, times(1)).save(department);

        verify(jpaQueryFactory, times(7))
                .update(eq(updateQueryObjects.qDepartment));

        verify(updateQueryObjects.mockUpdateClause, times(7))
                .where(any(Predicate.class));

        verify(updateQueryObjects.mockUpdateClause, times(7))
                .execute();

        verify(updateQueryObjects.mockUpdateClause, times(3))
                .set(eq(updateQueryObjects.qDepartment.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(3))
                .set(eq(updateQueryObjects.qDepartment.rightIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qDepartment.level),
                        any(NumberExpression.class));
        if (toRoot) {
            verify(updateQueryObjects.mockUpdateClause, times(1))
                    .set(eq(updateQueryObjects.qDepartment.rootId),
                            any(NumberPath.class));

            verify(updateQueryObjects.mockUpdateClause, times(1))
                    .set(eq(updateQueryObjects.qDepartment.parentDepartmentId),
                            (Long) eq(null));
        } else {
            verify(updateQueryObjects.mockUpdateClause, times(1))
                    .set(eq(updateQueryObjects.qDepartment.rootId),
                            anyLong());

            verify(updateQueryObjects.mockUpdateClause, times(1))
                    .set(eq(updateQueryObjects.qDepartment.parentDepartmentId),
                            anyLong());
        }
    }

    /**
     * Helper inner class to use in the behavior verification of an update operation.
     */
    class UpdateQueryObjects {
        public final JPAUpdateClause mockUpdateClause;
        public final QDepartment qDepartment;

        UpdateQueryObjects(JPAUpdateClause mockUpdateClause, QDepartment qDepartment) {
            this.mockUpdateClause = mockUpdateClause;
            this.qDepartment = qDepartment;
        }
    }

    /**
     * Helper method to mock and stub an update department query.
     *
     * @param toReturn the value to return
     * @return the mock query object to verify the behavior.
     */
    UpdateQueryObjects mockUpdateQuery(Long toReturn) {
        // Mocking the JPAUpdateClause behavior
        JPAUpdateClause mockUpdateClause = mock(JPAUpdateClause.class);
        when(mockUpdateClause.set((NumberPath<Integer>) any(NumberPath.class), any(NumberExpression.class))).thenReturn(mockUpdateClause);
        when(mockUpdateClause.set((NumberPath<Long>) any(NumberPath.class), any(NumberExpression.class))).thenReturn(mockUpdateClause);
        when(mockUpdateClause.set(any(Path.class), any(Object.class))).thenReturn(mockUpdateClause);
        when(mockUpdateClause.set(any(Path.class), (Object) eq(null))).thenReturn(mockUpdateClause);
        when(mockUpdateClause.where(any(Predicate.class))).thenReturn(mockUpdateClause);
        when(mockUpdateClause.execute()).thenReturn(toReturn);

        // Setting up the JPAQueryFactory to return the mock update clause
        QDepartment qDepartment = QDepartment.department;
        when(jpaQueryFactory.update(eq(qDepartment))).thenReturn(mockUpdateClause);

        return new UpdateQueryObjects(mockUpdateClause, qDepartment);
    }

    /**
     * Test creating a root department without a parent.
     */
    @Test
    public void testCreateRootDepartment_Success() {
        // Given
        String name = "Engineering";

        Department department = createDepartment(name, null);

        DepartmentRequestDto requestDto = toRequestDto(department);

        DepartmentResponseDto responseDto = toResponseDto(department);

        mockSelectNumber();

        // When
        DepartmentResponseDto result = departmentService.createDepartment(requestDto);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(name, result.getName());
        assertNull(result.getParentDepartmentId());
        assertNotNull(result.getSubDepartments());
        assertEquals(0, result.getSubDepartments().size());

        verify(departmentRepository, times(3)).save(department);
        verify(departmentMapper, times(1)).toEntity(any());
        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());

        Department savedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(savedDepartment);
        assertEquals(0, savedDepartment.getLevel());
        assertEquals(1, savedDepartment.getLeftIndex());
        assertEquals(2, savedDepartment.getRightIndex());
        assertEquals(savedDepartment.getId(), savedDepartment.getRootId());
    }

    /**
     * Test creating a child department with a parent and assert indexes.
     */
    @Test
    public void testCreateChildDepartment_Success() {
        // Given
        int parentLeftIndex = 1;
        int parentRightIndex = 2;
        String parentName = "Engineering";
        Department parentDepartment = createDepartment(parentName, null, 0, parentLeftIndex,
                parentRightIndex, null);

        String childName = "Software Development";
        Department department = createDepartment(childName, parentDepartment);

        DepartmentRequestDto requestDto = toRequestDto(department);

        DepartmentResponseDto responseDto = toResponseDto(department);

        mockSelectFromQuery(parentDepartment);

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(1L);

        // When creating child department
        DepartmentResponseDto result = departmentService.createDepartment(requestDto);

        // Then
        assertNotNull(result.getId());
        assertEquals(childName, result.getName());
        assertEquals(parentDepartment.getId(), result.getParentDepartmentId());
        assertNotNull(result.getSubDepartments());
        assertEquals(0, result.getSubDepartments().size());

        verify(departmentRepository, times(1)).save(department);
        verify(departmentMapper, times(1)).toEntity(any());
        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());

        Department savedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(savedDepartment);
        assertEquals(1, savedDepartment.getLevel());
        assertEquals(2, savedDepartment.getLeftIndex());
        assertEquals(3, savedDepartment.getRightIndex());

        verify(jpaQueryFactory, times(2))
                .update(eq(updateQueryObjects.qDepartment));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qDepartment.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qDepartment.rightIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .where(eq(updateQueryObjects.qDepartment.leftIndex.goe(parentRightIndex)));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .where(eq(updateQueryObjects.qDepartment.rightIndex.goe(parentRightIndex)));

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .execute();
    }

    /**
     * Test creating a real subtree of departments and assert indexes.
     */
    @Test
    public void testCreateRealSubtree_Success() {
        // Given
        Department root = createDepartment("Engineering", null, 0, 1, 6, null);
        Department subDept1 = createDepartment("Software Development", root, 1, 2, 3, root);
        Department subDept2 = createDepartment("Quality Assurance", root, 1, 4, 5, root);
        Department subSubDept = createDepartment("Automation Team", subDept1);

        toResponseDto(subSubDept);

        // Mocking query factory behavior
        mockSelectNumber();

        mockSelectFromQuery(subDept1);

        mockUpdateQuery(1L);

        // When
        DepartmentResponseDto result = departmentService.createDepartment(toRequestDto(subSubDept));

        // Then
        assertNotNull(result.getId());
        assertEquals(subSubDept.getName(), result.getName());
        assertEquals(subSubDept.getParentDepartmentId(), result.getParentDepartmentId());
        assertNotNull(result.getSubDepartments());
        assertEquals(0, result.getSubDepartments().size());
        verify(departmentRepository, times(1)).save(subSubDept);
        verify(departmentMapper, times(1)).toEntity(any());
        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());

        Department savedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(savedDepartment);
        assertEquals(2, savedDepartment.getLevel());
        assertEquals(3, savedDepartment.getLeftIndex());
        assertEquals(4, savedDepartment.getRightIndex());

    }

    /**
     * Test creating a department with an invalid name.
     */
    @Test
    public void testCreateDepartment_InvalidName() {
        // Given
        String invalidName = ""; // Invalid name

        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name(invalidName)
                .build();

        // When
        Exception exception = assertThrows(ValidationException.class, () -> {
            departmentService.createDepartment(requestDto);
        });

        // Then
        assertEquals("Department name cannot be null or empty.", exception.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    /**
     * Test creating a department with a non-existing parent.
     */
    @Test
    public void testCreateDepartment_ParentNotFound() {
        // Given
        long nonExistingParentId = -1L; // Non-existing parent ID

        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name("Software Development")
                .parentDepartmentId(nonExistingParentId)
                .build();

        mockSelectFromQuery(null);

        // When
        Exception exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.createDepartment(requestDto);
        });

        // Then
        assertEquals("Parent department not found with ID: " + nonExistingParentId, exception.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    /**
     * Test creating a department with a name equal to the maximum length.
     */
    @Test
    public void testCreateDepartment_NameMaxLength() {
        // Given: A DepartmentRequestDto with a name equal to the maximum length.
        String maxLengthName = "A".repeat(maxNameLength); // Long name exceeding max length

        Department department = createDepartment(maxLengthName, null);

        DepartmentRequestDto requestDto = toRequestDto(department);

        DepartmentResponseDto responseDto = toResponseDto(department);

        mockSelectNumber();

        // When
        DepartmentResponseDto result = departmentService.createDepartment(requestDto);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(maxLengthName, result.getName());
        assertNull(result.getParentDepartmentId());
        assertNotNull(result.getSubDepartments());
        assertEquals(0, result.getSubDepartments().size());

        verify(departmentRepository, times(3)).save(department);
        verify(departmentMapper, times(1)).toEntity(any());
        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());

        Department savedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(savedDepartment);
        assertEquals(0, savedDepartment.getLevel());
        assertEquals(1, savedDepartment.getLeftIndex());
        assertEquals(2, savedDepartment.getRightIndex());
        assertEquals(savedDepartment.getId(), savedDepartment.getRootId());
    }

    /**
     * Test creating a department with a name exceeding the maximum length.
     */
    @Test
    public void testCreateDepartment_NameTooLong() {
        // Given: A DepartmentRequestDto with a name exceeding the maximum length.
        String tooLongName = "A".repeat(maxNameLength + 1);

        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .name(tooLongName)
                .build();

        // When
        Exception exception = assertThrows(ValidationException.class, () -> {
            departmentService.createDepartment(requestDto);
        });

        // Then
        assertEquals("Department name cannot be longer than " + maxNameLength + " characters.", exception.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    /**
     * Test case for updating a department's name only.
     * Verifies that the department's name is updated correctly.
     */
    @Test
    public void testUpdateDepartment_NameOnly() {
        // Given
        Department existingDepartment = createDepartment("Engineering", null);
        DepartmentRequestDto updateDto = DepartmentRequestDto.builder()
                .name("Software Development")
                .build();

        toResponseDto(existingDepartment);

        // When
        DepartmentResponseDto result = departmentService.updateDepartment(existingDepartment.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());

        Department updatedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(updatedDepartment);
        assertEquals(existingDepartment.getId(), updatedDepartment.getId());
        assertEquals(updateDto.getName(), updatedDepartment.getName());
        assertNull(updatedDepartment.getParentDepartmentId());

        assertNotNull(result.getSubDepartments());
        assertEquals(0, result.getSubDepartments().size());

        verify(departmentRepository, times(1)).save(existingDepartment);
    }

    /**
     * Test case for updating a department's name only within a real subtree.
     * Verifies that the department's name is updated correctly.
     */
    @Test
    public void testUpdateDepartment_NameOnly_InSubtree() {
        // Given
        Department root = createDepartment("Engineering", null, 0, 1, 6, null);
        Department subDept1 = createDepartment("Software Development", root, 1, 2, 5, root);
        Department subSubDept = createDepartment("Quality Assurance", subDept1, 2, 3, 4, root);

        toResponseDto(subSubDept);

        DepartmentRequestDto updateDto = DepartmentRequestDto.builder()
                .name("Automation Team")
                .parentDepartmentId(subDept1.getId())
                .build();

        toResponseDto(subSubDept);

        // When
        DepartmentResponseDto result = departmentService.updateDepartment(subSubDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());

        Department updatedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(updatedDepartment);
        assertEquals(subSubDept.getId(), updatedDepartment.getId());
        assertEquals(updateDto.getName(), updatedDepartment.getName());
        assertEquals(subSubDept.getParentDepartmentId(), updatedDepartment.getParentDepartmentId());

        assertNotNull(result.getSubDepartments());
        assertEquals(0, result.getSubDepartments().size());

        verify(departmentRepository, times(1)).save(subSubDept);
    }

    /**
     * Test case for updating a department's parent.
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    public void testUpdateDepartment_ChangeParent() {
        // Given
        Department parentDept1 = createDepartment("Engineering", null, 0, 1, 4, null);
        Department childDept = createDepartment("Software Development", parentDept1, 1, 2, 3, parentDept1);
        Department parentDept2 = createDepartment("Marketing", null, 0, 5, 6, null);

        DepartmentRequestDto updateDto = toRequestDto(childDept);
        updateDto.setParentDepartmentId(parentDept2.getId());

        toResponseDto(childDept);

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(childDept.getId());

        // When
        DepartmentResponseDto result = departmentService.updateDepartment(childDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(childDept);

        assertEquals(childDept.getId(), result.getId());
        assertEquals(childDept.getName(), result.getName());
        assertNotNull(result.getSubDepartments());
        assertEquals(0, result.getSubDepartments().size());

        verifyUpdateSuccessBehavior(childDept, updateQueryObjects, false);
    }

    /**
     * Test case for updating a department's parent within a real subtree.
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree() {
        // Given
        Department rootDept = createDepartment("Engineering", null, 0, 1, 8, null);
        Department parentDept = createDepartment("Software Development", rootDept, 1, 2, 7, rootDept);
        Department childDept = createDepartment("Backend Team", parentDept, 2, 3, 6, rootDept);
        Department newParentDept = createDepartment("Frontend Team", parentDept, 2, 4, 5, rootDept);

        DepartmentRequestDto updateDto = toRequestDto(childDept);
        updateDto.setParentDepartmentId(rootDept.getId());

        toResponseDto(childDept);

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(childDept.getId());

        // When
        DepartmentResponseDto result = departmentService.updateDepartment(childDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(childDept);

        assertEquals(childDept.getId(), result.getId());
        assertEquals(childDept.getName(), result.getName());
        assertNotNull(result.getSubDepartments());
        assertEquals(0, result.getSubDepartments().size());

        verifyUpdateSuccessBehavior(childDept, updateQueryObjects, false);
    }

    /**
     * Test case for updating a department's parent within a real subtree (root to child of another root).
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree_RootToChildOfRoot() {
        // Given
        Department rootDept = createDepartment("Engineering", null, 0, 1, 6, null);
        Department parentDept = createDepartment("Software Development", rootDept, 1, 2, 5, rootDept);
        Department childDept = createDepartment("Backend Team", parentDept, 2, 3, 4, rootDept);
        Department superRootDept = createDepartment("HR Department", null, 0, 7, 8, null);


        DepartmentRequestDto updateDto = toRequestDto(rootDept);
        updateDto.setParentDepartmentId(superRootDept.getId());

        toResponseDtoWithSubs(rootDept, List.of(parentDept));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(rootDept.getId());

        // When
        DepartmentResponseDto result = departmentService.updateDepartment(rootDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(rootDept);

        assertEquals(rootDept.getId(), result.getId());
        assertEquals(rootDept.getName(), result.getName());
        assertNotNull(result.getSubDepartments());
        assertEquals(1, result.getSubDepartments().size());

        verifyUpdateSuccessBehavior(rootDept, updateQueryObjects, false);
    }

    /**
     * Test case for updating a department's parent.
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    public void testUpdateDepartment_ParentDepartment_SubDepartments() {
        // Given
        Department rootDept = createDepartment("Engineering", null, 0, 1, 2, null);

        Department parentDept = createDepartment("Software Development", null, 0, 3, 8, null);
        Department child1Dept = createDepartment("Backend Team", parentDept, 1, 4, 5, parentDept);
        Department newChildDept = createDepartment("Frontend Team", parentDept, 0, 6, 7, parentDept);

        DepartmentRequestDto updateDto = toRequestDto(parentDept);
        updateDto.setName("Updated Software Development");
        updateDto.setParentDepartmentId(rootDept.getId());

        toResponseDtoWithSubs(parentDept, List.of(child1Dept, newChildDept));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(updateDto.getId());

        // When
        DepartmentResponseDto result = departmentService.updateDepartment(parentDept.getId(), updateDto);

        // Then
        assertEquals(parentDept.getId(), result.getId());
        assertNotNull(result.getSubDepartments());
        assertEquals(2, result.getSubDepartments().size());

        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());
        Department updatedDepartment = departmentArgumentCaptor.getValue();
        assertEquals("Updated Software Development", updatedDepartment.getName());

        verifyUpdateSuccessBehavior(parentDept, updateQueryObjects, false);
    }

    /**
     * Test case for updating a department with an invalid name.
     * Verifies that a ValidationException is thrown.
     */
    @Test
    public void testUpdateDepartment_ValidationException() {
        // Given
        Department department = createDepartment("Engineering", null, 0, 1, 4, null);
        DepartmentRequestDto updateDto = toRequestDto(department);
        updateDto.setName(null); // Invalid name to trigger validation exception

        // When / Then
        assertThrows(ValidationException.class, () -> {
            departmentService.updateDepartment(department.getId(), updateDto);
        });

        verify(departmentMapper, never()).toDto(department);
        verify(departmentRepository, never()).save(department);
        verify(jpaQueryFactory, never()).update(any(QDepartment.class));
    }

    /**
     * Test case for updating a department with a non-existent parent.
     * Verifies that a ParentDepartmentNotFoundException is thrown.
     */
    @Test
    public void testUpdateDepartment_ParentNotFoundException() {
        // Given
        Department department = createDepartment("Engineering", null, 0, 1, 4, null);
        DepartmentRequestDto updateDto = toRequestDto(department);
        updateDto.setParentDepartmentId(-1L); // Non-existent parent ID to trigger exception

        // When / Then
        assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.updateDepartment(department.getId(), updateDto);
        });

        verify(departmentMapper, never()).toDto(department);
        verify(departmentRepository, never()).save(department);
        verify(jpaQueryFactory, never()).update(any(QDepartment.class));
    }

    /**
     * Test case for updating a department that results in a self-circular reference.
     * Verifies that a DataIntegrityException is thrown.
     */
    @Test
    public void testUpdateDepartment_SelfCircularReferences() {
        // Given
        Department parentDept = createDepartment("Engineering", null, 0, 1, 4, null);
        Department childDept = createDepartment("Software Development", parentDept, 1, 2, 3, parentDept);

        DepartmentRequestDto updateDto = toRequestDto(childDept);
        updateDto.setParentDepartmentId(childDept.getId()); // Circular reference to trigger exception

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateDepartment(childDept.getId(), updateDto);
        });

        verify(departmentMapper, never()).toDto(childDept);
        verify(departmentRepository, never()).save(childDept);
        verify(jpaQueryFactory, never()).update(any(QDepartment.class));
    }

    /**
     * Test case for updating a department that results in a circular reference.
     * Verifies that a DataIntegrityException is thrown.
     */
    @Test
    public void testUpdateDepartment_CircularReferences() {
        // Given
        Department parentDept = createDepartment("Engineering", null, 0, 1, 4, null);
        Department childDept = createDepartment("Software Development", parentDept, 1, 2, 3, parentDept);

        DepartmentRequestDto updateDto = toRequestDto(parentDept);
        updateDto.setParentDepartmentId(childDept.getId()); // Circular reference to trigger exception

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateDepartment(parentDept.getId(), updateDto);
        });

        verify(departmentMapper, never()).toDto(parentDept);
        verify(departmentRepository, never()).save(parentDept);
        verify(jpaQueryFactory, never()).update(any(QDepartment.class));
    }

    /**
     * Test for updateDepartment method to ensure it throws DataIntegrityException
     * when trying to update a department to be its own descendant's parent, resulting in a circular dependency.
     */
    @Test
    public void testUpdateDepartment_WithCircularDependency_ShouldThrowDataIntegrityException() {
        // Given
        Department rootDept = createDepartment("Root", null, 0, 1, 8, null);
        Department childDept1 = createDepartment("Child 1", rootDept, 1, 2, 3, rootDept);
        Department childDept2 = createDepartment("Child 2", rootDept, 1, 4, 7, rootDept);
        Department subChildDept = createDepartment("SubChild 1", childDept2, 2, 5, 6, rootDept);

        DepartmentRequestDto updateDto = toRequestDto(rootDept);
        updateDto.setParentDepartmentId(subChildDept.getId()); // Creating circular dependency

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateDepartment(rootDept.getId(), updateDto);
        });

        verify(departmentMapper, never()).toDto(childDept1);
        verify(departmentRepository, never()).save(childDept1);
        verify(jpaQueryFactory, never()).update(any(QDepartment.class));
    }

    /**
     * Test case for updating a child department to be a root.
     * Verifies that the department's parent and indexes are adjusted correctly.
     */
    @Test
    public void testUpdateDepartment_ChangeChildToRoot() {
        // Given
        Department rootDept = createDepartment("Root", null, 0, 1, 10, null);
        Department childDept = createDepartment("Child", rootDept, 1, 2, 3, rootDept);

        DepartmentRequestDto updateDto = toRequestDto(childDept);
        updateDto.setParentDepartmentId(null); // Change parent to root

        toResponseDto(childDept);

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(childDept.getId());

        // When
        DepartmentResponseDto result = departmentService.updateDepartment(childDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(childDept);

        assertEquals(childDept.getId(), result.getId());
        assertEquals(childDept.getName(), result.getName());
        assertNotNull(result.getSubDepartments());
        assertEquals(0, result.getSubDepartments().size());

        verifyUpdateSuccessBehavior(childDept, updateQueryObjects, true);
    }

    /**
     * Test case for retrieving all departments when the repository is populated.
     * Verifies that the method returns a list of all departments.
     */
    @Test
    public void testGetAllDepartments_withDepartments() {
        // Given
        Department dept1 = createDepartment("Engineering", null, 0, 1, 4, null);
        Department dept2 = createDepartment("HR", null, 0, 2, 3, null);

        toResponseDto(dept1);
        toResponseDto(dept2);
        JPAQuery<Department> mockQuery = mockSelectFromQueryList(Arrays.asList(dept1, dept2));

        // When
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .orderBy(any(OrderSpecifier.class));

        verify(mockQuery, times(1))
                .fetch();
    }

    /**
     * Test case for retrieving all departments when the repository is empty.
     * Verifies that the method returns an empty list.
     */
    @Test
    public void testGetAllDepartments_noDepartments() {
        // Given
        JPAQuery<Department> mockQuery = mockSelectFromQueryList(Collections.emptyList());

        // When
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .orderBy(any(OrderSpecifier.class));

        verify(mockQuery, times(1))
                .fetch();
    }

    /**
     * Test case for retrieving sub-departments when the parent department exists.
     * Verifies that the method returns a list of direct sub-departments.
     */
    @Test
    public void testGetSubDepartments_ExistingParent() {
        // Given
        Department parent = createDepartment("Engineering", null, 0, 1, 6, null);
        Department subDept1 = createDepartment("Software", parent, 1, 2, 3, parent);
        Department subDept2 = createDepartment("HR", parent, 1, 4, 5, parent);

        toResponseDto(subDept1);
        toResponseDto(subDept2);

        JPAQuery<Department> mockQuery = mockSelectFromQueryList(Arrays.asList(subDept1, subDept2));

        // When
        List<DepartmentResponseDto> result = departmentService.getSubDepartments(parent.getId());

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();
    }

    /**
     * Test case for retrieving sub-departments when the parent department does not exist.
     * Verifies that ParentDepartmentNotFoundException is thrown.
     */
    @Test
    public void testGetSubDepartments_NonExistingParent() {
        // Given
        Long nonExistingParentId = -1L;

        ParentDepartmentNotFoundException exception = assertThrows(
                ParentDepartmentNotFoundException.class,
                () -> departmentService.getSubDepartments(nonExistingParentId)
        );

        assertEquals("Parent department not found with ID: " + nonExistingParentId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingParentId);
        verify(jpaQueryFactory, never()).selectFrom(any(QDepartment.class));
    }

    /**
     * Test case for retrieving an existing department by ID.
     * Verifies that the method returns the correct department DTO.
     */
    @Test
    public void testGetDepartmentById_ExistingDepartment() {
        // Given
        Department dept = createDepartment("Engineering", null, 0, 1, 4, null);
        toResponseDto(dept);

        // When
        DepartmentResponseDto result = departmentService.getDepartmentById(dept.getId());

        // Then
        assertNotNull(result);
        assertEquals(dept.getId(), result.getId());

        verify(departmentRepository, times(1)).findById(dept.getId());
        verify(departmentMapper, times(1)).toDto(any());
    }

    /**
     * Test case for retrieving a non-existing department by ID.
     * Verifies that DepartmentNotFoundException is thrown.
     */
    @Test
    public void testGetDepartmentById_NonExistingDepartment() {
        // Given a non-existing department ID
        Long nonExistingId = -1L;

        // When / Then
        DepartmentNotFoundException exception = assertThrows(
                DepartmentNotFoundException.class,
                () -> departmentService.getDepartmentById(nonExistingId)
        );

        assertEquals("Department not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Test case for searching departments by an existing name.
     * Verifies that the method returns departments matching the search name.
     */
    @Test
    public void testSearchDepartmentsByName_ExistingName() {
        // Given
        String name = "Department";

        Department engineering = createDepartment("Engineering Department", null);
        Department hrDepartment = createDepartment("HR Department", null);
        Department itDepartment = createDepartment("IT Department", engineering);

        toResponseDto(engineering);
        toResponseDto(hrDepartment);
        toResponseDto(itDepartment);

        JPAQuery<Department> mockQuery = mockSelectFromQueryList(List.of(engineering, hrDepartment, itDepartment));

        // When
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName(name);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, times(3)).toDto(any());
    }

    /**
     * Test case for searching departments by a non-existing name.
     * Verifies that the method returns an empty list.
     */
    @Test
    public void testSearchDepartmentsByName_NonExistingName() {
        // Given
        String nonExistingDepartmentName = "NonExistingDepartment-" + UUID.randomUUID().toString();

        JPAQuery<Department> mockQuery = mockSelectFromQueryList(Collections.emptyList());

        // When searching for a non-existing department
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName(nonExistingDepartmentName);

        // Then the result should be an empty list
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Test case for retrieving the parent department of an existing department ID.
     * Verifies that the method returns the correct parent department.
     */
    @Test
    public void testGetParentDepartment_ExistingId() {
        // Given
        Department parent = createDepartment("Engineering", null, 0, 1, 4, null);
        Department child = createDepartment("Software", parent, 1, 2, 3, parent);

        toResponseDto(parent);

        // When
        DepartmentResponseDto result = departmentService.getParentDepartment(child.getId());

        // Then
        assertNotNull(result);
        assertEquals(parent.getId(), result.getId());

        verify(departmentRepository, times(1)).findById(child.getId());
        verify(departmentRepository, times(1)).findById(parent.getId());
        verify(departmentMapper, times(1)).toDto(parent);
    }

    /**
     * Test case for retrieving the parent department of a non-existent department ID.
     * Verifies that an DepartmentNotFoundException is thrown.
     */
    @Test
    public void testGetParentDepartment_NonExistingId() {
        // Call the method to test with a non-existing ID
        Long nonExistingId = -1L;

        // Verify that the method throws EntityNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getParentDepartment(nonExistingId);
        });

        // Verify the exception message
        assertEquals("Department not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
    }

    /**
     * Test case for retrieving the parent department of a department without a parent.
     * Verifies that the method throws ParentDepartmentNotFoundException.
     */
    @Test
    public void testGetParentDepartment_NoParent() {
        // Given
        Department itDepartment = createDepartment("IT Department", null);

        // When attempting to retrieve the parent department
        Throwable throwable = catchThrowable(() -> departmentService.getParentDepartment(itDepartment.getId()));

        // Then ParentDepartmentNotFoundException should be thrown
        assertThat(throwable).isInstanceOf(ParentDepartmentNotFoundException.class)
                .hasMessageContaining("Department with ID " + itDepartment.getId() + " has no parent department");

        verify(departmentRepository, times(1)).findById(itDepartment.getId());
    }

    /**
     * Test case for retrieving all root departments when there are root departments.
     * Verifies that the method returns a list of root departments.
     */
    @Test
    public void testGetAllRootDepartments_WithRootDepartments() {
        // Given
        Department root1 = createDepartment("Engineering", null, 0, 1, 2, null);
        Department root2 = createDepartment("HR", null, 0, 3, 4, null);

        toResponseDto(root1);
        toResponseDto(root2);

        JPAQuery<Department> mockQuery = mockSelectFromQueryList(List.of(root1, root2));

        // When
        List<DepartmentResponseDto> result = departmentService.getAllRootDepartments();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, times(2)).toDto(any());
    }

    /**
     * Test case for retrieving all root departments when there are no root departments.
     * Verifies that the method returns an empty list.
     */
    @Test
    public void testGetAllRootDepartments_NoRootDepartments() {
        // Given
        JPAQuery<Department> mockQuery = mockSelectFromQueryList(Collections.emptyList());

        // When
        List<DepartmentResponseDto> result = departmentService.getAllRootDepartments();

        // Then the result should be an empty list
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Test case for retrieving all descendants of an existing department ID.
     * Verifies that the method returns all descendants of the given department.
     */
    @Test
    public void testGetDescendants_ExistingId() {
        // Given
        Department root = createDepartment("Engineering", null, 0, 1, 6, null);
        Department child = createDepartment("Software", root, 1, 2, 5, root);
        Department subChild = createDepartment("Backend", child, 2, 3, 4, root);

        toResponseDto(child);
        toResponseDto(subChild);

        JPAQuery<Department> mockQuery = mockSelectFromQueryList(List.of(child, subChild));

        // When
        List<DepartmentResponseDto> result = departmentService.getDescendants(root.getId());

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(departmentRepository, times(1)).findById(root.getId());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, times(2)).toDto(any());
    }

    /**
     * Test case for retrieving descendants of a non-existent department ID.
     * Verifies that a DepartmentNotFoundException is thrown.
     */
    @Test
    public void testGetDescendants_NonExistingId() {
        // Call the method to test with a non-existent ID
        Long nonExistingId = -1L;

        // Verify that the method throws DepartmentNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getDescendants(nonExistingId);
        });

        // Verify the exception message
        assertEquals("Department not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(jpaQueryFactory, never()).selectFrom(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Test case for retrieving all ancestors of an existing department ID.
     * Verifies that the method returns all ancestors of the given department.
     */
    @Test
    public void testGetAncestors_ExistingId() {
        // Given
        Department root = createDepartment("Engineering", null, 0, 1, 6, null);
        Department child = createDepartment("Software", root, 1, 2, 5, root);
        Department subChild = createDepartment("Backend", child, 2, 3, 4, root);

        toResponseDto(root);
        toResponseDto(child);

        JPAQuery<Department> mockQuery = mockSelectFromQueryList(List.of(child, root));

        // When
        List<DepartmentResponseDto> result = departmentService.getAncestors(subChild.getId());

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(departmentRepository, times(1)).findById(subChild.getId());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, times(2)).toDto(any());
    }

    /**
     * Test case for retrieving ancestors of a non-existent department ID.
     * Verifies that a DepartmentNotFoundException is thrown.
     */
    @Test
    public void testGetAncestors_NonExistingId() {
        // Call the method to test with a non-existent ID
        Long nonExistingId = -1L;

        // Verify that the method throws DepartmentNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getAncestors(nonExistingId);
        });

        // Verify the exception message
        assertEquals("Department not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(jpaQueryFactory, never()).selectFrom(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Test case for deleting an existing department.
     * Verifies that the method deletes the department and its subtree correctly.
     */
    @Test
    public void testDeleteDepartment_ExistingId() {
        // Given
        Department dept = createDepartment("Engineering", null, 0, 1, 4, null);

        JPAQuery<Department> mockQuery = mockSelectFromQueryList(List.of(dept));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(dept.getId());

        doNothing().when(departmentRepository).delete(dept);

        // When
        departmentService.deleteDepartment(dept.getId());

        // Then
        verify(departmentRepository, times(1)).findById(dept.getId());
        verify(departmentRepository, times(1)).delete(dept);

        verify(jpaQueryFactory, times(2))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .orderBy(any(OrderSpecifier.class));

        verify(mockQuery, times(2))
                .fetch();

        verify(jpaQueryFactory, times(1))
                .update(eq(updateQueryObjects.qDepartment));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .where(any(Predicate.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .execute();

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qDepartment.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qDepartment.rightIndex),
                        any(NumberExpression.class));

    }

    /**
     * Test case for deleting a department with sub-departments (subtree deletion).
     * Verifies that the method deletes the department and all its descendants.
     */
    @Test
    public void testDeleteDepartment_WithSubDepartments() {
        // Given
        Department root = createDepartment("Engineering Department", null, 0, 1, 6, null);
        Department child = createDepartment("Software Department", root, 1, 2, 5, root);
        Department subChild = createDepartment("Software Department", child, 2, 3, 4, root);

        JPAQuery<Department> mockQuery = mockSelectFromQueryList(List.of(child, subChild));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(child.getId());

        doNothing().when(departmentRepository).delete(child);

        // When
        departmentService.deleteDepartment(child.getId());

        // Then
        verify(departmentRepository, times(1)).findById(child.getId());
        verify(departmentRepository, times(1)).findById(root.getId());
        verify(departmentRepository, times(1)).delete(child);
        verify(departmentRepository, times(1)).delete(subChild);

        verify(jpaQueryFactory, times(2))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .orderBy(any(OrderSpecifier.class));

        verify(mockQuery, times(2))
                .fetch();

        verify(jpaQueryFactory, times(2))
                .update(eq(updateQueryObjects.qDepartment));

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .where(any(Predicate.class));

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .execute();

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .set(eq(updateQueryObjects.qDepartment.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .set(eq(updateQueryObjects.qDepartment.rightIndex),
                        any(NumberExpression.class));
    }

    /**
     * Test case for attempting to delete a non-existent department.
     * Verifies that the method throws DepartmentNotFoundException.
     */
    @Test
    public void testDeleteDepartment_NonExistentId() {
        // Given
        Long nonExistingId = -1L;
        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(DepartmentNotFoundException.class, () -> departmentService.deleteDepartment(nonExistingId),
                "Expected DepartmentNotFoundException for non-existent ID");

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(departmentRepository, never()).delete(any());
    }

    /**
     * Tests deletion of a department with a non-existent parent.
     */
    @Test
    public void testDeleteDepartmentWithNonExistentParent() {
        // Given
        Department dept = createDepartment("Engineering", null, 0, 1, 4, null);
        Long nonExistentParentId = -1L;
        dept.setParentDepartmentId(nonExistentParentId);

        when(departmentRepository.findById(-1L)).thenReturn(Optional.empty());
        doNothing().when(departmentRepository).delete(dept);

        // When & Then
        assertThrows(ParentDepartmentNotFoundException.class, () -> departmentService.deleteDepartment(dept.getId()),
                ("Parent department not found with ID: " + nonExistentParentId));

        verify(departmentRepository, times(1)).findById(dept.getId());
        verify(departmentRepository, times(1)).findById(nonExistentParentId);
        verify(departmentRepository, never()).delete(dept);
    }

    /**
     * Tests deletion of a department with circular reference.
     */
    @Test
    public void testDeleteDepartmentWithCircularReference() {
        // Given
        Department root = createDepartment("Engineering Department", null, 0, 1, 6, null);
        Department child = createDepartment("Software Department", root, 1, 2, 5, root);
        Department subChild = createDepartment("Software Department", child, 2, 3, 4, root);

        root.setParentDepartmentId(subChild.getId()); // Circular reference

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
                    departmentService.deleteDepartment(root.getId());
                },
                "Cannot set a department as its own descendant's parent."
        );

        verify(departmentRepository, times(1)).findById(root.getId());
        verify(departmentRepository, times(1)).findById(subChild.getId());
        verify(departmentRepository, never()).delete(any());
    }

    /**
     * Test for delete Department method to ensure it throws DataIntegrityException
     * when trying to delete a department involved in a self circular dependency.
     */
    @Test
    public void testDeleteDepartment_WithSelfCircularDependency() {
        // Given
        Department dept1 = createDepartment("Engineering", null, 0, 1, 4, null);
        dept1.setParentDepartmentId(dept1.getId()); // Circular dependency

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
                    departmentService.deleteDepartment(dept1.getId());
                },
                "Circular references."
        );

        verify(departmentRepository, times(2)).findById(dept1.getId());
        verify(departmentRepository, never()).delete(dept1);
    }

    /**
     * Test case for deleting a leaf department (no sub-departments).
     * Verifies that the method deletes the department and reorders the nested set structure.
     */
    @Test
    public void testDeleteDepartment_LeafNode() {
        // Given
        Department root = createDepartment("Engineering Department", null, 0, 1, 6, null);
        Department child = createDepartment("Software Department", root, 1, 2, 5, root);
        Department subChild = createDepartment("Software Department", child, 2, 3, 4, root);

        JPAQuery<Department> mockQuery = mockSelectFromQueryList(List.of(subChild));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(subChild.getId());

        doNothing().when(departmentRepository).delete(subChild);

        // When
        departmentService.deleteDepartment(subChild.getId());

        // Then
        verify(departmentRepository, times(1)).findById(subChild.getId());
        verify(departmentRepository, times(1)).findById(child.getId());
        verify(departmentRepository, times(1)).delete(subChild);

        verify(jpaQueryFactory, times(2))
                .selectFrom(any(QDepartment.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .orderBy(any(OrderSpecifier.class));

        verify(mockQuery, times(2))
                .fetch();

        verify(jpaQueryFactory, times(1))
                .update(eq(updateQueryObjects.qDepartment));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .where(any(Predicate.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .execute();

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qDepartment.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qDepartment.rightIndex),
                        any(NumberExpression.class));
    }


}