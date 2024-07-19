package tn.engn.hierarchicalentityapi.service;

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
import tn.engn.hierarchicalentityapi.model.QDepartment;
import tn.engn.hierarchicalentityapi.model.QHierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
//@ExtendWith(MockitoExtension.class)
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

        // Mock the value of entityManager directly
        departmentService.setEntityManager(entityManager);

        // Mock EntityManager methods that don't return a value
//        doNothing().when(entityManager).flush();
//        doAnswer(invocation -> {
//            Object entity = invocation.getArgument(0);
//            return entity; // Simply return the entity passed to refresh
//        }).when(entityManager).refresh(any());

        // Mock ID
        id = 1L;
    }

    /**
     * Helper method to create a department with a parent and to stub the related repository methods.
     *
     * @param name   the department name
     * @param parent the department parent
     * @return the created department
     */
    private Department createEntity(String name, Department parent) {
        Department department = Department.builder()
                .id(id++) // Set an appropriate ID
                .name(name)
                .parentId(parent != null ? parent.getId() : null)
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
     * @param name       the department name
     * @param parent     the department parent
     * @param level      the department level in the hierarchy
     * @param leftIndex  the corresponding left index in the nested set model
     * @param rightIndex the corresponding right index in the nested set model
     * @param root       the corresponding root department in the hierarchy
     * @return
     */
    private Department createEntity(String name, Department parent, Integer level, Integer leftIndex, Integer rightIndex, Department root) {
        Department department = Department.builder()
                .id(id++) // Set an appropriate ID
                .name(name)
                .parentId(parent != null ? parent.getId() : null)
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
     * Helper method to convert Department to HierarchyResponseDto and to stub the related mapper method.
     *
     * @param department the department to convert
     * @return
     */
    private HierarchyResponseDto toResponseDtoWithStub(Department department) {
        HierarchyResponseDto responseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .parentEntityId(department.getParentId())
                .subEntities(new ArrayList<>())
                .build();

        // Mocking mapper behavior
        when(departmentMapper.toDto(department)).thenReturn(responseDto);
        when(departmentMapper.toDto(eq(department), anyBoolean())).thenReturn(responseDto);

        return responseDto;
    }

    /**
     * Helper method to convert Department to HierarchyResponseDto and to stub the related mapper method.
     *
     * @param department the department to convert
     * @return
     */
    private HierarchyResponseDto toResponseDto(Department department) {
        HierarchyResponseDto responseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .parentEntityId(department.getParentId())
                .subEntities(new ArrayList<>())
                .build();

        return responseDto;
    }

    // Example method to convert Department to HierarchyResponseDto With Subs

    /**
     * Helper method to convert Department to HierarchyResponseDto including sub-departments list,
     * and to stub the related mapper method.
     *
     * @param department     the department to convert
     * @param subDepartments the corresponding sub-departments list
     * @return
     */
    private HierarchyResponseDto toResponseDtoWithSubs(Department department, List<Department> subDepartments) {
        HierarchyResponseDto responseDto = HierarchyResponseDto.builder()
                .id(department.getId())
                .name(department.getName())
                .parentEntityId(department.getParentId())
                .subEntities(
                        subDepartments.stream()
                                .map(d -> toResponseDtoWithStub(d))
                                .collect(Collectors.toList())
                )
                .build();

        // Mocking mapper behavior
        when(departmentMapper.toDto(department)).thenReturn(responseDto);
        return responseDto;
    }

    // Example method to convert Department to HierarchyRequestDto

    /**
     * Helper method to convert Department to HierarchyRequestDto and to stub the related mapper method.
     *
     * @param department the department to convert
     * @return
     */
    private HierarchyRequestDto toRequestDto(Department department) {
        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .id(department.getId())
                .name(department.getName())
                .parentEntityId(department.getParentId())
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
        when(mockQuery.from(any(QHierarchyBaseEntity.class))).thenReturn(mockQuery);
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
    private JPAQuery<HierarchyBaseEntity<?>> mockSelectFromQuery(Department department) {
        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mock(JPAQuery.class);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetchOne()).thenReturn((HierarchyBaseEntity) department);

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
                department -> (HierarchyBaseEntity<?>) department
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
     * Helper method to verify the behaviors of success update operation.
     */

    private void verifyUpdateSuccessBehavior(Department department, UpdateQueryObjects updateQueryObjects, Boolean toRoot) {
        verify(departmentRepository, times(1)).save(department);

        verify(jpaQueryFactory, times(7))
                .update(eq(updateQueryObjects.qHierarchyBaseEntity));

        verify(updateQueryObjects.mockUpdateClause, times(7))
                .where(any(Predicate.class));

        verify(updateQueryObjects.mockUpdateClause, times(7))
                .execute();

        verify(updateQueryObjects.mockUpdateClause, times(3))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(3))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.rightIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.level),
                        any(NumberExpression.class));
        if (toRoot) {
            verify(updateQueryObjects.mockUpdateClause, times(1))
                    .set(eq(updateQueryObjects.qHierarchyBaseEntity.rootId),
                            any(NumberPath.class));

            verify(updateQueryObjects.mockUpdateClause, times(1))
                    .set(eq(updateQueryObjects.qHierarchyBaseEntity.parentId),
                            (Long) eq(null));
        } else {
            verify(updateQueryObjects.mockUpdateClause, times(1))
                    .set(eq(updateQueryObjects.qHierarchyBaseEntity.rootId),
                            anyLong());

            verify(updateQueryObjects.mockUpdateClause, times(1))
                    .set(eq(updateQueryObjects.qHierarchyBaseEntity.parentId),
                            anyLong());
        }
    }

    /**
     * Helper inner class to use in the behavior verification of an update operation.
     */
    class UpdateQueryObjects {
        public final JPAUpdateClause mockUpdateClause;
        public final QHierarchyBaseEntity qHierarchyBaseEntity;

        UpdateQueryObjects(JPAUpdateClause mockUpdateClause, QHierarchyBaseEntity qHierarchyBaseEntity) {
            this.mockUpdateClause = mockUpdateClause;
            this.qHierarchyBaseEntity = qHierarchyBaseEntity;
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
        QHierarchyBaseEntity qHierarchyBaseEntity = QHierarchyBaseEntity.hierarchyBaseEntity;
        when(jpaQueryFactory.update(eq(qHierarchyBaseEntity))).thenReturn(mockUpdateClause);

        return new UpdateQueryObjects(mockUpdateClause, qHierarchyBaseEntity);
    }

    /**
     * Test creating a root department without a parent.
     */
    @Test
    public void testCreateRootDepartment_Success() {
        // Given
        String name = "Engineering";

        Department department = createEntity(name, null);

        HierarchyRequestDto requestDto = toRequestDto(department);

        HierarchyResponseDto responseDto = toResponseDtoWithStub(department);

        mockSelectNumber();

        // When
        HierarchyResponseDto result = departmentService.createEntity(requestDto);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(name, result.getName());
        assertNull(result.getParentEntityId());
        assertNotNull(result.getSubEntities());
        assertEquals(0, result.getSubEntities().size());

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
        Department parentDepartment = createEntity(parentName, null, 0, parentLeftIndex,
                parentRightIndex, null);

        String childName = "Software Development";
        Department department = createEntity(childName, parentDepartment);

        HierarchyRequestDto requestDto = toRequestDto(department);

        HierarchyResponseDto responseDto = toResponseDtoWithStub(department);

        mockSelectFromQuery(parentDepartment);

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(1L);

        when(departmentMapper.toEntity(any())).thenReturn(department);

        // When creating child department
        HierarchyResponseDto result = departmentService.createEntity(requestDto);

        // Then
        assertNotNull(result.getId());
        assertEquals(childName, result.getName());
        assertEquals(parentDepartment.getId(), result.getParentEntityId());
        assertNotNull(result.getSubEntities());
        assertEquals(0, result.getSubEntities().size());

        verify(departmentRepository, times(1)).save(department);
        verify(departmentMapper, times(1)).toEntity(any());
        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());

        Department savedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(savedDepartment);
        assertEquals(1, savedDepartment.getLevel());
        assertEquals(2, savedDepartment.getLeftIndex());
        assertEquals(3, savedDepartment.getRightIndex());

        verify(jpaQueryFactory, times(2))
                .update(eq(updateQueryObjects.qHierarchyBaseEntity));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.rightIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .where(eq(updateQueryObjects.qHierarchyBaseEntity.leftIndex.goe(parentRightIndex)));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .where(eq(updateQueryObjects.qHierarchyBaseEntity.rightIndex.goe(parentRightIndex)));

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .execute();
    }

    /**
     * Test creating a real subtree of departments and assert indexes.
     */
    @Test
    public void testCreateRealSubtree_Success() {
        // Given
        Department root = createEntity("Engineering", null, 0, 1, 6, null);
        Department subDept1 = createEntity("Software Development", root, 1, 2, 3, root);
        Department subDept2 = createEntity("Quality Assurance", root, 1, 4, 5, root);
        Department subSubDept = createEntity("Automation Team", subDept1);

        toResponseDtoWithStub(subSubDept);

        // Mocking query factory behavior
        mockSelectNumber();

        mockSelectFromQuery(subDept1);

        mockUpdateQuery(1L);

        // When
        HierarchyResponseDto result = departmentService.createEntity(toRequestDto(subSubDept));

        // Then
        assertNotNull(result.getId());
        assertEquals(subSubDept.getName(), result.getName());
        assertEquals(subSubDept.getParentId(), result.getParentEntityId());
        assertNotNull(result.getSubEntities());
        assertEquals(0, result.getSubEntities().size());
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

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(invalidName)
                .build();

        // When
        Exception exception = assertThrows(ValidationException.class, () -> {
            departmentService.createEntity(requestDto);
        });

        // Then
        assertEquals("Entity name cannot be null or empty.", exception.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    /**
     * Test creating a department with a non-existing parent.
     */
    @Test
    public void testCreateDepartment_ParentNotFound() {
        // Given
        long nonExistingParentId = -1L; // Non-existing parent ID

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name("Software Development")
                .parentEntityId(nonExistingParentId)
                .build();

        mockSelectFromQuery(null);

        // When
        Exception exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.createEntity(requestDto);
        });

        // Then
        assertEquals("Parent entity not found with ID: " + nonExistingParentId, exception.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    /**
     * Test creating a department with a name equal to the maximum length.
     */
    @Test
    public void testCreateDepartment_NameMaxLength() {
        // Given: A HierarchyRequestDto with a name equal to the maximum length.
        String maxLengthName = "A".repeat(maxNameLength); // Long name exceeding max length

        Department department = createEntity(maxLengthName, null);

        HierarchyRequestDto requestDto = toRequestDto(department);

        HierarchyResponseDto responseDto = toResponseDtoWithStub(department);

        mockSelectNumber();

        // When
        HierarchyResponseDto result = departmentService.createEntity(requestDto);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(maxLengthName, result.getName());
        assertNull(result.getParentEntityId());
        assertNotNull(result.getSubEntities());
        assertEquals(0, result.getSubEntities().size());

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
        // Given: A HierarchyRequestDto with a name exceeding the maximum length.
        String tooLongName = "A".repeat(maxNameLength + 1);

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .name(tooLongName)
                .build();

        // When
        Exception exception = assertThrows(ValidationException.class, () -> {
            departmentService.createEntity(requestDto);
        });

        // Then
        assertEquals("Entity name cannot be longer than " + maxNameLength + " characters.", exception.getMessage());

        verify(departmentRepository, never()).save(any());
    }

    /**
     * Test case for updating a department's name only.
     * Verifies that the department's name is updated correctly.
     */
    @Test
    public void testUpdateDepartment_NameOnly() {
        // Given
        Department existingDepartment = createEntity("Engineering", null);
        HierarchyRequestDto updateDto = HierarchyRequestDto.builder()
                .name("Software Development")
                .build();

        toResponseDtoWithStub(existingDepartment);

        // When
        HierarchyResponseDto result = departmentService.updateEntity(existingDepartment.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());

        Department updatedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(updatedDepartment);
        assertEquals(existingDepartment.getId(), updatedDepartment.getId());
        assertEquals(updateDto.getName(), updatedDepartment.getName());
        assertNull(updatedDepartment.getParentId());

        assertNotNull(result.getSubEntities());
        assertEquals(0, result.getSubEntities().size());

        verify(departmentRepository, times(1)).save(existingDepartment);
    }

    /**
     * Test case for updating a department's name only within a real subtree.
     * Verifies that the department's name is updated correctly.
     */
    @Test
    public void testUpdateDepartment_NameOnly_InSubtree() {
        // Given
        Department root = createEntity("Engineering", null, 0, 1, 6, null);
        Department subDept1 = createEntity("Software Development", root, 1, 2, 5, root);
        Department subSubDept = createEntity("Quality Assurance", subDept1, 2, 3, 4, root);

        toResponseDtoWithStub(subSubDept);

        HierarchyRequestDto updateDto = HierarchyRequestDto.builder()
                .name("Automation Team")
                .parentEntityId(subDept1.getId())
                .build();

        toResponseDtoWithStub(subSubDept);

        // When
        HierarchyResponseDto result = departmentService.updateEntity(subSubDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(departmentArgumentCaptor.capture());

        Department updatedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(updatedDepartment);
        assertEquals(subSubDept.getId(), updatedDepartment.getId());
        assertEquals(updateDto.getName(), updatedDepartment.getName());
        assertEquals(subSubDept.getParentId(), updatedDepartment.getParentId());

        assertNotNull(result.getSubEntities());
        assertEquals(0, result.getSubEntities().size());

        verify(departmentRepository, times(1)).save(subSubDept);
    }

    /**
     * Test case for updating a department's parent.
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    public void testUpdateDepartment_ChangeParent() {
        // Given
        Department parentDept1 = createEntity("Engineering", null, 0, 1, 4, null);
        Department childDept = createEntity("Software Development", parentDept1, 1, 2, 3, parentDept1);
        Department parentDept2 = createEntity("Marketing", null, 0, 5, 6, null);

        HierarchyRequestDto updateDto = toRequestDto(childDept);
        updateDto.setParentEntityId(parentDept2.getId());

        toResponseDtoWithStub(childDept);

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(childDept.getId());

        // When
        HierarchyResponseDto result = departmentService.updateEntity(childDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(childDept);

        assertEquals(childDept.getId(), result.getId());
        assertEquals(childDept.getName(), result.getName());
        assertNotNull(result.getSubEntities());
        assertEquals(0, result.getSubEntities().size());

        verifyUpdateSuccessBehavior(childDept, updateQueryObjects, false);
    }

    /**
     * Test case for updating a department's parent within a real subtree.
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree() {
        // Given
        Department rootDept = createEntity("Engineering", null, 0, 1, 8, null);
        Department parentDept = createEntity("Software Development", rootDept, 1, 2, 7, rootDept);
        Department childDept = createEntity("Backend Team", parentDept, 2, 3, 6, rootDept);
        Department newParentDept = createEntity("Frontend Team", parentDept, 2, 4, 5, rootDept);

        HierarchyRequestDto updateDto = toRequestDto(childDept);
        updateDto.setParentEntityId(rootDept.getId());

        toResponseDtoWithStub(childDept);

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(childDept.getId());

        // When
        HierarchyResponseDto result = departmentService.updateEntity(childDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(childDept);

        assertEquals(childDept.getId(), result.getId());
        assertEquals(childDept.getName(), result.getName());
        assertNotNull(result.getSubEntities());
        assertEquals(0, result.getSubEntities().size());

        verifyUpdateSuccessBehavior(childDept, updateQueryObjects, false);
    }

    /**
     * Test case for updating a department's parent within a real subtree (root to child of another root).
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    public void testUpdateDepartment_ChangeParent_InSubtree_RootToChildOfRoot() {
        // Given
        Department rootDept = createEntity("Engineering", null, 0, 1, 6, null);
        Department parentDept = createEntity("Software Development", rootDept, 1, 2, 5, rootDept);
        Department childDept = createEntity("Backend Team", parentDept, 2, 3, 4, rootDept);
        Department superRootDept = createEntity("HR Department", null, 0, 7, 8, null);


        HierarchyRequestDto updateDto = toRequestDto(rootDept);
        updateDto.setParentEntityId(superRootDept.getId());

        toResponseDtoWithSubs(rootDept, List.of(parentDept));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(rootDept.getId());

        // When
        HierarchyResponseDto result = departmentService.updateEntity(rootDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(rootDept);

        assertEquals(rootDept.getId(), result.getId());
        assertEquals(rootDept.getName(), result.getName());
        assertNotNull(result.getSubEntities());
        assertEquals(1, result.getSubEntities().size());

        verifyUpdateSuccessBehavior(rootDept, updateQueryObjects, false);
    }

    /**
     * Test case for updating a department's parent.
     * Verifies that the department's parent is updated and indexes are adjusted correctly.
     */
    @Test
    public void testUpdateDepartment_ParentDepartment_SubDepartments() {
        // Given
        Department rootDept = createEntity("Engineering", null, 0, 1, 2, null);

        Department parentDept = createEntity("Software Development", null, 0, 3, 8, null);
        Department child1Dept = createEntity("Backend Team", parentDept, 1, 4, 5, parentDept);
        Department newChildDept = createEntity("Frontend Team", parentDept, 0, 6, 7, parentDept);

        HierarchyRequestDto updateDto = toRequestDto(parentDept);
        updateDto.setName("Updated Software Development");
        updateDto.setParentEntityId(rootDept.getId());

        toResponseDtoWithSubs(parentDept, List.of(child1Dept, newChildDept));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(updateDto.getId());

        // When
        HierarchyResponseDto result = departmentService.updateEntity(parentDept.getId(), updateDto);

        // Then
        assertEquals(parentDept.getId(), result.getId());
        assertNotNull(result.getSubEntities());
        assertEquals(2, result.getSubEntities().size());

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
        Department department = createEntity("Engineering", null, 0, 1, 4, null);
        HierarchyRequestDto updateDto = toRequestDto(department);
        updateDto.setName(null); // Invalid name to trigger validation exception

        // When / Then
        assertThrows(ValidationException.class, () -> {
            departmentService.updateEntity(department.getId(), updateDto);
        });

        verify(departmentMapper, never()).toDto(department);
        verify(departmentRepository, never()).save(department);
        verify(jpaQueryFactory, never()).update(any(QDepartment.class));
    }

    /**
     * Test case for updating a department with a non-existent parent.
     * Verifies that a ParentEntityNotFoundException is thrown.
     */
    @Test
    public void testUpdateDepartment_ParentNotFoundException() {
        // Given
        Department department = createEntity("Engineering", null, 0, 1, 4, null);
        HierarchyRequestDto updateDto = toRequestDto(department);
        updateDto.setParentEntityId(-1L); // Non-existent parent ID to trigger exception

        // When / Then
        assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.updateEntity(department.getId(), updateDto);
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
        Department parentDept = createEntity("Engineering", null, 0, 1, 4, null);
        Department childDept = createEntity("Software Development", parentDept, 1, 2, 3, parentDept);

        HierarchyRequestDto updateDto = toRequestDto(childDept);
        updateDto.setParentEntityId(childDept.getId()); // Circular reference to trigger exception

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateEntity(childDept.getId(), updateDto);
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
        Department parentDept = createEntity("Engineering", null, 0, 1, 4, null);
        Department childDept = createEntity("Software Development", parentDept, 1, 2, 3, parentDept);

        HierarchyRequestDto updateDto = toRequestDto(parentDept);
        updateDto.setParentEntityId(childDept.getId()); // Circular reference to trigger exception

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateEntity(parentDept.getId(), updateDto);
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
        Department rootDept = createEntity("Root", null, 0, 1, 8, null);
        Department childDept1 = createEntity("Child 1", rootDept, 1, 2, 3, rootDept);
        Department childDept2 = createEntity("Child 2", rootDept, 1, 4, 7, rootDept);
        Department subChildDept = createEntity("SubChild 1", childDept2, 2, 5, 6, rootDept);

        HierarchyRequestDto updateDto = toRequestDto(rootDept);
        updateDto.setParentEntityId(subChildDept.getId()); // Creating circular dependency

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateEntity(rootDept.getId(), updateDto);
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
        Department rootDept = createEntity("Root", null, 0, 1, 10, null);
        Department childDept = createEntity("Child", rootDept, 1, 2, 3, rootDept);

        HierarchyRequestDto updateDto = toRequestDto(childDept);
        updateDto.setParentEntityId(null); // Change parent to root

        toResponseDtoWithStub(childDept);

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(childDept.getId());

        // When
        HierarchyResponseDto result = departmentService.updateEntity(childDept.getId(), updateDto);

        // Then
        verify(departmentMapper, times(1)).toDto(childDept);

        assertEquals(childDept.getId(), result.getId());
        assertEquals(childDept.getName(), result.getName());
        assertNotNull(result.getSubEntities());
        assertEquals(0, result.getSubEntities().size());

        verifyUpdateSuccessBehavior(childDept, updateQueryObjects, true);
    }

    /**
     * Test case for retrieving all departments when the repository is populated.
     * Verifies that the method returns a list of all departments.
     */
    @Test
    public void testGetAllDepartments_withDepartments() {
        // Given
        Department dept1 = createEntity("Engineering", null, 0, 1, 4, null);
        Department dept2 = createEntity("HR", null, 0, 2, 3, null);

        when(departmentMapper.toDtoList(anyList(), anyBoolean())).thenReturn(Arrays.asList(toResponseDto(dept1), toResponseDto(dept2)));
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(Arrays.asList(dept1, dept2));

        // When
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

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
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(Collections.emptyList());

        // When
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .orderBy(any(OrderSpecifier.class));

        verify(mockQuery, times(1))
                .fetch();
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
                .leftIndex(1).rightIndex(10).level(0).rootId(1L).build();
        Department dept2 = Department.builder().id(2L).name("Department 2").parentId(dept1.getId())
                .leftIndex(2).rightIndex(7).level(1).rootId(1L).build();
        Department dept3 = Department.builder().id(3L).name("Department 3").parentId(dept2.getId())
                .leftIndex(3).rightIndex(4).level(2).rootId(1L).build();
        Department dept4 = Department.builder().id(4L).name("Department 4").parentId(dept2.getId())
                .leftIndex(5).rightIndex(6).level(2).rootId(1L).build();
        Department dept5 = Department.builder().id(5L).name("Department 5").parentId(dept1.getId())
                .leftIndex(8).rightIndex(9).level(1).rootId(1L).build();

//        dept1.setSubEntities(Arrays.asList(dept2, dept5));
//        dept2.setSubEntities(Arrays.asList(dept3, dept4));
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
     * Test case for retrieving sub-departments when the parent department exists.
     * Verifies that the method returns a list of direct sub-departments.
     */
    @Test
    public void testGetSubDepartments_ExistingParent() {
        // Given
        Department parent = createEntity("Engineering", null, 0, 1, 6, null);
        Department subDept1 = createEntity("Software", parent, 1, 2, 3, parent);
        Department subDept2 = createEntity("HR", parent, 1, 4, 5, parent);

        when(departmentMapper.toDtoList(anyList(), anyBoolean())).thenReturn(Arrays.asList(toResponseDto(subDept1), toResponseDto(subDept2)));

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(Arrays.asList(subDept1, subDept2));

        // When
        List<HierarchyResponseDto> result = departmentService.getSubEntities(parent.getId());

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();
    }

    /**
     * Test case for retrieving sub-departments when the parent department does not exist.
     * Verifies that ParentEntityNotFoundException is thrown.
     */
    @Test
    public void testGetSubDepartments_NonExistingParent() {
        // Given
        Long nonExistingParentId = -1L;

        ParentEntityNotFoundException exception = assertThrows(
                ParentEntityNotFoundException.class,
                () -> departmentService.getSubEntities(nonExistingParentId)
        );

        assertEquals("Parent entity not found with ID: " + nonExistingParentId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingParentId);
        verify(jpaQueryFactory, never()).selectFrom(any(QHierarchyBaseEntity.class));
    }

    /**
     * Test case for retrieving an existing department by ID.
     * Verifies that the method returns the correct department DTO.
     */
    @Test
    public void testGetDepartmentById_ExistingDepartment() {
        // Given
        Department dept = createEntity("Engineering", null, 0, 1, 4, null);
        toResponseDtoWithStub(dept);

        // When
        HierarchyResponseDto result = departmentService.getEntityById(dept.getId());

        // Then
        assertNotNull(result);
        assertEquals(dept.getId(), result.getId());

        verify(departmentRepository, times(1)).findById(dept.getId());
        verify(departmentMapper, times(1)).toDto(any(), anyBoolean());
    }

    /**
     * Test case for retrieving a non-existing department by ID.
     * Verifies that EntityNotFoundException is thrown.
     */
    @Test
    public void testGetDepartmentById_NonExistingDepartment() {
        // Given a non-existing department ID
        Long nonExistingId = -1L;

        // When / Then
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> departmentService.getEntityById(nonExistingId)
        );

        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(departmentMapper, never()).toDto(any());
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
     * Test case for searching departments by an existing name.
     * Verifies that the method returns departments matching the search name.
     */
    @Test
    public void testSearchDepartmentsByName_ExistingName() {
        // Given
        String name = "Department";

        Department engineering = createEntity("Engineering Department", null);
        Department hrDepartment = createEntity("HR Department", null);
        Department itDepartment = createEntity("IT Department", engineering);

        when(departmentMapper.toDtoList(anyList(), anyBoolean())).thenReturn(Arrays.asList(toResponseDto(engineering), toResponseDto(itDepartment), toResponseDto(hrDepartment)));


        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(List.of(engineering, hrDepartment, itDepartment));

        // When
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(name);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, times(1)).toDtoList(anyList(), anyBoolean());
    }

    /**
     * Test case for searching departments by a non-existing name.
     * Verifies that the method returns an empty list.
     */
    @Test
    public void testSearchDepartmentsByName_NonExistingName() {
        // Given
        String nonExistingDepartmentName = "NonExistingDepartment-" + UUID.randomUUID().toString();

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(Collections.emptyList());

        // When searching for a non-existing department
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(nonExistingDepartmentName);

        // Then the result should be an empty list
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, never()).toDto(any());
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
     * Test case for retrieving the parent department of an existing department ID.
     * Verifies that the method returns the correct parent department.
     */
    @Test
    public void testGetParentDepartment_ExistingId() {
        // Given
        Department parent = createEntity("Engineering", null, 0, 1, 4, null);
        Department child = createEntity("Software", parent, 1, 2, 3, parent);

        toResponseDtoWithStub(parent);

        // When
        HierarchyResponseDto result = departmentService.getParentEntity(child.getId());

        // Then
        assertNotNull(result);
        assertEquals(parent.getId(), result.getId());

        verify(departmentRepository, times(1)).findById(child.getId());
        verify(departmentRepository, times(1)).findById(parent.getId());
        verify(departmentMapper, times(1)).toDto(any(), anyBoolean());
    }

    /**
     * Test case for retrieving the parent department of a non-existent department ID.
     * Verifies that an EntityNotFoundException is thrown.
     */
    @Test
    public void testGetParentDepartment_NonExistingId() {
        // Call the method to test with a non-existing ID
        Long nonExistingId = -1L;

        // Verify that the method throws EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getParentEntity(nonExistingId);
        });

        // Verify the exception message
        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
    }

    /**
     * Test case for retrieving the parent department of a department without a parent.
     * Verifies that the method throws ParentEntityNotFoundException.
     */
    @Test
    public void testGetParentDepartment_NoParent() {
        // Given
        Department itDepartment = createEntity("IT Department", null);

        // When attempting to retrieve the parent department
        Throwable throwable = catchThrowable(() -> departmentService.getParentEntity(itDepartment.getId()));

        // Then ParentEntityNotFoundException should be thrown
        assertThat(throwable).isInstanceOf(ParentEntityNotFoundException.class)
                .hasMessageContaining("Entity with ID " + itDepartment.getId() + " has no parent entity");

        verify(departmentRepository, times(1)).findById(itDepartment.getId());
    }

    /**
     * Test case for retrieving all root departments when there are root departments.
     * Verifies that the method returns a list of root departments.
     */
    @Test
    public void testGetAllRootDepartments_WithRootDepartments() {
        // Given
        Department root1 = createEntity("Engineering", null, 0, 1, 2, null);
        Department root2 = createEntity("HR", null, 0, 3, 4, null);

        toResponseDtoWithStub(root1);
        toResponseDtoWithStub(root2);

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(List.of(root1, root2));

        // When
        List<HierarchyResponseDto> result = departmentService.getAllRootEntities();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

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
        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(Collections.emptyList());

        // When
        List<HierarchyResponseDto> result = departmentService.getAllRootEntities();

        // Then the result should be an empty list
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

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
        Department root = createEntity("Engineering", null, 0, 1, 6, null);
        Department child = createEntity("Software", root, 1, 2, 5, root);
        Department subChild = createEntity("Backend", child, 2, 3, 4, root);

        when(departmentMapper.toDtoList(anyList(), anyBoolean())).thenReturn(Arrays.asList(toResponseDto(child), toResponseDto(subChild)));

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(List.of(child, subChild));

        // When
        List<HierarchyResponseDto> result = departmentService.getDescendants(root.getId());

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(departmentRepository, times(1)).findById(root.getId());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, times(1)).toDtoList(anyList(), anyBoolean());
    }

    /**
     * Test case for retrieving descendants of a non-existent department ID.
     * Verifies that a EntityNotFoundException is thrown.
     */
    @Test
    public void testGetDescendants_NonExistingId() {
        // Call the method to test with a non-existent ID
        Long nonExistingId = -1L;

        // Verify that the method throws EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getDescendants(nonExistingId);
        });

        // Verify the exception message
        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(jpaQueryFactory, never()).selectFrom(any());
        verify(departmentMapper, never()).toDto(any());
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
     * Test case for retrieving all ancestors of an existing department ID.
     * Verifies that the method returns all ancestors of the given department.
     */
    @Test
    public void testGetAncestors_ExistingId() {
        // Given
        Department root = createEntity("Engineering", null, 0, 1, 6, null);
        Department child = createEntity("Software", root, 1, 2, 5, root);
        Department subChild = createEntity("Backend", child, 2, 3, 4, root);

        when(departmentMapper.toDtoList(anyList(), anyBoolean())).thenReturn(Arrays.asList(toResponseDto(root), toResponseDto(child)));


        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(List.of(child, root));

        // When
        List<HierarchyResponseDto> result = departmentService.getAncestors(subChild.getId());

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(departmentRepository, times(1)).findById(subChild.getId());

        verify(jpaQueryFactory, times(1))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .fetch();

        verify(departmentMapper, times(1)).toDtoList(anyList(), anyBoolean());
    }

    /**
     * Test case for retrieving ancestors of a non-existent department ID.
     * Verifies that a EntityNotFoundException is thrown.
     */
    @Test
    public void testGetAncestors_NonExistingId() {
        // Call the method to test with a non-existent ID
        Long nonExistingId = -1L;

        // Verify that the method throws EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getAncestors(nonExistingId);
        });

        // Verify the exception message
        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(jpaQueryFactory, never()).selectFrom(any());
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

        // Given: Mock JPAQueryFactory behavior for retrieving ancestors
        JPAQuery<HierarchyBaseEntity<?>> jpaQuery = mockSelectFromQueryList(ancestors);

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
        verify(jpaQueryFactory, times(1)).selectFrom(any(QHierarchyBaseEntity.class));
        verify(jpaQuery, times(1)).where(any(Predicate.class));
        verify(jpaQuery, times(1)).fetch();
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
     * Test case for deleting an existing department.
     * Verifies that the method deletes the department and its subtree correctly.
     */
    @Test
    public void testDeleteDepartment_ExistingId() {
        // Given
        Department dept = createEntity("Engineering", null, 0, 1, 4, null);

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(List.of(dept));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(dept.getId());

        doNothing().when(departmentRepository).delete(dept);

        // When
        departmentService.deleteEntity(dept.getId());

        // Then
        verify(departmentRepository, times(1)).findById(dept.getId());
        verify(departmentRepository, times(1)).delete(dept);

        verify(jpaQueryFactory, times(2))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .orderBy(any(OrderSpecifier.class));

        verify(mockQuery, times(2))
                .fetch();

        verify(jpaQueryFactory, times(1))
                .update(eq(updateQueryObjects.qHierarchyBaseEntity));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .where(any(Predicate.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .execute();

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.rightIndex),
                        any(NumberExpression.class));

    }

    /**
     * Test case for deleting a department with sub-departments (subtree deletion).
     * Verifies that the method deletes the department and all its descendants.
     */
    @Test
    public void testDeleteDepartment_WithSubDepartments() {
        // Given
        Department root = createEntity("Engineering Department", null, 0, 1, 6, null);
        Department child = createEntity("Software Department", root, 1, 2, 5, root);
        Department subChild = createEntity("Software Department", child, 2, 3, 4, root);

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(List.of(child, subChild));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(child.getId());

        doNothing().when(departmentRepository).delete(child);

        // When
        departmentService.deleteEntity(child.getId());

        // Then
        verify(departmentRepository, times(1)).findById(child.getId());
        verify(departmentRepository, times(1)).findById(root.getId());
        verify(departmentRepository, times(1)).delete(child);
        verify(departmentRepository, times(1)).delete(subChild);

        verify(jpaQueryFactory, times(2))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .orderBy(any(OrderSpecifier.class));

        verify(mockQuery, times(2))
                .fetch();

        verify(jpaQueryFactory, times(2))
                .update(eq(updateQueryObjects.qHierarchyBaseEntity));

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .where(any(Predicate.class));

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .execute();

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(2))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.rightIndex),
                        any(NumberExpression.class));
    }

    /**
     * Test case for attempting to delete a non-existent department.
     * Verifies that the method throws EntityNotFoundException.
     */
    @Test
    public void testDeleteDepartment_NonExistentId() {
        // Given
        Long nonExistingId = -1L;
        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> departmentService.deleteEntity(nonExistingId),
                "Expected EntityNotFoundException for non-existent ID");

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(departmentRepository, never()).delete(any());
    }

    /**
     * Tests deletion of a department with a non-existent parent.
     */
    @Test
    public void testDeleteDepartmentWithNonExistentParent() {
        // Given
        Department dept = createEntity("Engineering", null, 0, 1, 4, null);
        Long nonExistentParentId = -1L;
        dept.setParentId(nonExistentParentId);

        when(departmentRepository.findById(-1L)).thenReturn(Optional.empty());
        doNothing().when(departmentRepository).delete(dept);

        // When & Then
        assertThrows(ParentEntityNotFoundException.class, () -> departmentService.deleteEntity(dept.getId()),
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
        Department root = createEntity("Engineering Department", null, 0, 1, 6, null);
        Department child = createEntity("Software Department", root, 1, 2, 5, root);
        Department subChild = createEntity("Software Department", child, 2, 3, 4, root);

        root.setParentId(subChild.getId()); // Circular reference

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
                    departmentService.deleteEntity(root.getId());
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
        Department dept1 = createEntity("Engineering", null, 0, 1, 4, null);
        dept1.setParentId(dept1.getId()); // Circular dependency

        // When / Then
        assertThrows(DataIntegrityException.class, () -> {
                    departmentService.deleteEntity(dept1.getId());
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
        Department root = createEntity("Engineering Department", null, 0, 1, 6, null);
        Department child = createEntity("Software Department", root, 1, 2, 5, root);
        Department subChild = createEntity("Software Department", child, 2, 3, 4, root);

        JPAQuery<HierarchyBaseEntity<?>> mockQuery = mockSelectFromQueryList(List.of(subChild));

        UpdateQueryObjects updateQueryObjects = mockUpdateQuery(subChild.getId());

        doNothing().when(departmentRepository).delete(subChild);

        // When
        departmentService.deleteEntity(subChild.getId());

        // Then
        verify(departmentRepository, times(1)).findById(subChild.getId());
        verify(departmentRepository, times(1)).findById(child.getId());
        verify(departmentRepository, times(1)).delete(subChild);

        verify(jpaQueryFactory, times(2))
                .selectFrom(any(QHierarchyBaseEntity.class));

        verify(mockQuery, times(1))
                .where(any(Predicate.class));

        verify(mockQuery, times(1))
                .orderBy(any(OrderSpecifier.class));

        verify(mockQuery, times(2))
                .fetch();

        verify(jpaQueryFactory, times(1))
                .update(eq(updateQueryObjects.qHierarchyBaseEntity));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .where(any(Predicate.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .execute();

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.leftIndex),
                        any(NumberExpression.class));

        verify(updateQueryObjects.mockUpdateClause, times(1))
                .set(eq(updateQueryObjects.qHierarchyBaseEntity.rightIndex),
                        any(NumberExpression.class));
    }


}