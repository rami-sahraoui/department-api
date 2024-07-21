package tn.engn.hierarchicalentityapi.service;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.stubbing.OngoingStubbing;
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
import tn.engn.hierarchicalentityapi.factory.DepartmentClosureFactory;
import tn.engn.hierarchicalentityapi.mapper.DepartmentMapper;
import tn.engn.hierarchicalentityapi.model.*;
import tn.engn.hierarchicalentityapi.repository.DepartmentClosureRepository;
import tn.engn.hierarchicalentityapi.repository.DepartmentRepository;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ClosureTableDepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentClosureRepository departmentClosureRepository;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private DepartmentClosureFactory departmentClosureFactory;

    @InjectMocks
    private ClosureTableDepartmentService departmentService;

    private int maxNameLength = 50; // Mocked value for maxNameLength

    @Captor
    ArgumentCaptor<Department> departmentArgumentCaptor;

    @Captor
    ArgumentCaptor<List<DepartmentClosure>> departmentClosuresArgumentCaptor;

    @BeforeEach
    void setUp() {
        // Initialize mocks before each test
        MockitoAnnotations.openMocks(this);

        // Mock the value of maxNameLength directly
        departmentService.setMaxNameLength(maxNameLength);
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
     * Helper method to mock and stub a select list of department closures from query.
     *
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<HierarchyBaseEntityClosure<?>> mockSelectFromQueryListClosure(List<DepartmentClosure> departmentClosures) {
        List<HierarchyBaseEntityClosure<?>> entityClosures = departmentClosures.stream().map(
                entity -> (HierarchyBaseEntityClosure<?>) entity
        ).collect(Collectors.toList());

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mock(JPAQuery.class);
        when(mockQuery.orderBy(any(OrderSpecifier.class))).thenReturn(mockQuery);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);
        when(mockQuery.fetch()).thenReturn(entityClosures);

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QHierarchyBaseEntityClosure.class))).thenReturn(mockQuery);

        return mockQuery;

    }

    /**
     * Helper method to mock and stub a select list of department closures from query.
     *
     * @return the mock query object to verify the behavior.
     */
    private JPAQuery<HierarchyBaseEntityClosure<?>> mockSelectFromQueryMultipleLists(List<List<DepartmentClosure>> departmentClosuresLists) {
        List<List<HierarchyBaseEntityClosure<?>>> entityClosuresList = new ArrayList<>();

        for (int i = 0; i < departmentClosuresLists.size(); i++) {
            entityClosuresList.add(departmentClosuresLists.get(i).stream().map(
                    entity -> (HierarchyBaseEntityClosure<?>) entity
            ).collect(Collectors.toList()));
        }

        // Mocking the JPAQuery behavior
        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mock(JPAQuery.class);
        when(mockQuery.orderBy(any(OrderSpecifier.class))).thenReturn(mockQuery);
        when(mockQuery.where(any(Predicate.class))).thenReturn(mockQuery);

        OngoingStubbing<List<HierarchyBaseEntityClosure<?>>> stubbing = when(mockQuery.fetch());
        for (List<HierarchyBaseEntityClosure<?>> entityClosures : entityClosuresList) {
            stubbing = stubbing.thenReturn(entityClosures);
        }

        // Setting up the JPAQueryFactory to return the mock query
        when(jpaQueryFactory.selectFrom(any(QHierarchyBaseEntityClosure.class))).thenReturn(mockQuery);

        return mockQuery;

    }

    /**
     * Helper method to verify the behaviors of a successful fetch operation.
     *
     * This method ensures that the select, where, and fetch operations using the JPAQueryFactory
     * were called the expected number of times and with the correct parameters.
     *
     * @param mockQuery the mock query object used in the fetch operation
     * @param times the number of times the fetch operation is expected to be called
     */
    private void verifySuccessFetch(JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery, int times) {
        // Verify that the selectFrom method on the JPAQueryFactory was called with any instance of QHierarchyBaseEntityClosure
        verify(jpaQueryFactory, times(times)).selectFrom(any(QHierarchyBaseEntityClosure.class));

        // Verify that the where clause was applied with any predicate
        verify(mockQuery, times(times)).where(any(Predicate.class));

        // Verify that the fetch method was called the specified number of times
        verify(mockQuery, times(times)).fetch();
    }


    /**
     * Helper inner class to use in the behavior verification of an update operation.
     */
    class DeleteQueryComponents {
        public final JPADeleteClause mockDeleteClause;
        public final QHierarchyBaseEntityClosure qHierarchyBaseEntityClosure;

        DeleteQueryComponents(JPADeleteClause mockDeleteClause, QHierarchyBaseEntityClosure qHierarchyBaseEntityClosure) {
            this.mockDeleteClause = mockDeleteClause;
            this.qHierarchyBaseEntityClosure = qHierarchyBaseEntityClosure;
        }
    }

    /**
     * Helper method to mock and stub an update department query.
     *
     * @param toReturn the value to return
     * @return the mock query object to verify the behavior.
     */
    DeleteQueryComponents mockDeleteQuery(Long toReturn) {
        // Mocking the JPAUpdateClause behavior
        JPADeleteClause mockDeleteClause = mock(JPADeleteClause.class);
        when(mockDeleteClause.where(any(Predicate.class))).thenReturn(mockDeleteClause);
        when(mockDeleteClause.execute()).thenReturn(toReturn);

        // Setting up the JPAQueryFactory to return the mock delete clause
        QHierarchyBaseEntityClosure qHierarchyBaseEntityClosure = QHierarchyBaseEntityClosure.hierarchyBaseEntityClosure;
        when(jpaQueryFactory.delete(eq(qHierarchyBaseEntityClosure))).thenReturn(mockDeleteClause);

        return new DeleteQueryComponents(mockDeleteClause, qHierarchyBaseEntityClosure);
    }

    /**
     * Helper method to verify the behaviors of a successful delete operation.
     * <p>
     * This method checks that the delete operation using the JPAQueryFactory
     * was called the expected number of times, and that the appropriate
     * predicates and execution steps were performed.
     *
     * @param deleteQueryComponents an object containing the mock delete query components
     * @param times                 the number of times the delete operation is expected to be called
     */
    private void verifySuccessDelete(DeleteQueryComponents deleteQueryComponents, int times) {
        // Verify that the delete method on the JPAQueryFactory was called the specified number of times
        verify(jpaQueryFactory, times(times)).delete(deleteQueryComponents.qHierarchyBaseEntityClosure);

        // Verify that the where clause was applied the specified number of times with any predicate
        verify(deleteQueryComponents.mockDeleteClause, times(times)).where(any(Predicate.class));

        // Verify that the execute method was called the specified number of times
        verify(deleteQueryComponents.mockDeleteClause, times(times)).execute();
    }

    /**
     * Unit test for creating a root department successfully.
     */
    @Test
    public void testCreateRootDepartment_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto for a root department.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Root Department");

        Department createdDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .build();

        HierarchyResponseDto responseDto = HierarchyResponseDto.builder()
                .id(1L)
                .name("Root Department")
                .build();

        DepartmentClosure departmentClosure = DepartmentClosure.builder()
                .ancestorId(1L)
                .descendantId(1L)
                .level(0)
                .build();

        when(departmentRepository.findById(anyLong())).thenReturn(Optional.of(createdDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(createdDepartment);

        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(createdDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        when(departmentClosureFactory.createClosure(1L, 1L, 0)).thenReturn(departmentClosure);

        // When: Creating the root department.
        HierarchyResponseDto result = departmentService.createEntity(requestDto);

        // Then: Verify the department is created successfully.
        assertNotNull(result);
        assertEquals("Root Department", result.getName());
        assertNull(result.getParentEntityId());

        verify(departmentRepository, times(1)).save(departmentArgumentCaptor.capture());
        Department capturedDepartment = departmentArgumentCaptor.getValue();
        assertNotNull(capturedDepartment);
        assertEquals("Root Department", capturedDepartment.getName());

        verify(departmentClosureRepository, times(1)).saveAll(departmentClosuresArgumentCaptor.capture());
        List<DepartmentClosure> capturedClosureEntries = departmentClosuresArgumentCaptor.getValue();
        assertNotNull(capturedClosureEntries);
        assertEquals(1, capturedClosureEntries.size());
        assertEquals(capturedDepartment.getId(), capturedClosureEntries.get(0).getAncestorId());
        assertEquals(capturedDepartment.getId(), capturedClosureEntries.get(0).getDescendantId());
        assertEquals(0, capturedClosureEntries.get(0).getLevel());
    }

    /**
     * Unit test for creating a child department successfully.
     */
    @Test
    public void testCreateChildDepartment_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto for a child department.
        Department parentDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .build();

        Department createdDepartment = Department.builder()
                .id(2L)
                .name("Child Department")
                .parentId(parentDepartment.getId())
                .build();

        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Child Department");
        requestDto.setParentEntityId(parentDepartment.getId());

        HierarchyResponseDto responseDto = HierarchyResponseDto.builder()
                .id(2L)
                .name("Child Department")
                .parentEntityId(parentDepartment.getId())
                .build();

        DepartmentClosure parentDepartmentSelfClosure = DepartmentClosure.builder()
                .ancestorId(parentDepartment.getId())
                .descendantId(parentDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure departmentSelfClosure = DepartmentClosure.builder()
                .ancestorId(createdDepartment.getId())
                .descendantId(createdDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure departmentParentClosure = DepartmentClosure.builder()
                .ancestorId(parentDepartment.getId())
                .descendantId(createdDepartment.getId())
                .level(1)
                .build();

        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(createdDepartment.getId())).thenReturn(Optional.of(createdDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(createdDepartment);

        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(createdDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        when(departmentClosureFactory.createClosure(createdDepartment.getId(), createdDepartment.getId(), 0)).thenReturn(departmentSelfClosure);
        when(departmentClosureFactory.createClosure(parentDepartment.getId(), createdDepartment.getId(), 1)).thenReturn(departmentParentClosure);

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(Arrays.asList(parentDepartmentSelfClosure));
        // When: Creating the child department.
        HierarchyResponseDto result = departmentService.createEntity(requestDto);

        // Then: Verify the department is created successfully.
        assertNotNull(result);
        assertEquals("Child Department", result.getName());
        assertEquals(1L, result.getParentEntityId());

        verify(departmentRepository, times(1)).save(departmentArgumentCaptor.capture());
        Department capturedDepartment = departmentArgumentCaptor.getValue();

        assertNotNull(capturedDepartment);
        assertEquals("Child Department", capturedDepartment.getName());
        assertEquals(parentDepartment.getId(), capturedDepartment.getParentId());

        verify(departmentClosureRepository, times(1)).saveAll(departmentClosuresArgumentCaptor.capture());
        List<DepartmentClosure> capturedClosureEntries = departmentClosuresArgumentCaptor.getValue();

        assertNotNull(capturedClosureEntries);
        assertEquals(2, capturedClosureEntries.size());

        assertEquals(capturedDepartment.getId(), capturedClosureEntries.get(0).getAncestorId());
        assertEquals(capturedDepartment.getId(), capturedClosureEntries.get(0).getDescendantId());
        assertEquals(0, capturedClosureEntries.get(0).getLevel());

        assertEquals(parentDepartment.getId(), capturedClosureEntries.get(1).getAncestorId());
        assertEquals(capturedDepartment.getId(), capturedClosureEntries.get(1).getDescendantId());
        assertEquals(1, capturedClosureEntries.get(1).getLevel());

        verifySuccessFetch(mockQuery, 1);
    }

    /**
     * Unit test for creating a real subtree successfully.
     */
    @Test
    public void testCreateRealSubtree_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto for a real subtree.
        String rootName = "Root";
        Long rootId = 1L;
        HierarchyRequestDto rootRequestDto = HierarchyRequestDto.builder()
                .name(rootName)
                .build();

        Department rootDepartment = Department.builder()
                .id(rootId)
                .name(rootName)
                .build();

        HierarchyResponseDto rootResponseDto = HierarchyResponseDto.builder()
                .id(rootId)
                .name(rootName)
                .build();

        String anotherRootName = "Another Root";
        Long anotherRootId = 2L;
        HierarchyRequestDto anotherRootRequestDto = HierarchyRequestDto.builder()
                .name(anotherRootName)
                .build();

        Department anotherRootDepartment = Department.builder()
                .id(anotherRootId)
                .name(anotherRootName)
                .build();

        HierarchyResponseDto anotherRootResponseDto = HierarchyResponseDto.builder()
                .id(anotherRootId)
                .name(anotherRootName)
                .build();

        String childName = "Child";
        Long childId = 3L;
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name(childName)
                .parentEntityId(rootDepartment.getId())
                .build();

        Department childDepartment = Department.builder()
                .id(childId)
                .name(childName)
                .parentId(rootDepartment.getId())
                .build();

        HierarchyResponseDto childResponseDto = HierarchyResponseDto.builder()
                .id(childId)
                .name(childName)
                .parentEntityId(rootDepartment.getId())
                .build();

        String grandChildName = "Grand Child";
        Long grandChildId = 4L;
        HierarchyRequestDto grandChildRequestDto = HierarchyRequestDto.builder()
                .name(grandChildName)
                .parentEntityId(childDepartment.getId())
                .build();

        Department grandChildDepartment = Department.builder()
                .id(grandChildId)
                .name(grandChildName)
                .parentId(childDepartment.getId())
                .build();

        HierarchyResponseDto grandChildResponseDto = HierarchyResponseDto.builder()
                .id(grandChildId)
                .name(grandChildName)
                .parentEntityId(childDepartment.getId())
                .build();

        DepartmentClosure selfDepartmentClosureForRoot = DepartmentClosure.builder()
                .ancestorId(rootDepartment.getId())
                .descendantId(rootDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure selfDepartmentClosureForAnotherRoot = DepartmentClosure.builder()
                .ancestorId(anotherRootDepartment.getId())
                .descendantId(anotherRootDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure selfDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(childDepartment.getId())
                .descendantId(childDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure ancestorDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(rootDepartment.getId())
                .descendantId(childDepartment.getId())
                .level(1)
                .build();

        DepartmentClosure selfDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(grandChildDepartment.getId())
                .descendantId(grandChildDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure parentDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(childDepartment.getId())
                .descendantId(grandChildDepartment.getId())
                .level(1)
                .build();

        DepartmentClosure ancestorDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(rootDepartment.getId())
                .descendantId(grandChildDepartment.getId())
                .level(2)
                .build();

        when(departmentRepository.findById(rootDepartment.getId())).thenReturn(Optional.of(rootDepartment));
        when(departmentRepository.findById(childDepartment.getId())).thenReturn(Optional.of(childDepartment));
        when(departmentRepository.findById(grandChildDepartment.getId())).thenReturn(Optional.of(grandChildDepartment));
        when(departmentRepository.findById(anotherRootDepartment.getId())).thenReturn(Optional.of(anotherRootDepartment));

        when(departmentRepository.save(any(Department.class)))
                .thenReturn(rootDepartment)
                .thenReturn(childDepartment)
                .thenReturn(grandChildDepartment)
                .thenReturn(anotherRootDepartment);

        when(departmentMapper.toEntity(any(HierarchyRequestDto.class)))
                .thenReturn(rootDepartment)
                .thenReturn(childDepartment)
                .thenReturn(grandChildDepartment)
                .thenReturn(anotherRootDepartment);

        when(departmentMapper.toDto(any(Department.class)))
                .thenReturn(rootResponseDto)
                .thenReturn(childResponseDto)
                .thenReturn(grandChildResponseDto)
                .thenReturn(anotherRootResponseDto);

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryMultipleLists(Arrays.asList(
                Arrays.asList(selfDepartmentClosureForRoot),
                Arrays.asList(selfDepartmentClosureForChild, ancestorDepartmentClosureForChild)
        ));

        when(departmentClosureFactory.createClosure(anotherRootDepartment.getId(), anotherRootDepartment.getId(), 0))
                .thenReturn(selfDepartmentClosureForAnotherRoot);
        when(departmentClosureFactory.createClosure(rootDepartment.getId(), rootDepartment.getId(), 0))
                .thenReturn(selfDepartmentClosureForRoot);
        when(departmentClosureFactory.createClosure(childDepartment.getId(), childDepartment.getId(), 0))
                .thenReturn(selfDepartmentClosureForChild);
        when(departmentClosureFactory.createClosure(rootDepartment.getId(), childDepartment.getId(), 1))
                .thenReturn(ancestorDepartmentClosureForChild);
        when(departmentClosureFactory.createClosure(grandChildDepartment.getId(), grandChildDepartment.getId(), 0))
                .thenReturn(selfDepartmentClosureForGrandChild);
        when(departmentClosureFactory.createClosure(childDepartment.getId(), grandChildDepartment.getId(), 1))
                .thenReturn(parentDepartmentClosureForGrandChild);
        when(departmentClosureFactory.createClosure(rootDepartment.getId(), grandChildDepartment.getId(), 2))
                .thenReturn(ancestorDepartmentClosureForGrandChild);

        // When: Creating the departments.
        HierarchyResponseDto rootResult = departmentService.createEntity(rootRequestDto);
        HierarchyResponseDto childResult = departmentService.createEntity(childRequestDto);
        HierarchyResponseDto grandChildResult = departmentService.createEntity(grandChildRequestDto);
        HierarchyResponseDto anotherRootResult = departmentService.createEntity(anotherRootRequestDto);

        // Then: Verify the subtree is created successfully.
        assertNotNull(rootResult);
        assertEquals(rootName, rootResult.getName());
        assertNull(rootResult.getParentEntityId());

        assertNotNull(anotherRootResult);
        assertEquals(anotherRootName, anotherRootResult.getName());
        assertNull(anotherRootResult.getParentEntityId());

        assertNotNull(childResult);
        assertEquals(childName, childResult.getName());
        assertEquals(rootResult.getId(), childResult.getParentEntityId());

        assertNotNull(grandChildResult);
        assertEquals(grandChildName, grandChildResult.getName());
        assertEquals(childResult.getId(), grandChildResult.getParentEntityId());

        verify(departmentRepository, times(4)).save(departmentArgumentCaptor.capture());
        List<Department> capturedDepartments = departmentArgumentCaptor.getAllValues();

        Department capturedRootDepartment = capturedDepartments.get(0);
        assertEquals(rootName, capturedRootDepartment.getName());

        Department capturedChildDepartment = capturedDepartments.get(1);
        assertEquals(childName, capturedChildDepartment.getName());
        assertEquals(capturedRootDepartment.getId(), capturedChildDepartment.getParentId());

        Department capturedGrandChildDepartment = capturedDepartments.get(2);
        assertEquals(grandChildName, capturedGrandChildDepartment.getName());
        assertEquals(capturedChildDepartment.getId(), capturedGrandChildDepartment.getParentId());

        Department capturedAnotherRootDepartment = capturedDepartments.get(3);
        assertEquals(anotherRootName, capturedAnotherRootDepartment.getName());

        verify(departmentClosureRepository, times(4)).saveAll(departmentClosuresArgumentCaptor.capture());
        List<List<DepartmentClosure>> capturedClosureEntriesList = departmentClosuresArgumentCaptor.getAllValues();

        assertNotNull(capturedClosureEntriesList);
        assertEquals(4, capturedClosureEntriesList.size());

        // root closures
        List<DepartmentClosure> rootClosures = capturedClosureEntriesList.get(0);

        assertNotNull(rootClosures);
        assertEquals(1, rootClosures.size());

        assertEquals(capturedRootDepartment.getId(), rootClosures.get(0).getAncestorId());
        assertEquals(capturedRootDepartment.getId(), rootClosures.get(0).getDescendantId());
        assertEquals(0, rootClosures.get(0).getLevel());

        // child closures
        List<DepartmentClosure> childClosures = capturedClosureEntriesList.get(1);
        assertNotNull(childClosures);
        assertEquals(2, childClosures.size());

        assertEquals(capturedChildDepartment.getId(), childClosures.get(0).getAncestorId());
        assertEquals(capturedChildDepartment.getId(), childClosures.get(0).getDescendantId());
        assertEquals(0, childClosures.get(0).getLevel());

        assertEquals(capturedRootDepartment.getId(), childClosures.get(1).getAncestorId());
        assertEquals(capturedChildDepartment.getId(), childClosures.get(1).getDescendantId());
        assertEquals(1, childClosures.get(1).getLevel());

        // grand child closures
        List<DepartmentClosure> grandChildClosures = capturedClosureEntriesList.get(2);

        assertNotNull(grandChildClosures);
        assertEquals(3, grandChildClosures.size());

        assertEquals(capturedGrandChildDepartment.getId(), grandChildClosures.get(0).getAncestorId());
        assertEquals(capturedGrandChildDepartment.getId(), grandChildClosures.get(0).getDescendantId());
        assertEquals(0, grandChildClosures.get(0).getLevel());

        assertEquals(capturedChildDepartment.getId(), grandChildClosures.get(1).getAncestorId());
        assertEquals(capturedGrandChildDepartment.getId(), grandChildClosures.get(1).getDescendantId());
        assertEquals(1, grandChildClosures.get(1).getLevel());

        assertEquals(capturedRootDepartment.getId(), grandChildClosures.get(2).getAncestorId());
        assertEquals(capturedGrandChildDepartment.getId(), grandChildClosures.get(2).getDescendantId());
        assertEquals(2, grandChildClosures.get(2).getLevel());

        // another root closures
        List<DepartmentClosure> anotherRootClosures = capturedClosureEntriesList.get(3);

        assertNotNull(anotherRootClosures);
        assertEquals(1, anotherRootClosures.size());

        assertEquals(capturedAnotherRootDepartment.getId(), anotherRootClosures.get(0).getAncestorId());
        assertEquals(capturedAnotherRootDepartment.getId(), anotherRootClosures.get(0).getDescendantId());
        assertEquals(0, anotherRootClosures.get(0).getLevel());

        verify(departmentClosureFactory, times(7)).createClosure(anyLong(), anyLong(), anyInt());
        verifySuccessFetch(mockQuery, 2);
    }

    /**
     * Unit test for creating a child department with empty parent closure.
     */
    @Test
    public void testCreateChildDepartment_EmptyParentClosure() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid HierarchyRequestDto for a child department.
        Department parentDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .build();

        Department createdDepartment = Department.builder()
                .id(2L)
                .name("Child Department")
                .parentId(parentDepartment.getId())
                .build();

        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Child Department");
        requestDto.setParentEntityId(parentDepartment.getId());


        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(createdDepartment.getId())).thenReturn(Optional.of(createdDepartment));
        when(departmentRepository.save(createdDepartment)).thenReturn(createdDepartment);

        when(departmentMapper.toEntity(requestDto)).thenReturn(createdDepartment);

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(Collections.emptyList());

        // When: Creating the department.
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.createEntity(requestDto);
        });

        // Then: Verify the ParentEntityNotFoundException is thrown.
        assertEquals("Parent entity not found.", exception.getMessage());
        verify(departmentRepository, times(1)).save(any(Department.class));
        verify(departmentRepository, times(1)).delete(any(Department.class));
        verify(departmentMapper, never()).toDto(any());
        verify(departmentClosureRepository, never()).saveAll(anyList());

        verifySuccessFetch(mockQuery, 1);
    }

    /**
     * Unit test for creating a department with an invalid name.
     */
    @Test
    public void testCreateDepartment_InvalidName() {
        // Given: A HierarchyRequestDto with an invalid name.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("");

        // When: Creating the department.
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            departmentService.createEntity(requestDto);
        });

        // Then: Verify the ValidationException is thrown.
        assertEquals("Entity name cannot be null or empty.", exception.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
        verify(departmentClosureRepository, never()).saveAll(anyList());
    }

    /**
     * Unit test for creating a department with a parent not found.
     */
    @Test
    public void testCreateDepartment_ParentNotFound() {
        // Given: A HierarchyRequestDto with a non-existent parent ID.
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName("Child Department");
        requestDto.setParentEntityId(-1L);

        when(departmentRepository.findById(-1L)).thenReturn(Optional.empty());

        // When: Creating the department.
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.createEntity(requestDto);
        });

        // Then: Verify the ParentEntityNotFoundException is thrown.
        assertEquals("Parent entity not found.", exception.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
        verify(departmentClosureRepository, never()).saveAll(anyList());
    }

    /**
     * Unit test for creating a department with a name exceeding max length.
     */
    @Test
    public void testCreateDepartment_NameTooLong() {
        // Given: A HierarchyRequestDto with a name exceeding max length.
        String longName = "ThisIsAVeryLongDepartmentNameThatExceedsMaxCharactersLimit";
        HierarchyRequestDto requestDto = new HierarchyRequestDto();
        requestDto.setName(longName);

        // When: Creating the department.
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            departmentService.createEntity(requestDto);
        });

        // Then: Verify the ValidationException is thrown.
        assertEquals("Entity name cannot be longer than " + maxNameLength + " characters.", exception.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
        verify(departmentClosureRepository, never()).saveAll(anyList());
    }

    /**
     * Unit test for updating a department successfully.
     */
    @Test
    public void testUpdateDepartment_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException, EntityNotFoundException {
        // Given: A valid HierarchyRequestDto for an existing department.
        Long departmentId = 1L;
        String updatedName = "Updated Department";

        HierarchyRequestDto updateRequestDto = HierarchyRequestDto.builder()
                .id(departmentId)
                .name(updatedName)
                .build();

        Department existingDepartment = Department.builder()
                .id(departmentId)
                .name("Original Department")
                .build();

        Department updatedDepartment = Department.builder()
                .id(departmentId)
                .name(updatedName)
                .build();

        HierarchyResponseDto updatedResponseDto = HierarchyResponseDto.builder()
                .id(departmentId)
                .name(updatedName)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);
        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(updatedDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(updatedResponseDto);

        // When: Updating the department.
        HierarchyResponseDto result = departmentService.updateEntity(departmentId, updateRequestDto);

        // Then: Verify the department is updated successfully.
        assertNotNull(result);
        assertEquals(updatedName, result.getName());
        assertNull(result.getParentEntityId());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(departmentArgumentCaptor.capture());

        Department capturedDepartment = departmentArgumentCaptor.getValue();
        assertEquals(updatedName, capturedDepartment.getName());
    }

    /**
     * Unit test for updating a department to root successfully.
     */
    @Test
    public void testUpdateDepartment_ToRoot_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException, EntityNotFoundException {
        // Given: A valid HierarchyRequestDto for an existing department to be updated to root.
        Long departmentId = 1L;
        String updatedName = "Updated Root Department";

        HierarchyRequestDto updateRequestDto = HierarchyRequestDto.builder()
                .id(departmentId)
                .name(updatedName)
                .parentEntityId(null)
                .build();

        Department existingDepartment = Department.builder()
                .id(departmentId)
                .name("Original Department")
                .parentId(2L)
                .build();

        DepartmentClosure parentSelfClosure = DepartmentClosure.builder()
                .descendantId(2L)
                .ancestorId(2L)
                .level(0)
                .build();

        Department updatedDepartment = Department.builder()
                .id(departmentId)
                .name(updatedName)
                .build();

        HierarchyResponseDto updatedResponseDto = HierarchyResponseDto.builder()
                .id(departmentId)
                .name(updatedName)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);
        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(updatedDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(updatedResponseDto);

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(Arrays.asList(parentSelfClosure));
        mockDeleteQuery(2L);
        // When: Updating the department to root.
        HierarchyResponseDto result = departmentService.updateEntity(departmentId, updateRequestDto);

        // Then: Verify the department is updated successfully to root.
        assertNotNull(result);
        assertEquals(updatedName, result.getName());
        assertNull(result.getParentEntityId());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(departmentArgumentCaptor.capture());
        verifySuccessFetch(mockQuery, 1);

        Department capturedDepartment = departmentArgumentCaptor.getValue();
        assertEquals(updatedName, capturedDepartment.getName());
        assertNull(capturedDepartment.getParentId());
    }

    /**
     * Unit test for updating a department with an invalid name.
     */
    @Test
    public void testUpdateDepartment_InvalidName() {
        // Given: An invalid HierarchyRequestDto with a null name.
        Long departmentId = 1L;

        HierarchyRequestDto updateRequestDto = HierarchyRequestDto.builder()
                .id(departmentId)
                .name(null)
                .build();

        // When & Then: Updating the department should throw ValidationException.
        assertThrows(ValidationException.class, () -> {
            departmentService.updateEntity(departmentId, updateRequestDto);
        });

        verify(departmentRepository, never()).findById(departmentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for updating a department when parent department is not found.
     */
    @Test
    public void testUpdateDepartment_ParentNotFound() {
        // Given: A HierarchyRequestDto with a non-existent parent department ID.
        Long departmentId = 1L;
        Long nonExistentParentId = -1L;

        HierarchyRequestDto updateRequestDto = HierarchyRequestDto.builder()
                .id(departmentId)
                .name("Updated Department")
                .parentEntityId(nonExistentParentId)
                .build();

        Department existingDepartment = Department.builder()
                .id(departmentId)
                .name("Original Department")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(nonExistentParentId)).thenReturn(Optional.empty());

        // When & Then: Updating the department should throw ParentEntityNotFoundException.
        assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.updateEntity(departmentId, updateRequestDto);
        });

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(nonExistentParentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for updating a real subtree successfully.
     */
    @Test
    public void testUpdateDepartmentRealSubtree_Success() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException, EntityNotFoundException {
        // Given: A valid HierarchyRequestDto for a real subtree.
        String rootName = "Root";
        Long rootId = 1L;
        HierarchyRequestDto rootRequestDto = HierarchyRequestDto.builder()
                .name(rootName)
                .build();

        Department rootDepartment = Department.builder()
                .id(rootId)
                .name(rootName)
                .build();

        HierarchyResponseDto rootResponseDto = HierarchyResponseDto.builder()
                .id(rootId)
                .name(rootName)
                .build();

        String anotherRootName = "Another Root";
        Long anotherRootId = 2L;
        HierarchyRequestDto anotherRootRequestDto = HierarchyRequestDto.builder()
                .name(anotherRootName)
                .build();

        Department anotherRootDepartment = Department.builder()
                .id(anotherRootId)
                .name(anotherRootName)
                .build();

        HierarchyResponseDto anotherRootResponseDto = HierarchyResponseDto.builder()
                .id(anotherRootId)
                .name(anotherRootName)
                .build();

        String childName = "Child";
        Long childId = 3L;
        HierarchyRequestDto childRequestDto = HierarchyRequestDto.builder()
                .name(childName)
                .parentEntityId(rootDepartment.getId())
                .build();

        HierarchyRequestDto updateChildRequestDto = HierarchyRequestDto.builder()
                .id(childId)
                .name(childName)
                .parentEntityId(anotherRootResponseDto.getId())
                .build();

        Department childDepartment = Department.builder()
                .id(childId)
                .name(childName)
                .parentId(rootDepartment.getId())
                .build();

        Department updatedChildDepartment = Department.builder()
                .id(childId)
                .name(childName)
                .parentId(anotherRootDepartment.getId())
                .build();

        HierarchyResponseDto childResponseDto = HierarchyResponseDto.builder()
                .id(childId)
                .name(childName)
                .parentEntityId(rootDepartment.getId())
                .build();

        HierarchyResponseDto updateChildResponseDto = HierarchyResponseDto.builder()
                .id(childId)
                .name(childName)
                .parentEntityId(anotherRootDepartment.getId())
                .build();

        String grandChildName = "Grand Child";
        Long grandChildId = 4L;
        HierarchyRequestDto grandChildRequestDto = HierarchyRequestDto.builder()
                .name(grandChildName)
                .parentEntityId(childDepartment.getId())
                .build();

        Department grandChildDepartment = Department.builder()
                .id(grandChildId)
                .name(grandChildName)
                .parentId(childDepartment.getId())
                .build();

        HierarchyResponseDto grandChildResponseDto = HierarchyResponseDto.builder()
                .id(grandChildId)
                .name(grandChildName)
                .parentEntityId(childDepartment.getId())
                .build();

        DepartmentClosure selfDepartmentClosureForRoot = DepartmentClosure.builder()
                .ancestorId(rootDepartment.getId())
                .descendantId(rootDepartment.getId())
                .level(0)
                .build();

        when(departmentClosureFactory.createClosure(rootDepartment.getId(), rootDepartment.getId(), 0))
                .thenReturn(selfDepartmentClosureForRoot);

        DepartmentClosure selfDepartmentClosureForAnotherRoot = DepartmentClosure.builder()
                .ancestorId(anotherRootDepartment.getId())
                .descendantId(anotherRootDepartment.getId())
                .level(0)
                .build();

        when(departmentClosureFactory.createClosure(anotherRootDepartment.getId(), anotherRootDepartment.getId(), 0))
                .thenReturn(selfDepartmentClosureForAnotherRoot);

        DepartmentClosure selfDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(childDepartment.getId())
                .descendantId(childDepartment.getId())
                .level(0)
                .build();

        when(departmentClosureFactory.createClosure(childDepartment.getId(), childDepartment.getId(), 0))
                .thenReturn(selfDepartmentClosureForChild);

        DepartmentClosure selfDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(grandChildDepartment.getId())
                .descendantId(grandChildDepartment.getId())
                .level(0)
                .build();

        when(departmentClosureFactory.createClosure(grandChildDepartment.getId(), grandChildDepartment.getId(), 0))
                .thenReturn(selfDepartmentClosureForGrandChild);

        DepartmentClosure ancestorDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(rootDepartment.getId())
                .descendantId(childDepartment.getId())
                .level(1)
                .build();

        when(departmentClosureFactory.createClosure(rootDepartment.getId(), childDepartment.getId(), 1))
                .thenReturn(ancestorDepartmentClosureForChild);

        DepartmentClosure updatedAncestorDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(anotherRootDepartment.getId())
                .descendantId(childDepartment.getId())
                .level(1)
                .build();

        when(departmentClosureFactory.createClosure(anotherRootDepartment.getId(), childDepartment.getId(), 1))
                .thenReturn(updatedAncestorDepartmentClosureForChild);

        DepartmentClosure ancestorDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(childDepartment.getId())
                .descendantId(grandChildDepartment.getId())
                .level(1)
                .build();

        when(departmentClosureFactory.createClosure(childDepartment.getId(), grandChildDepartment.getId(), 1))
                .thenReturn(ancestorDepartmentClosureForGrandChild);

        DepartmentClosure grandAncestorDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(rootDepartment.getId())
                .descendantId(grandChildDepartment.getId())
                .level(2)
                .build();

        when(departmentClosureFactory.createClosure(rootDepartment.getId(), grandChildDepartment.getId(), 2))
                .thenReturn(grandAncestorDepartmentClosureForGrandChild);

        DepartmentClosure updatedGrandAncestorDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(anotherRootDepartment.getId())
                .descendantId(grandChildDepartment.getId())
                .level(2)
                .build();

        when(departmentClosureFactory.createClosure(anotherRootDepartment.getId(), grandChildDepartment.getId(), 2))
                .thenReturn(updatedGrandAncestorDepartmentClosureForGrandChild);

        when(departmentRepository.findById(rootDepartment.getId())).thenReturn(Optional.of(rootDepartment));
        when(departmentRepository.findById(childDepartment.getId())).thenReturn(Optional.of(childDepartment));
        when(departmentRepository.findById(grandChildDepartment.getId())).thenReturn(Optional.of(grandChildDepartment));
        when(departmentRepository.findById(anotherRootDepartment.getId())).thenReturn(Optional.of(anotherRootDepartment));

        when(departmentRepository.save(any(Department.class)))
                .thenReturn(rootDepartment)
                .thenReturn(childDepartment)
                .thenReturn(grandChildDepartment)
                .thenReturn(anotherRootDepartment)
                .thenReturn(updatedChildDepartment);

        when(departmentMapper.toEntity(any(HierarchyRequestDto.class)))
                .thenReturn(rootDepartment)
                .thenReturn(childDepartment)
                .thenReturn(grandChildDepartment)
                .thenReturn(anotherRootDepartment)
                .thenReturn(updatedChildDepartment);

        when(departmentMapper.toDto(any(Department.class)))
                .thenReturn(rootResponseDto)
                .thenReturn(childResponseDto)
                .thenReturn(grandChildResponseDto)
                .thenReturn(anotherRootResponseDto)
                .thenReturn(updateChildResponseDto);

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryMultipleLists(Arrays.asList(
                Arrays.asList(selfDepartmentClosureForRoot),
                Arrays.asList(
                        selfDepartmentClosureForChild,
                        ancestorDepartmentClosureForChild
                ),
                Arrays.asList(selfDepartmentClosureForAnotherRoot),
                Arrays.asList(
                        selfDepartmentClosureForChild,
                        ancestorDepartmentClosureForGrandChild
                ),
                Arrays.asList(selfDepartmentClosureForAnotherRoot)
        ));

        mockDeleteQuery(3L);

        // When: Creating the departments and updating child department.
        HierarchyResponseDto rootResult = departmentService.createEntity(rootRequestDto);
        HierarchyResponseDto childResult = departmentService.createEntity(childRequestDto);
        HierarchyResponseDto grandChildResult = departmentService.createEntity(grandChildRequestDto);
        HierarchyResponseDto anotherRootResult = departmentService.createEntity(anotherRootRequestDto);

        // Then: Verify the subtree is created successfully.
        assertNotNull(rootResult);
        assertEquals(rootName, rootResult.getName());
        assertNull(rootResult.getParentEntityId());

        assertNotNull(anotherRootResult);
        assertEquals(anotherRootName, anotherRootResult.getName());
        assertNull(anotherRootResult.getParentEntityId());

        assertNotNull(childResult);
        assertEquals(childName, childResult.getName());
        assertEquals(rootResult.getId(), childResult.getParentEntityId());

        assertNotNull(grandChildResult);
        assertEquals(grandChildName, grandChildResult.getName());
        assertEquals(childResult.getId(), grandChildResult.getParentEntityId());

        verify(departmentRepository, times(4)).save(departmentArgumentCaptor.capture());
        List<Department> capturedDepartments = departmentArgumentCaptor.getAllValues();

        Department capturedRootDepartment = capturedDepartments.get(0);
        assertEquals(rootName, capturedRootDepartment.getName());

        Department capturedChildDepartment = capturedDepartments.get(1);
        assertEquals(childName, capturedChildDepartment.getName());
        assertEquals(capturedRootDepartment.getId(), capturedChildDepartment.getParentId());

        Department capturedGrandChildDepartment = capturedDepartments.get(2);
        assertEquals(grandChildName, capturedGrandChildDepartment.getName());
        assertEquals(capturedChildDepartment.getId(), capturedGrandChildDepartment.getParentId());

        Department capturedAnotherRootDepartment = capturedDepartments.get(3);
        assertEquals(anotherRootName, capturedAnotherRootDepartment.getName());

        verify(departmentClosureRepository, times(4)).saveAll(departmentClosuresArgumentCaptor.capture());
        List<List<DepartmentClosure>> capturedClosureEntriesList = departmentClosuresArgumentCaptor.getAllValues();

        assertNotNull(capturedClosureEntriesList);
        assertEquals(4, capturedClosureEntriesList.size());

        // root closures
        List<DepartmentClosure> rootClosures = capturedClosureEntriesList.get(0);

        assertNotNull(rootClosures);
        assertEquals(1, rootClosures.size());

        assertEquals(capturedRootDepartment.getId(), rootClosures.get(0).getAncestorId());
        assertEquals(capturedRootDepartment.getId(), rootClosures.get(0).getDescendantId());
        assertEquals(0, rootClosures.get(0).getLevel());

        // child closures
        List<DepartmentClosure> childClosures = capturedClosureEntriesList.get(1);
        assertNotNull(childClosures);
        assertEquals(2, childClosures.size());

        assertEquals(capturedChildDepartment.getId(), childClosures.get(0).getAncestorId());
        assertEquals(capturedChildDepartment.getId(), childClosures.get(0).getDescendantId());
        assertEquals(0, childClosures.get(0).getLevel());

        assertEquals(capturedRootDepartment.getId(), childClosures.get(1).getAncestorId());
        assertEquals(capturedChildDepartment.getId(), childClosures.get(1).getDescendantId());
        assertEquals(1, childClosures.get(1).getLevel());

        // grand child closures
        List<DepartmentClosure> grandChildClosures = capturedClosureEntriesList.get(2);

        assertNotNull(grandChildClosures);
        assertEquals(3, grandChildClosures.size());

        assertEquals(capturedGrandChildDepartment.getId(), grandChildClosures.get(0).getAncestorId());
        assertEquals(capturedGrandChildDepartment.getId(), grandChildClosures.get(0).getDescendantId());
        assertEquals(0, grandChildClosures.get(0).getLevel());

        assertEquals(capturedChildDepartment.getId(), grandChildClosures.get(1).getAncestorId());
        assertEquals(capturedGrandChildDepartment.getId(), grandChildClosures.get(1).getDescendantId());
        assertEquals(1, grandChildClosures.get(1).getLevel());

        assertEquals(capturedRootDepartment.getId(), grandChildClosures.get(2).getAncestorId());
        assertEquals(capturedGrandChildDepartment.getId(), grandChildClosures.get(2).getDescendantId());
        assertEquals(2, grandChildClosures.get(2).getLevel());

        // another root closures
        List<DepartmentClosure> anotherRootClosures = capturedClosureEntriesList.get(3);

        assertNotNull(anotherRootClosures);
        assertEquals(1, anotherRootClosures.size());

        assertEquals(capturedAnotherRootDepartment.getId(), anotherRootClosures.get(0).getAncestorId());
        assertEquals(capturedAnotherRootDepartment.getId(), anotherRootClosures.get(0).getDescendantId());
        assertEquals(0, anotherRootClosures.get(0).getLevel());

        // When: Creating the departments and updating child department.
        HierarchyResponseDto updatedChildResult = departmentService.updateEntity(childResult.getId(), updateChildRequestDto);

        assertNotNull(updatedChildResult);
        assertEquals(childName, updatedChildResult.getName());
        assertEquals(anotherRootResult.getId(), updatedChildResult.getParentEntityId());

        verify(departmentRepository, times(5)).save(departmentArgumentCaptor.capture());
        Department capturedDepartment = departmentArgumentCaptor.getAllValues().get(8);

        assertNotNull(capturedDepartment);
        assertEquals(childName, capturedDepartment.getName());
        assertEquals(anotherRootResult.getId(), capturedDepartment.getParentId());

        verify(departmentClosureRepository, times(5)).saveAll(departmentClosuresArgumentCaptor.capture());
        List<List<DepartmentClosure>> updatedCapturedClosureEntriesList = departmentClosuresArgumentCaptor.getAllValues();
        List<DepartmentClosure> capturedClosureEntries = departmentClosuresArgumentCaptor.getAllValues().get(8);

        assertNotNull(capturedClosureEntries);
        assertEquals(5, capturedClosureEntries.size());

        assertEquals(childResult.getId(), capturedClosureEntries.get(0).getAncestorId());
        assertEquals(childResult.getId(), capturedClosureEntries.get(0).getDescendantId());
        assertEquals(0, capturedClosureEntries.get(0).getLevel());

        assertEquals(grandChildResult.getId(), capturedClosureEntries.get(1).getAncestorId());
        assertEquals(grandChildResult.getId(), capturedClosureEntries.get(1).getDescendantId());
        assertEquals(0, capturedClosureEntries.get(1).getLevel());

        assertEquals(anotherRootResult.getId(), capturedClosureEntries.get(2).getAncestorId());
        assertEquals(childResult.getId(), capturedClosureEntries.get(2).getDescendantId());
        assertEquals(1, capturedClosureEntries.get(2).getLevel());

        assertEquals(childResult.getId(), capturedClosureEntries.get(3).getAncestorId());
        assertEquals(grandChildResult.getId(), capturedClosureEntries.get(3).getDescendantId());
        assertEquals(1, capturedClosureEntries.get(3).getLevel());

        assertEquals(anotherRootResult.getId(), capturedClosureEntries.get(4).getAncestorId());
        assertEquals(grandChildResult.getId(), capturedClosureEntries.get(4).getDescendantId());
        assertEquals(2, capturedClosureEntries.get(4).getLevel());

        verifySuccessFetch(mockQuery, 5);
    }

    /**
     * Unit test for updating a department not found.
     */
    @Test
    public void testUpdateDepartment_NotFound() {
        // Given: A HierarchyRequestDto with a non-existent department ID.
        Long departmentId = -1L;

        HierarchyRequestDto updateRequestDto = HierarchyRequestDto.builder()
                .id(departmentId)
                .name("Non-Existent Department")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When & Then: Updating the department should throw EntityNotFoundException.
        assertThrows(EntityNotFoundException.class, () -> {
            departmentService.updateEntity(departmentId, updateRequestDto);
        });

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for updating a department without any modification.
     */
    @Test
    public void testUpdateDepartment_NoModification() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException, EntityNotFoundException {
        // Given: A valid HierarchyRequestDto for an existing department.
        Long departmentId = 1L;

        String name = "Original Department";

        HierarchyRequestDto updateRequestDto = HierarchyRequestDto.builder()
                .id(departmentId)
                .name(name)
                .build();

        Department existingDepartment = Department.builder()
                .id(departmentId)
                .name(name)
                .build();

        HierarchyResponseDto updatedResponseDto = HierarchyResponseDto.builder()
                .id(departmentId)
                .name(name)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);
        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(existingDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(updatedResponseDto);

        // When: Updating the department.
        HierarchyResponseDto result = departmentService.updateEntity(departmentId, updateRequestDto);

        // Then: Verify the department is updated successfully.
        assertNotNull(result);
        assertEquals(name, result.getName());
        assertNull(result.getParentEntityId());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for updating a department with empty parent closure.
     */
    @Test
    public void testUpdateDepartment_EmptyParentClosure() throws ParentEntityNotFoundException, ValidationException, DataIntegrityException, EntityNotFoundException {
        // Given: A valid HierarchyRequestDto for a child department.
        long parentId = 1L;
        Department parentDepartment = Department.builder()
                .id(parentId)
                .name("Root Department")
                .build();

        long childId = 2L;
        String childName = "Child Department";
        Department createdDepartment = Department.builder()
                .id(childId)
                .name(childName)
                .build();

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .id(childId)
                .name(childName)
                .parentEntityId(parentDepartment.getId())
                .build();

        DepartmentClosure departmentClosure = DepartmentClosure.builder()
                .ancestorId(childId)
                .descendantId(childId)
                .level(0)
                .build();


        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(createdDepartment.getId())).thenReturn(Optional.of(createdDepartment));

        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(createdDepartment);

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(Collections.emptyList());

        // When & Then: Updating the department should throw ParentEntityNotFoundException.
        assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.updateEntity(childId, requestDto);
        });

        verify(departmentRepository, times(1)).findById(childId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentRepository, never()).save(any());
        verify(departmentMapper, never()).toDto(any());

        verifySuccessFetch(mockQuery, 1);
    }

    /**
     * Unit test for updating a department with circular dependency.
     */
    @Test
    void testUpdateDepartmentHasCircularDependency() {
        // Given: An invalid HierarchyRequestDto for a parent department.
        long parentId = 1L;
        Department parentDepartment = Department.builder()
                .id(parentId)
                .name("Root Department")
                .build();

        long childId = 2L;
        String childName = "Child Department";
        Department childDepartment = Department.builder()
                .id(childId)
                .name(childName)
                .parentId(parentDepartment.getId())
                .build();

        HierarchyRequestDto requestDto = HierarchyRequestDto.builder()
                .id(parentId)
                .name("Root Department")
                .parentEntityId(childDepartment.getId())
                .build();

        DepartmentClosure selfDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(childId)
                .descendantId(childId)
                .level(0)
                .build();

        DepartmentClosure ancestorDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(parentId)
                .descendantId(childId)
                .level(1)
                .build();

        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(childDepartment.getId())).thenReturn(Optional.of(childDepartment));

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(Arrays.asList(
                selfDepartmentClosureForChild,
                ancestorDepartmentClosureForChild
        ));

        when(departmentMapper.toEntity(any(HierarchyRequestDto.class))).thenReturn(parentDepartment);

        // When & Then: Updating the department should throw DataIntegrityException.
        assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateEntity(parentId, requestDto);
        });

        verify(departmentRepository, times(1)).findById(childId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentRepository, never()).save(any());
        verify(departmentMapper, never()).toDto(any());
        verifySuccessFetch(mockQuery, 1);
    }

    /**
     * Unit test for deleting a department with non-existing id.
     */
    @Test
    public void testDeleteDepartment_NotFound() {
        // Given: A HierarchyRequestDto with a non-existent department ID.
        Long nonExistingId = -1L;

        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Deleting the department.
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.deleteEntity(nonExistingId);
        });

        // Then: Verify it should throw EntityNotFoundException.

        assertEquals("Entity not found with id: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(departmentClosureRepository, never()).delete(any(DepartmentClosure.class));
        verify(departmentRepository, never()).delete(any(Department.class));
    }

    /**
     * Unit test for deleting a department with  circular reference.
     */
    @Test
    public void testDeleteDepartment_CircularReference() throws EntityNotFoundException, DataIntegrityException {
        // Given: A departments with circular references.
        Department parentDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .parentId(2L)
                .build();

        Department childDepartment = Department.builder()
                .id(2L)
                .name("Child Department")
                .parentId(1L)
                .build();

        DepartmentClosure parentDepartmentSelfClosure = DepartmentClosure.builder()
                .descendantId(parentDepartment.getId())
                .ancestorId(parentDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure parentDepartmentAncestorClosure = DepartmentClosure.builder()
                .descendantId(parentDepartment.getId())
                .ancestorId(childDepartment.getId())
                .level(0)
                .build();

        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(Arrays.asList(parentDepartmentSelfClosure,
                parentDepartmentAncestorClosure));

        // When: Deleting the parent department
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteEntity((parentDepartment.getId()));
        });

        // Then: Verify it should throw DataIntegrityException due to circular reference
        assertEquals("Circular dependency detected: Entity cannot be its own ancestor.", exception.getMessage());

        verify(departmentRepository, times(1)).findById(parentDepartment.getId());
        verify(departmentRepository, never()).delete(parentDepartment);
        verifySuccessFetch(mockQuery, 1);
    }

    /**
     * Unit test for deleting a department with  circular reference.
     */
    @Test
    public void testDeleteDepartment_SelfCircularReference() throws EntityNotFoundException, DataIntegrityException {
        // Given: A departments with circular references.
        Department parentDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .parentId(1L)
                .build();


        DepartmentClosure selfDepartmentClosureForParent = DepartmentClosure.builder()
                .ancestorId(parentDepartment.getId())
                .descendantId(parentDepartment.getId())
                .level(0)
                .build();


        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));

        // When: Deleting the parent department
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteEntity((parentDepartment.getId()));
        });

        // Then: Verify it should throw DataIntegrityException due to circular reference
        assertEquals("Circular dependency detected: Entity cannot be its own ancestor.", exception.getMessage());

        verify(departmentRepository, times(1)).findById(parentDepartment.getId());
        verify(departmentRepository, never()).delete(parentDepartment);

    }

    /**
     * Unit test for deleting a department with  its descendants successfully.
     */
    @Test
    void testDeleteDepartment_ExistingId_WithDescendants() {
        Department parentDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .build();

        Department childDepartment = Department.builder()
                .id(2L)
                .name("Child Department")
                .parentId(parentDepartment.getId())
                .build();

        // Assuming descendants are properly mocked here
        DepartmentClosure selfDepartmentClosureForParent = DepartmentClosure.builder()
                .ancestorId(parentDepartment.getId())
                .descendantId(parentDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure selfDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(childDepartment.getId())
                .descendantId(childDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure ancestorDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(parentDepartment.getId())
                .descendantId(childDepartment.getId())
                .level(1)
                .build();


        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(childDepartment.getId())).thenReturn(Optional.of(childDepartment));

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryMultipleLists(Arrays.asList(
                Arrays.asList(selfDepartmentClosureForParent),
                Arrays.asList(selfDepartmentClosureForChild,
                        ancestorDepartmentClosureForChild),
                Arrays.asList(selfDepartmentClosureForChild,
                        ancestorDepartmentClosureForChild),
                Arrays.asList(selfDepartmentClosureForChild)
        ));

        DeleteQueryComponents deleteQueryComponents = mockDeleteQuery(1L);

        // When: Deleting the department
        departmentService.deleteEntity(1L);

        // Then: Verify that delete methods are called appropriately
        verify(departmentRepository, times(1)).findById(parentDepartment.getId());
        verify(departmentRepository, times(1)).delete(parentDepartment);

        verifySuccessDelete(deleteQueryComponents, 2);
        verifySuccessFetch(mockQuery, 1);
    }

    // DepartmentServiceTest.java

    /**
     * Test to verify getAllEntities() when there are no departments in the repository.
     */
    @Test
    public void testGetAllDepartments_noDepartments() {
        // Given: No departments in the repository
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());

        // When: Retrieving all departments
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then: Verify the result is an empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(departmentRepository, times(1)).findAll();
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test to verify getAllEntities() when there are departments in the repository.
     */
    @Test
    public void testGetAllDepartments_withDepartments() {
        // Given: A list of departments in the repository
        Department department1 = Department.builder()
                .id(1L)
                .name("Department 1")
                .build();

        Department department2 = Department.builder()
                .id(2L)
                .name("Department 2")
                .build();

        when(departmentRepository.findAll()).thenReturn(Arrays.asList(department1, department2));

        HierarchyResponseDto responseDto1 = HierarchyResponseDto.builder()
                .id(1L)
                .name("Department 1")
                .build();

        HierarchyResponseDto responseDto2 = HierarchyResponseDto.builder()
                .id(2L)
                .name("Department 2")
                .build();

        when(departmentMapper
                .toDtoList(
                        eq(Arrays.asList(department1, department2)),
                        anyBoolean()
                )
        )
                .thenReturn(
                        Arrays.asList(responseDto1, responseDto2)
                );

        // When: Retrieving all departments
        List<HierarchyResponseDto> result = departmentService.getAllEntities();

        // Then: Verify the result contains the expected departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(responseDto1));
        assertTrue(result.contains(responseDto2));

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
                .build();
        Department dept2 = Department.builder().id(2L).name("Department 2").parentId(dept1.getId())
                .build();
        Department dept3 = Department.builder().id(3L).name("Department 3").parentId(dept2.getId())
                .build();
        Department dept4 = Department.builder().id(4L).name("Department 4").parentId(dept2.getId())
                .build();
        Department dept5 = Department.builder().id(5L).name("Department 5").parentId(dept1.getId())
                .build();

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
     * Test to verify getSubEntities() when the parent department exists.
     */
    @Test
    public void testGetSubDepartments_existingParent() throws ParentEntityNotFoundException {
        // Given: An existing parent department and its sub-departments
        Long parentId = 1L;
        Department parentDepartment = Department.builder()
                .id(parentId)
                .name("Parent Department")
                .build();

        Department subDepartment1 = Department.builder()
                .id(2L)
                .name("Sub Department 1")
                .parentId(parentId)
                .build();

        Department subDepartment2 = Department.builder()
                .id(3L)
                .name("Sub Department 2")
                .parentId(parentId)
                .build();

        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));

        mockSelectFromQueryList(Arrays.asList(subDepartment1, subDepartment2));

        HierarchyResponseDto responseDto1 = HierarchyResponseDto.builder()
                .id(2L)
                .name("Sub Department 1")
                .build();

        HierarchyResponseDto responseDto2 = HierarchyResponseDto.builder()
                .id(3L)
                .name("Sub Department 2")
                .build();

        when(departmentMapper
                .toDtoList(eq(Arrays.asList(subDepartment1, subDepartment2)), anyBoolean()))
                .thenReturn(Arrays.asList(responseDto1, responseDto2));
        when(departmentMapper.toDto(subDepartment2)).thenReturn(responseDto2);

        // When: Retrieving sub-departments of the parent department
        List<HierarchyResponseDto> result = departmentService.getSubEntities(parentId);

        // Then: Verify the result contains the expected sub-departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(responseDto1));
        assertTrue(result.contains(responseDto2));

        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentMapper, times(1)).toDtoList(eq(Arrays.asList(subDepartment1, subDepartment2)), anyBoolean());
    }

    /**
     * Test to verify getSubEntities() when the parent department does not exist.
     */
    @Test
    public void testGetSubDepartments_nonExistingParent() {
        // Given: A non-existent parent department ID
        Long nonExistingParentId = -1L;
        when(departmentRepository.findById(nonExistingParentId)).thenReturn(Optional.empty());

        // When & Then: Retrieving sub-departments should throw ParentEntityNotFoundException
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.getSubEntities(nonExistingParentId);
        });

        // Then: Verify it should throw ParentEntityNotFoundException
        assertEquals("Parent entity not found with id: " + nonExistingParentId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingParentId);
        verify(departmentMapper, never()).toDto(any(Department.class));
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
     * Test to verify getEntityById() when the department exists.
     */
    @Test
    public void testGetDepartmentById_ExistingDepartment() throws EntityNotFoundException {
        // Given: An existing department
        Long departmentId = 1L;
        Department department = Department.builder()
                .id(departmentId)
                .name("Engineering")
                .build();

        HierarchyResponseDto responseDto = HierarchyResponseDto.builder()
                .id(departmentId)
                .name("Engineering")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentMapper.toDto(eq(department), anyBoolean())).thenReturn(responseDto);

        // When: Retrieving the department by ID
        HierarchyResponseDto result = departmentService.getEntityById(departmentId);

        // Then: Verify the result matches the expected department
        assertNotNull(result);
        assertEquals(departmentId, result.getId());
        assertEquals("Engineering", result.getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentMapper, times(1)).toDto(eq(department), anyBoolean());
    }

    /**
     * Test to verify getEntityById() when the department does not exist.
     */
    @Test
    public void testGetDepartmentById_NonExistingDepartment() {
        // Given: A non-existent department ID
        Long nonExistingId = -1L;
        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Retrieving the department should throw EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getEntityById(nonExistingId);
        });

        // Then: Verify it should throw EntityNotFoundException
        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test to verify searchEntitiesByName() when departments with the search name exist.
     */
    @Test
    public void testSearchDepartmentsByName_ExistingName() {
        // Given: Existing departments with matching names
        String searchName = "Engineering";
        List<Department> departments = Arrays.asList(
                Department.builder().id(1L).name("Software Engineering").build(),
                Department.builder().id(2L).name("Engineering Team").build()
        );

        List<HierarchyResponseDto> responseDtos = Arrays.asList(
                HierarchyResponseDto.builder().id(1L).name("Software Engineering").build(),
                HierarchyResponseDto.builder().id(2L).name("Engineering Team").build()
        );

        mockSelectFromQueryList(departments);

        when(departmentMapper.toDtoList(eq(departments), anyBoolean())).thenReturn(responseDtos);

        // When: Searching departments by name
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(searchName);

        // Then: Verify the result matches the expected departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Software Engineering", result.get(0).getName());
        assertEquals("Engineering Team", result.get(1).getName());

        verify(departmentMapper, times(1)).toDtoList(anyList(), anyBoolean());
    }

    /**
     * Test to verify searchEntitiesByName() when no departments with the search name exist.
     */
    @Test
    public void testSearchDepartmentsByName_NonExistingName() {
        // Given: No departments with the search name
        String searchName = "Marketing";

        mockSelectFromQueryList(Collections.emptyList());
        // When: Searching departments by name
        List<HierarchyResponseDto> result = departmentService.searchEntitiesByName(searchName);

        // Then: Verify the result is an empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(departmentMapper, never()).toDto(any(Department.class));
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
     * Test Case: Retrieve existing parent department for a given department.
     * <p>
     * Given: Existing parent department
     * - departmentId: 1
     * - parentId: 2
     * - childDepartment: Department with parentId set to parentId
     * - parentDepartment: Department with id parentId
     */
    @Test
    public void testGetParentDepartment_existingParent() {
        Long departmentId = 1L;
        Long parentId = 2L;
        Department childDepartment = Department.builder()
                .id(departmentId)
                .parentId(parentId)
                .build();
        Department parentDepartment = Department.builder()
                .id(parentId)
                .name("Parent Department")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(childDepartment));
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));

        HierarchyResponseDto parentDto = HierarchyResponseDto.builder()
                .id(parentDepartment.getId())
                .name(parentDepartment.getName())
                .build();

        when(departmentMapper.toDto(eq(parentDepartment), anyBoolean())).thenReturn(parentDto);

        // When: Retrieving parent department
        HierarchyResponseDto result = departmentService.getParentEntity(departmentId);

        // Then: Verify the parent department is returned
        assertNotNull(result);
        assertEquals(parentDto.getId(), result.getId());
        assertEquals(parentDto.getName(), result.getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentMapper, times(1)).toDto(eq(parentDepartment), anyBoolean());
    }

    /**
     * Test Case: Attempt to retrieve parent department for a non-existing department.
     * <p>
     * Given: Non-existing department ID
     * - nonExistingId: -1
     */
    @Test
    public void testGetParentDepartment_nonExistingDepartment() {
        Long nonExistingId = -1L;

        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Retrieving parent department should throw EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getParentEntity(nonExistingId);
        });

        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(anyLong());
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test Case: Attempt to retrieve parent department for a department with no parent.
     * <p>
     * Given: Existing department with no parent
     * - nonExistingParentId: -1
     * - departmentId: 1
     * - department: Department with parentId set to nonExistingParentId
     */
    @Test
    public void testGetParentDepartment_noParent() {
        Long departmentId = 1L;
        Department department = Department.builder()
                .id(departmentId)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        // When & Then: Retrieving parent department should throw ParentEntityNotFoundException
        ParentEntityNotFoundException exception = assertThrows(ParentEntityNotFoundException.class, () -> {
            departmentService.getParentEntity(departmentId);
        });

        assertEquals("Entity with ID " + departmentId + " has no parent entity", exception.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test Case: Retrieve descendants for an existing department.
     * <p>
     * Given: Existing department with descendants
     * - departmentId: 1
     * - department: Root Department
     * - child1: Child Department 1 with parentId set to departmentId
     * - child2: Child Department 2 with parentId set to departmentId
     * - departmentClosures: List of DepartmentClosure objects representing the hierarchy
     * - descendantIds: List of descendant IDs
     */
    @Test
    public void testGetDescendants_existingDepartment() {
        Long departmentId = 1L;
        Department department = Department.builder()
                .id(departmentId)
                .name("Root Department")
                .build();

        Department child1 = Department.builder()
                .id(2L)
                .name("Child Department 1")
                .parentId(departmentId)
                .build();

        Department child2 = Department.builder()
                .id(3L)
                .name("Child Department 2")
                .parentId(departmentId)
                .build();

        List<DepartmentClosure> departmentClosures = Arrays.asList(
                DepartmentClosure.builder()
                        .ancestorId(departmentId)
                        .descendantId(departmentId)
                        .level(0)
                        .build(),
                DepartmentClosure.builder()
                        .ancestorId(departmentId)
                        .descendantId(child1.getId())
                        .level(1)
                        .build(),
                DepartmentClosure.builder()
                        .ancestorId(departmentId)
                        .descendantId(child2.getId())
                        .level(1)
                        .build()
        );

        List<Long> descendantIds = Arrays.asList(child1.getId(), child2.getId());

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentRepository.findById(child1.getId())).thenReturn(Optional.of(child1));
        when(departmentRepository.findById(child2.getId())).thenReturn(Optional.of(child2));

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(departmentClosures);

        HierarchyResponseDto child1Dto = HierarchyResponseDto.builder()
                .id(child1.getId())
                .name(child1.getName())
                .build();

        HierarchyResponseDto child2Dto = HierarchyResponseDto.builder()
                .id(child2.getId())
                .name(child2.getName())
                .build();

        when(departmentMapper
                .toDtoList(eq(Arrays.asList(child1, child2)), anyBoolean()))
                .thenReturn(Arrays.asList(child1Dto, child2Dto));
        when(departmentMapper.toDto(child2)).thenReturn(child2Dto);

        // When: Retrieving descendants
        List<HierarchyResponseDto> result = departmentService.getDescendants(departmentId);

        // Then: Verify descendants are returned correctly
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(child1Dto.getId(), result.get(0).getId());
        assertEquals(child1Dto.getName(), result.get(0).getName());
        assertEquals(child2Dto.getId(), result.get(1).getId());
        assertEquals(child2Dto.getName(), result.get(1).getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(child1.getId());
        verify(departmentRepository, times(1)).findById(child2.getId());
        verify(departmentMapper, times(1)).toDtoList(eq(Arrays.asList(child1, child2)), anyBoolean());
        verifySuccessFetch(mockQuery, 1);
    }

    /**
     * Test Case: Attempt to retrieve descendants for a non-existing department.
     * <p>
     * Given: Non-existing department ID
     * - nonExistingId: -1
     */
    @Test
    public void testGetDescendants_nonExistingDepartment() {
        Long nonExistingId = -1L;

        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Retrieving descendants should throw EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getDescendants(nonExistingId);
        });

        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(anyLong());
        verify(departmentMapper, never()).toDto(any(Department.class));
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

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(createMockDescendantsClosures());

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
        verifySuccessFetch(mockQuery, 1);
        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));
    }

    /**
     * Helper method to create mock descendants.
     *
     * @return a list of mock descendant entities.
     */
    private List<Department> createMockDescendants() {
        // Replace with your mock descendant creation logic
        Department Department1 = Department.builder().id(1L).name("Department 1").parentId(null).build();
        Department Department2 = Department.builder().id(2L).name("Department 2").parentId(null).build();
        Department Department3 = Department.builder().id(3L).name("Department 3").parentId(null).build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(Department1));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(Department2));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(Department3));

        return Arrays.asList(
                Department1,
                Department2,
                Department3
                // Add more descendants as needed
        );
    }

    /**
     * Helper method to create mock descendant closures.
     *
     * @return a list of mock descendant entities.
     */
    private List<DepartmentClosure> createMockDescendantsClosures() {
        // Replace with your mock descendant creation logic
        return Arrays.asList(
                DepartmentClosure.builder().id(1L)
                        .ancestorId(1L).descendantId(1L).level(0).build(),
                DepartmentClosure.builder().id(2L)
                        .ancestorId(2L).descendantId(2L).level(0).build(),
                DepartmentClosure.builder().id(3L)
                        .ancestorId(3L).descendantId(3L).level(0).build()
                // Add more descendants as needed
        );
    }

    /**
     * Test Case: Retrieve ancestors for an existing department.
     * <p>
     * Given: Existing department with ancestors
     * - departmentId: 1
     * - department: Department with id departmentId
     * - closure1: DepartmentClosure with ancestorId and descendantId set to departmentId, level 0
     * - closure2: DepartmentClosure with ancestorId set to 2, descendantId set to departmentId, level 1
     * - closure3: DepartmentClosure with ancestorId set to 3, descendantId set to departmentId, level 2
     */
    @Test
    void testGetAncestors_existingDepartment() {
        Long departmentId = 1L;
        Department department = new Department();
        department.setId(departmentId);

        Department ancestor1 = new Department();
        ancestor1.setId(2L);
        ancestor1.setName("Ancestor 1");

        Department ancestor2 = new Department();
        ancestor2.setId(3L);
        ancestor2.setName("Ancestor 2");

        DepartmentClosure closure1 = new DepartmentClosure();
        closure1.setAncestorId(departmentId);
        closure1.setDescendantId(departmentId);
        closure1.setLevel(0);

        DepartmentClosure closure2 = new DepartmentClosure();
        closure2.setAncestorId(2L);
        closure2.setDescendantId(departmentId);
        closure2.setLevel(1);

        DepartmentClosure closure3 = new DepartmentClosure();
        closure3.setAncestorId(3L);
        closure3.setDescendantId(departmentId);
        closure3.setLevel(2);

        List<DepartmentClosure> closureList = Arrays.asList(closure1, closure2, closure3);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(closureList);

        when(departmentRepository.findById(2L)).thenReturn(Optional.of(ancestor1));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(ancestor2));

        HierarchyResponseDto ancestorDto1 = new HierarchyResponseDto();
        ancestorDto1.setId(ancestor1.getId());
        ancestorDto1.setName(ancestor1.getName());

        HierarchyResponseDto ancestorDto2 = new HierarchyResponseDto();
        ancestorDto2.setId(ancestor2.getId());
        ancestorDto2.setName(ancestor2.getName());

        when(departmentMapper
                .toDtoList(
                        eq(Arrays.asList(ancestor1, ancestor2)),
                        anyBoolean()
                )
        )
                .thenReturn(Arrays.asList(ancestorDto1, ancestorDto2));

        // When: Retrieving ancestors
        List<HierarchyResponseDto> result = departmentService.getAncestors(departmentId);

        // Then: Verify ancestors are returned correctly
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(ancestorDto1.getId(), result.get(0).getId());
        assertEquals(ancestorDto1.getName(), result.get(0).getName());
        assertEquals(ancestorDto2.getId(), result.get(1).getId());
        assertEquals(ancestorDto2.getName(), result.get(1).getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(2L);
        verify(departmentRepository, times(1)).findById(3L);
        verify(departmentMapper, times(1)).toDtoList(anyList(), anyBoolean());
        verifySuccessFetch(mockQuery, 1);
    }

    /**
     * Test Case: Attempt to retrieve ancestors for a non-existing department.
     * <p>
     * Given: Non-existing department ID
     * - nonExistingId: -1
     */
    @Test
    void testGetAncestors_nonExistingDepartment() {
        Long nonExistingId = -1L;

        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Retrieving ancestors should throw EntityNotFoundException
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            departmentService.getAncestors(nonExistingId);
        });

        assertEquals("Entity not found with ID: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(anyLong());
        verify(departmentMapper, never()).toDto(any(Department.class));
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

        JPAQuery<HierarchyBaseEntityClosure<?>> mockQuery = mockSelectFromQueryListClosure(createMockAncestorsClosures());

        // When: Call the service method
        PaginatedResponseDto<HierarchyResponseDto> result = departmentService.getAncestors(entityId, pageable, true);

        // Then: Assertions
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals("Department 1", result.getContent().get(0).getName());
        assertEquals("Department 2", result.getContent().get(1).getName());
        assertEquals("Department 3", result.getContent().get(2).getName());

        // Then: Verify interactions
        verify(departmentRepository, times(3)).findById(anyLong());

        verify(departmentMapper).toPaginatedDtoList(anyList(), eq(pageable), anyLong(), eq(true));

        verifySuccessFetch(mockQuery, 1);
    }

    /**
     * Helper method to create mock ancestors.
     *
     * @return a list of mock ancestor entities.
     */
    private List<Department> createMockAncestors() {
        // Replace with your mock descendant creation logic
        Department Department1 = Department.builder().id(1L).name("Department 1").parentId(null).build();
        Department Department2 = Department.builder().id(2L).name("Department 2").parentId(null).build();
        Department Department3 = Department.builder().id(3L).name("Department 3").parentId(null).build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(Department1));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(Department2));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(Department3));

        return Arrays.asList(
                Department1,
                Department2,
                Department3
                // Add more descendants as needed
        );
    }

    /**
     * Helper method to create mock ancestor closures.
     *
     * @return a list of mock ancestor entities closures.
     */
    private List<DepartmentClosure> createMockAncestorsClosures() {
        // Replace with your mock descendant creation logic
        return Arrays.asList(
                DepartmentClosure.builder().id(1L)
                        .ancestorId(1L).descendantId(1L).level(0).build(),
                DepartmentClosure.builder().id(2L)
                        .ancestorId(2L).descendantId(2L).level(0).build(),
                DepartmentClosure.builder().id(3L)
                        .ancestorId(3L).descendantId(3L).level(0).build()
                // Add more descendants as needed
        );
    }
}