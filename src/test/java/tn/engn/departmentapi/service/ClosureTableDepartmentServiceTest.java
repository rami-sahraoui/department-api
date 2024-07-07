package tn.engn.departmentapi.service;

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
import tn.engn.departmentapi.model.DepartmentClosure;
import tn.engn.departmentapi.repository.DepartmentClosureRepository;
import tn.engn.departmentapi.repository.DepartmentRepository;

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
    private DepartmentMapper departmentMapper;

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
     * Unit test for creating a root department successfully.
     */
    @Test
    public void testCreateRootDepartment_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto for a root department.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Root Department");

        Department createdDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .build();

        DepartmentResponseDto responseDto = DepartmentResponseDto.builder()
                .id(1L)
                .name("Root Department")
                .build();

        when(departmentRepository.findById(anyLong())).thenReturn(Optional.of(createdDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(createdDepartment);

        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(createdDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        // When: Creating the root department.
        DepartmentResponseDto result = departmentService.createDepartment(requestDto);

        // Then: Verify the department is created successfully.
        assertNotNull(result);
        assertEquals("Root Department", result.getName());
        assertNull(result.getParentDepartmentId());

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
    public void testCreateChildDepartment_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto for a child department.
        Department parentDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .build();

        Department createdDepartment = Department.builder()
                .id(2L)
                .name("Child Department")
                .parentDepartmentId(parentDepartment.getId())
                .build();

        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Child Department");
        requestDto.setParentDepartmentId(parentDepartment.getId());

        DepartmentResponseDto responseDto = DepartmentResponseDto.builder()
                .id(2L)
                .name("Child Department")
                .parentDepartmentId(parentDepartment.getId())
                .build();

        DepartmentClosure departmentClosure = DepartmentClosure.builder()
                .ancestorId(parentDepartment.getId())
                .descendantId(parentDepartment.getId())
                .level(0)
                .build();

        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(createdDepartment.getId())).thenReturn(Optional.of(createdDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(createdDepartment);

        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(createdDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(responseDto);

        when(departmentClosureRepository.findByDescendantId(parentDepartment.getId())).thenReturn(List.of(departmentClosure));

        // When: Creating the child department.
        DepartmentResponseDto result = departmentService.createDepartment(requestDto);

        // Then: Verify the department is created successfully.
        assertNotNull(result);
        assertEquals("Child Department", result.getName());
        assertEquals(1L, result.getParentDepartmentId());

        verify(departmentRepository, times(1)).save(departmentArgumentCaptor.capture());
        Department capturedDepartment = departmentArgumentCaptor.getValue();

        assertNotNull(capturedDepartment);
        assertEquals("Child Department", capturedDepartment.getName());
        assertEquals(parentDepartment.getId(), capturedDepartment.getParentDepartmentId());

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
    }

    /**
     * Unit test for creating a real subtree successfully.
     */
    @Test
    public void testCreateRealSubtree_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto for a real subtree.
        String rootName="Root";
        Long rootId = 1L;
        DepartmentRequestDto rootRequestDto = DepartmentRequestDto.builder()
                .name(rootName)
                .build();

        Department rootDepartment = Department.builder()
                .id(rootId)
                .name(rootName)
                .build();

        DepartmentResponseDto rootResponseDto = DepartmentResponseDto.builder()
                .id(rootId)
                .name(rootName)
                .build();

        String anotherRootName="Another Root";
        Long anotherRootId = 2L;
        DepartmentRequestDto anotherRootRequestDto = DepartmentRequestDto.builder()
                .name(anotherRootName)
                .build();

        Department anotherRootDepartment = Department.builder()
                .id(anotherRootId)
                .name(anotherRootName)
                .build();

        DepartmentResponseDto anotherRootResponseDto = DepartmentResponseDto.builder()
                .id(anotherRootId)
                .name(anotherRootName)
                .build();

        String childName="Child";
        Long childId = 3L;
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name(childName)
                .parentDepartmentId(rootDepartment.getId())
                .build();

        Department childDepartment = Department.builder()
                .id(childId)
                .name(childName)
                .parentDepartmentId(rootDepartment.getId())
                .build();

        DepartmentResponseDto childResponseDto = DepartmentResponseDto.builder()
                .id(childId)
                .name(childName)
                .parentDepartmentId(rootDepartment.getId())
                .build();

        String grandChildName="Grand Child";
        Long grandChildId = 4L;
        DepartmentRequestDto grandChildRequestDto = DepartmentRequestDto.builder()
                .name(grandChildName)
                .parentDepartmentId(childDepartment.getId())
                .build();

        Department grandChildDepartment = Department.builder()
                .id(grandChildId)
                .name(grandChildName)
                .parentDepartmentId(childDepartment.getId())
                .build();

        DepartmentResponseDto grandChildResponseDto = DepartmentResponseDto.builder()
                .id(grandChildId)
                .name(grandChildName)
                .parentDepartmentId(childDepartment.getId())
                .build();

        DepartmentClosure selfDepartmentClosureForRoot = DepartmentClosure.builder()
                .ancestorId(rootDepartment.getId())
                .descendantId(rootDepartment.getId())
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

        when(departmentRepository.findById(rootDepartment.getId())).thenReturn(Optional.of(rootDepartment));
        when(departmentRepository.findById(childDepartment.getId())).thenReturn(Optional.of(childDepartment));
        when(departmentRepository.findById(grandChildDepartment.getId())).thenReturn(Optional.of(grandChildDepartment));
        when(departmentRepository.findById(anotherRootDepartment.getId())).thenReturn(Optional.of(anotherRootDepartment));

        when(departmentRepository.save(any(Department.class)))
                .thenReturn(rootDepartment)
                .thenReturn(childDepartment)
                .thenReturn(grandChildDepartment)
                .thenReturn(anotherRootDepartment);

        when(departmentMapper.toEntity(any(DepartmentRequestDto.class)))
                .thenReturn(rootDepartment)
                .thenReturn(childDepartment)
                .thenReturn(grandChildDepartment)
                .thenReturn(anotherRootDepartment);

        when(departmentMapper.toDto(any(Department.class)))
                .thenReturn(rootResponseDto)
                .thenReturn(childResponseDto)
                .thenReturn(grandChildResponseDto)
                .thenReturn(anotherRootResponseDto);

        when(departmentClosureRepository.findByDescendantId(rootDepartment.getId()))
                .thenReturn(List.of(selfDepartmentClosureForRoot));
        when(departmentClosureRepository.findByDescendantId(childDepartment.getId()))
                .thenReturn(
                        List.of(
                                selfDepartmentClosureForChild,
                                ancestorDepartmentClosureForChild
                        )
                );

        // When: Creating the departments.
        DepartmentResponseDto rootResult = departmentService.createDepartment(rootRequestDto);
        DepartmentResponseDto childResult = departmentService.createDepartment(childRequestDto);
        DepartmentResponseDto grandChildResult = departmentService.createDepartment(grandChildRequestDto);
        DepartmentResponseDto anotherRootResult = departmentService.createDepartment(anotherRootRequestDto);

        // Then: Verify the subtree is created successfully.
        assertNotNull(rootResult);
        assertEquals(rootName, rootResult.getName());
        assertNull(rootResult.getParentDepartmentId());

        assertNotNull(anotherRootResult);
        assertEquals(anotherRootName, anotherRootResult.getName());
        assertNull(anotherRootResult.getParentDepartmentId());

        assertNotNull(childResult);
        assertEquals(childName, childResult.getName());
        assertEquals(rootResult.getId(), childResult.getParentDepartmentId());

        assertNotNull(grandChildResult);
        assertEquals(grandChildName, grandChildResult.getName());
        assertEquals(childResult.getId(), grandChildResult.getParentDepartmentId());

        verify(departmentRepository, times(4)).save(departmentArgumentCaptor.capture());
        List<Department> capturedDepartments = departmentArgumentCaptor.getAllValues();

        Department capturedRootDepartment = capturedDepartments.get(0);
        assertEquals(rootName, capturedRootDepartment.getName());

        Department capturedChildDepartment = capturedDepartments.get(1);
        assertEquals(childName, capturedChildDepartment.getName());
        assertEquals(capturedRootDepartment.getId(), capturedChildDepartment.getParentDepartmentId());

        Department capturedGrandChildDepartment = capturedDepartments.get(2);
        assertEquals(grandChildName, capturedGrandChildDepartment.getName());
        assertEquals(capturedChildDepartment.getId(), capturedGrandChildDepartment.getParentDepartmentId());

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
    }

    /**
     * Unit test for creating a child department with empty parent closure.
     */
    @Test
    public void testCreateChildDepartment_EmptyParentClosure() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException {
        // Given: A valid DepartmentRequestDto for a child department.
        Department parentDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .build();

        Department createdDepartment = Department.builder()
                .id(2L)
                .name("Child Department")
                .parentDepartmentId(parentDepartment.getId())
                .build();

        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Child Department");
        requestDto.setParentDepartmentId(parentDepartment.getId());


        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(createdDepartment.getId())).thenReturn(Optional.of(createdDepartment));
        when(departmentRepository.save(createdDepartment)).thenReturn(createdDepartment);

        when(departmentMapper.toEntity(requestDto)).thenReturn(createdDepartment);

        when(departmentClosureRepository.findByDescendantId(parentDepartment.getId())).thenReturn(Collections.emptyList());

        // When: Creating the department.
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.createDepartment(requestDto);
        });

        // Then: Verify the ParentDepartmentNotFoundException is thrown.
        assertEquals("Parent department not found.", exception.getMessage());
        verify(departmentRepository, times(1)).save(any(Department.class));
        verify(departmentRepository, times(1)).delete(any(Department.class));
        verify(departmentMapper, never()).toDto(any());
        verify(departmentClosureRepository, never()).saveAll(anyList());
    }

    /**
     * Unit test for creating a department with an invalid name.
     */
    @Test
    public void testCreateDepartment_InvalidName() {
        // Given: A DepartmentRequestDto with an invalid name.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("");

        // When: Creating the department.
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            departmentService.createDepartment(requestDto);
        });

        // Then: Verify the ValidationException is thrown.
        assertEquals("Department name cannot be null or empty.", exception.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
        verify(departmentClosureRepository, never()).saveAll(anyList());
    }

    /**
     * Unit test for creating a department with a parent not found.
     */
    @Test
    public void testCreateDepartment_ParentNotFound() {
        // Given: A DepartmentRequestDto with a non-existent parent ID.
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName("Child Department");
        requestDto.setParentDepartmentId(-1L);

        when(departmentRepository.findById(-1L)).thenReturn(Optional.empty());

        // When: Creating the department.
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.createDepartment(requestDto);
        });

        // Then: Verify the ParentDepartmentNotFoundException is thrown.
        assertEquals("Parent department not found.", exception.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
        verify(departmentClosureRepository, never()).saveAll(anyList());
    }

    /**
     * Unit test for creating a department with a name exceeding max length.
     */
    @Test
    public void testCreateDepartment_NameTooLong() {
        // Given: A DepartmentRequestDto with a name exceeding max length.
        String longName = "ThisIsAVeryLongDepartmentNameThatExceedsMaxCharactersLimit";
        DepartmentRequestDto requestDto = new DepartmentRequestDto();
        requestDto.setName(longName);

        // When: Creating the department.
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            departmentService.createDepartment(requestDto);
        });

        // Then: Verify the ValidationException is thrown.
        assertEquals("Department name cannot be longer than " + maxNameLength + " characters.", exception.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
        verify(departmentClosureRepository, never()).saveAll(anyList());
    }

    /**
     * Unit test for updating a department successfully.
     */
    @Test
    public void testUpdateDepartment_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException, DepartmentNotFoundException {
        // Given: A valid DepartmentRequestDto for an existing department.
        Long departmentId = 1L;
        String updatedName = "Updated Department";

        DepartmentRequestDto updateRequestDto = DepartmentRequestDto.builder()
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

        DepartmentResponseDto updatedResponseDto = DepartmentResponseDto.builder()
                .id(departmentId)
                .name(updatedName)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);
        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(updatedDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(updatedResponseDto);

        // When: Updating the department.
        DepartmentResponseDto result = departmentService.updateDepartment(departmentId, updateRequestDto);

        // Then: Verify the department is updated successfully.
        assertNotNull(result);
        assertEquals(updatedName, result.getName());
        assertNull(result.getParentDepartmentId());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(departmentArgumentCaptor.capture());

        Department capturedDepartment = departmentArgumentCaptor.getValue();
        assertEquals(updatedName, capturedDepartment.getName());
    }

    /**
     * Unit test for updating a department to root successfully.
     */
    @Test
    public void testUpdateDepartment_ToRoot_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException, DepartmentNotFoundException {
        // Given: A valid DepartmentRequestDto for an existing department to be updated to root.
        Long departmentId = 1L;
        String updatedName = "Updated Root Department";

        DepartmentRequestDto updateRequestDto = DepartmentRequestDto.builder()
                .id(departmentId)
                .name(updatedName)
                .parentDepartmentId(null)
                .build();

        Department existingDepartment = Department.builder()
                .id(departmentId)
                .name("Original Department")
                .parentDepartmentId(2L)
                .build();

        Department updatedDepartment = Department.builder()
                .id(departmentId)
                .name(updatedName)
                .build();

        DepartmentResponseDto updatedResponseDto = DepartmentResponseDto.builder()
                .id(departmentId)
                .name(updatedName)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(updatedDepartment);
        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(updatedDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(updatedResponseDto);

        // When: Updating the department to root.
        DepartmentResponseDto result = departmentService.updateDepartment(departmentId, updateRequestDto);

        // Then: Verify the department is updated successfully to root.
        assertNotNull(result);
        assertEquals(updatedName, result.getName());
        assertNull(result.getParentDepartmentId());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).save(departmentArgumentCaptor.capture());

        Department capturedDepartment = departmentArgumentCaptor.getValue();
        assertEquals(updatedName, capturedDepartment.getName());
        assertNull(capturedDepartment.getParentDepartmentId());
    }

    /**
     * Unit test for updating a department with an invalid name.
     */
    @Test
    public void testUpdateDepartment_InvalidName() {
        // Given: An invalid DepartmentRequestDto with a null name.
        Long departmentId = 1L;

        DepartmentRequestDto updateRequestDto = DepartmentRequestDto.builder()
                .id(departmentId)
                .name(null)
                .build();

        // When & Then: Updating the department should throw ValidationException.
        assertThrows(ValidationException.class, () -> {
            departmentService.updateDepartment(departmentId, updateRequestDto);
        });

        verify(departmentRepository, never()).findById(departmentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for updating a department when parent department is not found.
     */
    @Test
    public void testUpdateDepartment_ParentNotFound() {
        // Given: A DepartmentRequestDto with a non-existent parent department ID.
        Long departmentId = 1L;
        Long nonExistentParentId = -1L;

        DepartmentRequestDto updateRequestDto = DepartmentRequestDto.builder()
                .id(departmentId)
                .name("Updated Department")
                .parentDepartmentId(nonExistentParentId)
                .build();

        Department existingDepartment = Department.builder()
                .id(departmentId)
                .name("Original Department")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.findById(nonExistentParentId)).thenReturn(Optional.empty());

        // When & Then: Updating the department should throw ParentDepartmentNotFoundException.
        assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.updateDepartment(departmentId, updateRequestDto);
        });

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(nonExistentParentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for updating a real subtree successfully.
     */
    @Test
    public void testUpdateDepartmentRealSubtree_Success() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException, DepartmentNotFoundException {
        // Given: A valid DepartmentRequestDto for a real subtree.
        String rootName="Root";
        Long rootId = 1L;
        DepartmentRequestDto rootRequestDto = DepartmentRequestDto.builder()
                .name(rootName)
                .build();

        Department rootDepartment = Department.builder()
                .id(rootId)
                .name(rootName)
                .build();

        DepartmentResponseDto rootResponseDto = DepartmentResponseDto.builder()
                .id(rootId)
                .name(rootName)
                .build();

        String anotherRootName="Another Root";
        Long anotherRootId = 2L;
        DepartmentRequestDto anotherRootRequestDto = DepartmentRequestDto.builder()
                .name(anotherRootName)
                .build();

        Department anotherRootDepartment = Department.builder()
                .id(anotherRootId)
                .name(anotherRootName)
                .build();

        DepartmentResponseDto anotherRootResponseDto = DepartmentResponseDto.builder()
                .id(anotherRootId)
                .name(anotherRootName)
                .build();

        String childName="Child";
        Long childId = 3L;
        DepartmentRequestDto childRequestDto = DepartmentRequestDto.builder()
                .name(childName)
                .parentDepartmentId(rootDepartment.getId())
                .build();

        DepartmentRequestDto updateChildRequestDto = DepartmentRequestDto.builder()
                .id(childId)
                .name(childName)
                .parentDepartmentId(anotherRootResponseDto.getId())
                .build();

        Department childDepartment = Department.builder()
                .id(childId)
                .name(childName)
                .parentDepartmentId(rootDepartment.getId())
                .build();

        Department updatedChildDepartment = Department.builder()
                .id(childId)
                .name(childName)
                .parentDepartmentId(anotherRootDepartment.getId())
                .build();

        DepartmentResponseDto childResponseDto = DepartmentResponseDto.builder()
                .id(childId)
                .name(childName)
                .parentDepartmentId(rootDepartment.getId())
                .build();

        DepartmentResponseDto updateChildResponseDto = DepartmentResponseDto.builder()
                .id(childId)
                .name(childName)
                .parentDepartmentId(anotherRootDepartment.getId())
                .build();

        String grandChildName="Grand Child";
        Long grandChildId = 4L;
        DepartmentRequestDto grandChildRequestDto = DepartmentRequestDto.builder()
                .name(grandChildName)
                .parentDepartmentId(childDepartment.getId())
                .build();

        Department grandChildDepartment = Department.builder()
                .id(grandChildId)
                .name(grandChildName)
                .parentDepartmentId(childDepartment.getId())
                .build();

        DepartmentResponseDto grandChildResponseDto = DepartmentResponseDto.builder()
                .id(grandChildId)
                .name(grandChildName)
                .parentDepartmentId(childDepartment.getId())
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

        DepartmentClosure selfDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(grandChildDepartment.getId())
                .descendantId(grandChildDepartment.getId())
                .level(0)
                .build();

        DepartmentClosure ancestorDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(rootDepartment.getId())
                .descendantId(childDepartment.getId())
                .level(1)
                .build();

        DepartmentClosure updatedAncestorDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(anotherRootDepartment.getId())
                .descendantId(childDepartment.getId())
                .level(1)
                .build();

        DepartmentClosure ancestorDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(childDepartment.getId())
                .descendantId(grandChildDepartment.getId())
                .level(1)
                .build();

        DepartmentClosure updatedGrandAncestorDepartmentClosureForGrandChild = DepartmentClosure.builder()
                .ancestorId(anotherRootDepartment.getId())
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
                .thenReturn(anotherRootDepartment)
                .thenReturn(updatedChildDepartment);

        when(departmentMapper.toEntity(any(DepartmentRequestDto.class)))
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

        when(departmentClosureRepository.findByDescendantId(rootDepartment.getId()))
                .thenReturn(List.of(selfDepartmentClosureForRoot));
        when(departmentClosureRepository.findByDescendantId(childDepartment.getId()))
                .thenReturn(
                        List.of(
                                selfDepartmentClosureForChild,
                                ancestorDepartmentClosureForChild
                        )
                );
        when(departmentClosureRepository.findByAncestorId(childDepartment.getId()))
                .thenReturn(
                        List.of(
                                selfDepartmentClosureForChild,
                                ancestorDepartmentClosureForGrandChild
                        )
                );
        when(departmentClosureRepository.findByDescendantId(anotherRootDepartment.getId()))
                .thenReturn(List.of(selfDepartmentClosureForAnotherRoot));

        // When: Creating the departments and updating child department.
        DepartmentResponseDto rootResult = departmentService.createDepartment(rootRequestDto);
        DepartmentResponseDto childResult = departmentService.createDepartment(childRequestDto);
        DepartmentResponseDto grandChildResult = departmentService.createDepartment(grandChildRequestDto);
        DepartmentResponseDto anotherRootResult = departmentService.createDepartment(anotherRootRequestDto);

        // Then: Verify the subtree is created successfully.
        assertNotNull(rootResult);
        assertEquals(rootName, rootResult.getName());
        assertNull(rootResult.getParentDepartmentId());

        assertNotNull(anotherRootResult);
        assertEquals(anotherRootName, anotherRootResult.getName());
        assertNull(anotherRootResult.getParentDepartmentId());

        assertNotNull(childResult);
        assertEquals(childName, childResult.getName());
        assertEquals(rootResult.getId(), childResult.getParentDepartmentId());

        assertNotNull(grandChildResult);
        assertEquals(grandChildName, grandChildResult.getName());
        assertEquals(childResult.getId(), grandChildResult.getParentDepartmentId());

        verify(departmentRepository, times(4)).save(departmentArgumentCaptor.capture());
        List<Department> capturedDepartments = departmentArgumentCaptor.getAllValues();

        Department capturedRootDepartment = capturedDepartments.get(0);
        assertEquals(rootName, capturedRootDepartment.getName());

        Department capturedChildDepartment = capturedDepartments.get(1);
        assertEquals(childName, capturedChildDepartment.getName());
        assertEquals(capturedRootDepartment.getId(), capturedChildDepartment.getParentDepartmentId());

        Department capturedGrandChildDepartment = capturedDepartments.get(2);
        assertEquals(grandChildName, capturedGrandChildDepartment.getName());
        assertEquals(capturedChildDepartment.getId(), capturedGrandChildDepartment.getParentDepartmentId());

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
        DepartmentResponseDto updatedChildResult = departmentService.updateDepartment(childResult.getId(), updateChildRequestDto);

        assertNotNull(updatedChildResult);
        assertEquals(childName, updatedChildResult.getName());
        assertEquals(anotherRootResult.getId(), updatedChildResult.getParentDepartmentId());

        verify(departmentRepository, times(5)).save(departmentArgumentCaptor.capture());
        Department capturedDepartment = departmentArgumentCaptor.getAllValues().get(8);

        assertNotNull(capturedDepartment);
        assertEquals(childName, capturedDepartment.getName());
        assertEquals(anotherRootResult.getId(), capturedDepartment.getParentDepartmentId());

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
    }

    /**
     * Unit test for updating a department not found.
     */
    @Test
    public void testUpdateDepartment_NotFound() {
        // Given: A DepartmentRequestDto with a non-existent department ID.
        Long departmentId = -1L;

        DepartmentRequestDto updateRequestDto = DepartmentRequestDto.builder()
                .id(departmentId)
                .name("Non-Existent Department")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        // When & Then: Updating the department should throw DepartmentNotFoundException.
        assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.updateDepartment(departmentId, updateRequestDto);
        });

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).save(any(Department.class));
    }

    /**
     * Unit test for updating a department without any modification.
     */
    @Test
    public void testUpdateDepartment_NoModification() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException, DepartmentNotFoundException {
        // Given: A valid DepartmentRequestDto for an existing department.
        Long departmentId = 1L;

        String name = "Original Department";

        DepartmentRequestDto updateRequestDto = DepartmentRequestDto.builder()
                .id(departmentId)
                .name(name)
                .build();

        Department existingDepartment = Department.builder()
                .id(departmentId)
                .name(name)
                .build();

        DepartmentResponseDto updatedResponseDto = DepartmentResponseDto.builder()
                .id(departmentId)
                .name(name)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        when(departmentRepository.save(any(Department.class))).thenReturn(existingDepartment);
        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(existingDepartment);
        when(departmentMapper.toDto(any(Department.class))).thenReturn(updatedResponseDto);

        // When: Updating the department.
        DepartmentResponseDto result = departmentService.updateDepartment(departmentId, updateRequestDto);

        // Then: Verify the department is updated successfully.
        assertNotNull(result);
        assertEquals(name, result.getName());
        assertNull(result.getParentDepartmentId());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, never()).save(any());
    }

    /**
     * Unit test for updating a department with empty parent closure.
     */
    @Test
    public void testUpdateDepartment_EmptyParentClosure() throws ParentDepartmentNotFoundException, ValidationException, DataIntegrityException, DepartmentNotFoundException {
        // Given: A valid DepartmentRequestDto for a child department.
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

        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .id(childId)
                .name(childName)
                .parentDepartmentId(parentDepartment.getId())
                .build();

        DepartmentClosure departmentClosure = DepartmentClosure.builder()
                .ancestorId(childId)
                .descendantId(childId)
                .level(0)
                .build();


        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(createdDepartment.getId())).thenReturn(Optional.of(createdDepartment));

        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(createdDepartment);

        when(departmentClosureRepository.findByAncestorId(createdDepartment.getId())).thenReturn(List.of(departmentClosure));
        when(departmentClosureRepository.findByDescendantId(parentDepartment.getId())).thenReturn(Collections.emptyList());

        // When & Then: Updating the department should throw ParentDepartmentNotFoundException.
        assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.updateDepartment(childId, requestDto);
        });

        verify(departmentRepository, times(1)).findById(childId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentRepository, never()).save(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Unit test for updating a department with circular dependency.
     */
    @Test
    void testUpdateDepartmentHasCircularDependency() {
        // Given: An invalid DepartmentRequestDto for a parent department.
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
                .parentDepartmentId(parentDepartment.getId())
                .build();

        DepartmentRequestDto requestDto = DepartmentRequestDto.builder()
                .id(parentId)
                .name("Root Department")
                .parentDepartmentId(childDepartment.getId())
                .build();

        DepartmentClosure selfDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(childId)
                .descendantId(childId)
                .level(0)
                .build();

        DepartmentClosure selfDepartmentClosureForParent = DepartmentClosure.builder()
                .ancestorId(parentId)
                .descendantId(parentId)
                .level(0)
                .build();

        DepartmentClosure ancestorDepartmentClosureForChild = DepartmentClosure.builder()
                .ancestorId(parentId)
                .descendantId(childId)
                .level(1)
                .build();


        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findById(childDepartment.getId())).thenReturn(Optional.of(childDepartment));

        when(departmentMapper.toEntity(any(DepartmentRequestDto.class))).thenReturn(parentDepartment);

        when(departmentClosureRepository.findByAncestorId(childDepartment.getId())).thenReturn(List.of(selfDepartmentClosureForChild));
        when(departmentClosureRepository.findByDescendantId(childDepartment.getId())).thenReturn(
                List.of(
                        selfDepartmentClosureForChild,
                        ancestorDepartmentClosureForChild
                )
        );

        when(departmentClosureRepository.findByAncestorId(parentDepartment.getId())).thenReturn(List.of(selfDepartmentClosureForParent));
        when(departmentClosureRepository.findByDescendantId(parentDepartment.getId())).thenReturn(List.of(selfDepartmentClosureForParent));

        // When & Then: Updating the department should throw DataIntegrityException.
        assertThrows(DataIntegrityException.class, () -> {
            departmentService.updateDepartment(parentId, requestDto);
        });

        verify(departmentRepository, times(1)).findById(childId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentRepository, never()).save(any());
        verify(departmentMapper, never()).toDto(any());
    }

    /**
     * Unit test for deleting a department with non-existing id.
     */
    @Test
    public void testDeleteDepartment_NotFound() {
        // Given: A DepartmentRequestDto with a non-existent department ID.
        Long nonExistingId = -1L;

        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Deleting the department.
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.deleteDepartment(nonExistingId);
        });

        // Then: Verify it should throw DepartmentNotFoundException.

        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(departmentClosureRepository, never()).delete(any(DepartmentClosure.class));
        verify(departmentRepository, never()).delete(any(Department.class));
    }

    /**
     * Unit test for deleting a department with  circular reference.
     */
    @Test
    public void testDeleteDepartment_CircularReference() throws DepartmentNotFoundException, DataIntegrityException {
        // Given: A departments with circular references.
        Department parentDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .parentDepartmentId(2L)
                .build();

        Department childDepartment = Department.builder()
                .id(2L)
                .name("Child Department")
                .parentDepartmentId(1L)
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

        when(departmentClosureRepository.findByDescendantId(childDepartment.getId()))
                .thenReturn(
                        Arrays.asList(
                                selfDepartmentClosureForChild,
                                ancestorDepartmentClosureForChild
                        )
                );

        // When: Deleting the parent department
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteDepartment((parentDepartment.getId()));
        });

        // Then: Verify it should throw DataIntegrityException due to circular reference
        assertEquals("Circular dependency detected: Department cannot be its own ancestor.", exception.getMessage());

        verify(departmentRepository, times(1)).findById(parentDepartment.getId());
        verify(departmentClosureRepository, never()).deleteByAncestorId(parentDepartment.getId());
        verify(departmentRepository, never()).delete(parentDepartment);

    }

    /**
     * Unit test for deleting a department with  circular reference.
     */
    @Test
    public void testDeleteDepartment_SelfCircularReference() throws DepartmentNotFoundException, DataIntegrityException {
        // Given: A departments with circular references.
        Department parentDepartment = Department.builder()
                .id(1L)
                .name("Root Department")
                .parentDepartmentId(1L)
                .build();


        DepartmentClosure selfDepartmentClosureForParent = DepartmentClosure.builder()
                .ancestorId(parentDepartment.getId())
                .descendantId(parentDepartment.getId())
                .level(0)
                .build();


        when(departmentRepository.findById(parentDepartment.getId())).thenReturn(Optional.of(parentDepartment));

        when(departmentClosureRepository.findByDescendantId(parentDepartment.getId()))
                .thenReturn(
                        Arrays.asList(
                                selfDepartmentClosureForParent
                        )
                );

        // When: Deleting the parent department
        DataIntegrityException exception = assertThrows(DataIntegrityException.class, () -> {
            departmentService.deleteDepartment((parentDepartment.getId()));
        });

        // Then: Verify it should throw DataIntegrityException due to circular reference
        assertEquals("Circular dependency detected: Department cannot be its own ancestor.", exception.getMessage());

        verify(departmentRepository, times(1)).findById(parentDepartment.getId());
        verify(departmentClosureRepository, never()).deleteByAncestorId(parentDepartment.getId());
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
                .parentDepartmentId(parentDepartment.getId())
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

        when(departmentClosureRepository.findByDescendantId(parentDepartment.getId())).thenReturn(Arrays.asList(selfDepartmentClosureForParent));
        when(departmentClosureRepository.findByDescendantId(childDepartment.getId())).thenReturn(Arrays.asList(selfDepartmentClosureForChild, ancestorDepartmentClosureForChild));
        when(departmentClosureRepository.findByAncestorId(parentDepartment.getId())).thenReturn(Arrays.asList(selfDepartmentClosureForParent, ancestorDepartmentClosureForChild));
        when(departmentClosureRepository.findByAncestorId(childDepartment.getId())).thenReturn(Arrays.asList(selfDepartmentClosureForChild));

        // When: Deleting the department
        departmentService.deleteDepartment(1L);

        // Then: Verify that delete methods are called appropriately
        verify(departmentRepository, times(1)).findById(parentDepartment.getId());
        verify(departmentClosureRepository, times(1)).deleteByAncestorId(parentDepartment.getId());
        verify(departmentRepository, times(1)).delete(parentDepartment);
        verify(departmentRepository, times(1)).findById(childDepartment.getId());
        verify(departmentClosureRepository, times(1)).deleteByAncestorId(childDepartment.getId());
        verify(departmentRepository, times(1)).delete(childDepartment);
    }

    // DepartmentServiceTest.java
    /**
     * Test to verify getAllDepartments() when there are no departments in the repository.
     */
    @Test
    public void testGetAllDepartments_noDepartments() {
        // Given: No departments in the repository
        when(departmentRepository.findAll()).thenReturn(Collections.emptyList());

        // When: Retrieving all departments
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Then: Verify the result is an empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(departmentRepository, times(1)).findAll();
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test to verify getAllDepartments() when there are departments in the repository.
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

        DepartmentResponseDto responseDto1 = DepartmentResponseDto.builder()
                .id(1L)
                .name("Department 1")
                .build();

        DepartmentResponseDto responseDto2 = DepartmentResponseDto.builder()
                .id(2L)
                .name("Department 2")
                .build();

        when(departmentMapper.toDto(department1)).thenReturn(responseDto1);
        when(departmentMapper.toDto(department2)).thenReturn(responseDto2);

        // When: Retrieving all departments
        List<DepartmentResponseDto> result = departmentService.getAllDepartments();

        // Then: Verify the result contains the expected departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(responseDto1));
        assertTrue(result.contains(responseDto2));

        verify(departmentRepository, times(1)).findAll();
        verify(departmentMapper, times(1)).toDto(department1);
        verify(departmentMapper, times(1)).toDto(department2);
    }

    /**
     * Test to verify getSubDepartments() when the parent department exists.
     */
    @Test
    public void testGetSubDepartments_existingParent() throws ParentDepartmentNotFoundException {
        // Given: An existing parent department and its sub-departments
        Long parentId = 1L;
        Department parentDepartment = Department.builder()
                .id(parentId)
                .name("Parent Department")
                .build();

        Department subDepartment1 = Department.builder()
                .id(2L)
                .name("Sub Department 1")
                .parentDepartmentId(parentId)
                .build();

        Department subDepartment2 = Department.builder()
                .id(3L)
                .name("Sub Department 2")
                .parentDepartmentId(parentId)
                .build();

        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));
        when(departmentRepository.findByParentDepartmentId(parentId)).thenReturn(Arrays.asList(subDepartment1, subDepartment2));

        DepartmentResponseDto responseDto1 = DepartmentResponseDto.builder()
                .id(2L)
                .name("Sub Department 1")
                .build();

        DepartmentResponseDto responseDto2 = DepartmentResponseDto.builder()
                .id(3L)
                .name("Sub Department 2")
                .build();

        when(departmentMapper.toDto(subDepartment1)).thenReturn(responseDto1);
        when(departmentMapper.toDto(subDepartment2)).thenReturn(responseDto2);

        // When: Retrieving sub-departments of the parent department
        List<DepartmentResponseDto> result = departmentService.getSubDepartments(parentId);

        // Then: Verify the result contains the expected sub-departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(responseDto1));
        assertTrue(result.contains(responseDto2));

        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentRepository, times(1)).findByParentDepartmentId(parentId);
        verify(departmentMapper, times(1)).toDto(subDepartment1);
        verify(departmentMapper, times(1)).toDto(subDepartment2);
    }

    /**
     * Test to verify getSubDepartments() when the parent department does not exist.
     */
    @Test
    public void testGetSubDepartments_nonExistingParent() {
        // Given: A non-existent parent department ID
        Long nonExistingParentId = -1L;
        when(departmentRepository.findById(nonExistingParentId)).thenReturn(Optional.empty());

        // When & Then: Retrieving sub-departments should throw ParentDepartmentNotFoundException
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.getSubDepartments(nonExistingParentId);
        });

        // Then: Verify it should throw ParentDepartmentNotFoundException
        assertEquals("Parent department not found with id: " + nonExistingParentId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingParentId);
        verify(departmentRepository, never()).findByParentDepartmentId(anyLong());
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test to verify getDepartmentById() when the department exists.
     */
    @Test
    public void testGetDepartmentById_ExistingDepartment() throws DepartmentNotFoundException {
        // Given: An existing department
        Long departmentId = 1L;
        Department department = Department.builder()
                .id(departmentId)
                .name("Engineering")
                .build();

        DepartmentResponseDto responseDto = DepartmentResponseDto.builder()
                .id(departmentId)
                .name("Engineering")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentMapper.toDto(department)).thenReturn(responseDto);

        // When: Retrieving the department by ID
        DepartmentResponseDto result = departmentService.getDepartmentById(departmentId);

        // Then: Verify the result matches the expected department
        assertNotNull(result);
        assertEquals(departmentId, result.getId());
        assertEquals("Engineering", result.getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentMapper, times(1)).toDto(department);
    }

    /**
     * Test to verify getDepartmentById() when the department does not exist.
     */
    @Test
    public void testGetDepartmentById_NonExistingDepartment() {
        // Given: A non-existent department ID
        Long nonExistingId = -1L;
        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Retrieving the department should throw DepartmentNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getDepartmentById(nonExistingId);
        });

        // Then: Verify it should throw DepartmentNotFoundException
        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(nonExistingId);
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test to verify searchDepartmentsByName() when departments with the search name exist.
     */
    @Test
    public void testSearchDepartmentsByName_ExistingName() {
        // Given: Existing departments with matching names
        String searchName = "Engineering";
        List<Department> departments = Arrays.asList(
                Department.builder().id(1L).name("Software Engineering").build(),
                Department.builder().id(2L).name("Engineering Team").build()
        );

        List<DepartmentResponseDto> responseDtos = Arrays.asList(
                DepartmentResponseDto.builder().id(1L).name("Software Engineering").build(),
                DepartmentResponseDto.builder().id(2L).name("Engineering Team").build()
        );

        when(departmentRepository.findByNameContainingIgnoreCase(searchName)).thenReturn(departments);
        when(departmentMapper.toDto(any(Department.class))).thenAnswer(invocation -> {
            Department department = invocation.getArgument(0);
            return responseDtos.stream()
                    .filter(dto -> dto.getId().equals(department.getId()))
                    .findFirst()
                    .orElse(null);
        });

        // When: Searching departments by name
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName(searchName);

        // Then: Verify the result matches the expected departments
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Software Engineering", result.get(0).getName());
        assertEquals("Engineering Team", result.get(1).getName());

        verify(departmentRepository, times(1)).findByNameContainingIgnoreCase(searchName);
        verify(departmentMapper, times(2)).toDto(any(Department.class));
    }

    /**
     * Test to verify searchDepartmentsByName() when no departments with the search name exist.
     */
    @Test
    public void testSearchDepartmentsByName_NonExistingName() {
        // Given: No departments with the search name
        String searchName = "Marketing";

        when(departmentRepository.findByNameContainingIgnoreCase(searchName)).thenReturn(Collections.emptyList());

        // When: Searching departments by name
        List<DepartmentResponseDto> result = departmentService.searchDepartmentsByName(searchName);

        // Then: Verify the result is an empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(departmentRepository, times(1)).findByNameContainingIgnoreCase(searchName);
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test Case: Retrieve existing parent department for a given department.
     *
     * Given: Existing parent department
     * - departmentId: 1
     * - parentId: 2
     * - childDepartment: Department with parentDepartmentId set to parentId
     * - parentDepartment: Department with id parentId
     */
    @Test
    public void testGetParentDepartment_existingParent() {
        Long departmentId = 1L;
        Long parentId = 2L;
        Department childDepartment = Department.builder()
                .id(departmentId)
                .parentDepartmentId(parentId)
                .build();
        Department parentDepartment = Department.builder()
                .id(parentId)
                .name("Parent Department")
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(childDepartment));
        when(departmentRepository.findById(parentId)).thenReturn(Optional.of(parentDepartment));

        DepartmentResponseDto parentDto = DepartmentResponseDto.builder()
                .id(parentDepartment.getId())
                .name(parentDepartment.getName())
                .build();

        when(departmentMapper.toDto(parentDepartment)).thenReturn(parentDto);

        // When: Retrieving parent department
        DepartmentResponseDto result = departmentService.getParentDepartment(departmentId);

        // Then: Verify the parent department is returned
        assertNotNull(result);
        assertEquals(parentDto.getId(), result.getId());
        assertEquals(parentDto.getName(), result.getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentRepository, times(1)).findById(parentId);
        verify(departmentMapper, times(1)).toDto(parentDepartment);
    }

    /**
     * Test Case: Attempt to retrieve parent department for a non-existing department.
     *
     * Given: Non-existing department ID
     * - nonExistingId: -1
     */
    @Test
    public void testGetParentDepartment_nonExistingDepartment() {
        Long nonExistingId = -1L;

        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Retrieving parent department should throw DepartmentNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getParentDepartment(nonExistingId);
        });

        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(anyLong());
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test Case: Attempt to retrieve parent department for a department with no parent.
     *
     * Given: Existing department with no parent
     * - nonExistingParentId: -1
     * - departmentId: 1
     * - department: Department with parentDepartmentId set to nonExistingParentId
     */
    @Test
    public void testGetParentDepartment_noParent() {
        Long departmentId = 1L;
        Department department = Department.builder()
                .id(departmentId)
                .build();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        // When & Then: Retrieving parent department should throw ParentDepartmentNotFoundException
        ParentDepartmentNotFoundException exception = assertThrows(ParentDepartmentNotFoundException.class, () -> {
            departmentService.getParentDepartment(departmentId);
        });

        assertEquals("Department has no parent.", exception.getMessage());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test Case: Retrieve descendants for an existing department.
     *
     * Given: Existing department with descendants
     * - departmentId: 1
     * - department: Root Department
     * - child1: Child Department 1 with parentDepartmentId set to departmentId
     * - child2: Child Department 2 with parentDepartmentId set to departmentId
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
                .parentDepartmentId(departmentId)
                .build();

        Department child2 = Department.builder()
                .id(3L)
                .name("Child Department 2")
                .parentDepartmentId(departmentId)
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
        when(departmentClosureRepository.findByAncestorId(departmentId)).thenReturn(departmentClosures);
        when(departmentRepository.findById(child1.getId())).thenReturn(Optional.of(child1));
        when(departmentRepository.findById(child2.getId())).thenReturn(Optional.of(child2));

        DepartmentResponseDto child1Dto = DepartmentResponseDto.builder()
                .id(child1.getId())
                .name(child1.getName())
                .build();

        DepartmentResponseDto child2Dto = DepartmentResponseDto.builder()
                .id(child2.getId())
                .name(child2.getName())
                .build();

        when(departmentMapper.toDto(child1)).thenReturn(child1Dto);
        when(departmentMapper.toDto(child2)).thenReturn(child2Dto);

        // When: Retrieving descendants
        List<DepartmentResponseDto> result = departmentService.getDescendants(departmentId);

        // Then: Verify descendants are returned correctly
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(child1Dto.getId(), result.get(0).getId());
        assertEquals(child1Dto.getName(), result.get(0).getName());
        assertEquals(child2Dto.getId(), result.get(1).getId());
        assertEquals(child2Dto.getName(), result.get(1).getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentClosureRepository, times(1)).findByAncestorId(departmentId);
        verify(departmentRepository, times(1)).findById(child1.getId());
        verify(departmentRepository, times(1)).findById(child2.getId());
        verify(departmentMapper, times(1)).toDto(child1);
        verify(departmentMapper, times(1)).toDto(child2);
    }

    /**
     * Test Case: Attempt to retrieve descendants for a non-existing department.
     *
     * Given: Non-existing department ID
     * - nonExistingId: -1
     */
    @Test
    public void testGetDescendants_nonExistingDepartment() {
        Long nonExistingId = -1L;

        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Retrieving descendants should throw DepartmentNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getDescendants(nonExistingId);
        });

        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(anyLong());
        verify(departmentClosureRepository, never()).findByAncestorId(anyLong());
        verify(departmentMapper, never()).toDto(any(Department.class));
    }

    /**
     * Test Case: Retrieve ancestors for an existing department.
     *
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
        when(departmentClosureRepository.findByDescendantId(departmentId)).thenReturn(closureList);

        Department ancestor1 = new Department();
        ancestor1.setId(2L);
        ancestor1.setName("Ancestor 1");

        Department ancestor2 = new Department();
        ancestor2.setId(3L);
        ancestor2.setName("Ancestor 2");

        when(departmentRepository.findById(2L)).thenReturn(Optional.of(ancestor1));
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(ancestor2));

        DepartmentResponseDto ancestorDto1 = new DepartmentResponseDto();
        ancestorDto1.setId(ancestor1.getId());
        ancestorDto1.setName(ancestor1.getName());

        DepartmentResponseDto ancestorDto2 = new DepartmentResponseDto();
        ancestorDto2.setId(ancestor2.getId());
        ancestorDto2.setName(ancestor2.getName());

        when(departmentMapper.toDto(ancestor1)).thenReturn(ancestorDto1);
        when(departmentMapper.toDto(ancestor2)).thenReturn(ancestorDto2);

        // When: Retrieving ancestors
        List<DepartmentResponseDto> result = departmentService.getAncestors(departmentId);

        // Then: Verify ancestors are returned correctly
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(ancestorDto1.getId(), result.get(0).getId());
        assertEquals(ancestorDto1.getName(), result.get(0).getName());
        assertEquals(ancestorDto2.getId(), result.get(1).getId());
        assertEquals(ancestorDto2.getName(), result.get(1).getName());

        verify(departmentRepository, times(1)).findById(departmentId);
        verify(departmentClosureRepository, times(1)).findByDescendantId(departmentId);
        verify(departmentRepository, times(1)).findById(2L);
        verify(departmentRepository, times(1)).findById(3L);
        verify(departmentMapper, times(1)).toDto(ancestor1);
        verify(departmentMapper, times(1)).toDto(ancestor2);
    }

    /**
     * Test Case: Attempt to retrieve ancestors for a non-existing department.
     *
     * Given: Non-existing department ID
     * - nonExistingId: -1
     */
    @Test
    void testGetAncestors_nonExistingDepartment() {
        Long nonExistingId = -1L;

        when(departmentRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then: Retrieving ancestors should throw DepartmentNotFoundException
        DepartmentNotFoundException exception = assertThrows(DepartmentNotFoundException.class, () -> {
            departmentService.getAncestors(nonExistingId);
        });

        assertEquals("Department not found with id: " + nonExistingId, exception.getMessage());

        verify(departmentRepository, times(1)).findById(anyLong());
        verify(departmentClosureRepository, never()).findByDescendantId(anyLong());
        verify(departmentMapper, never()).toDto(any(Department.class));
    }
}