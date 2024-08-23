package tn.engn.assignmentapi.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import tn.engn.assignmentapi.dto.*;
import tn.engn.assignmentapi.exception.AssignmentAlreadyExistsException;
import tn.engn.assignmentapi.exception.AssignmentNotFoundException;
import tn.engn.assignmentapi.exception.MetadataNotFoundException;
import tn.engn.assignmentapi.mapper.AssignableEntityMapper;
import tn.engn.assignmentapi.mapper.AssignmentMapper;
import tn.engn.assignmentapi.mapper.AssignmentMetadataMapper;
import tn.engn.assignmentapi.model.AssignableEntity;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.assignmentapi.model.AssignmentMetadata;
import tn.engn.assignmentapi.repository.AssignableEntityRepository;
import tn.engn.assignmentapi.repository.AssignmentMetadataRepository;
import tn.engn.assignmentapi.repository.AssignmentRepository;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.exception.EntityNotFoundException;
import tn.engn.hierarchicalentityapi.exception.ValidationException;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract service class for managing hierarchical entities and their assignable entities.
 *
 * @param <HE> Hierarchical Entity type
 * @param <AE> Assignable Entity type
 * @param <H>  Hierarchical Entity DTO type
 * @param <D>  Assignable Entity DTO type
 * @param <A>  Assignment Entity type
 */
@AllArgsConstructor
public abstract class BaseAssignmentService<A extends Assignment<HE, AE>, HE extends HierarchyBaseEntity<HE>, AE extends AssignableEntity<AE>, R extends AssignableEntityRequestDto, D extends AssignableEntityResponseDto, HR extends HierarchyRequestDto, H extends HierarchyResponseDto> {

    /**
     * Repository for managing hierarchical entities.
     * <p>
     * This repository provides CRUD operations and additional query capabilities for entities
     * representing hierarchical structures. It is used to interact with the persistence layer
     * for hierarchical entities.
     * </p>
     */
    @Autowired
    protected HierarchyBaseRepository<HE> hierarchicalEntityRepository;

    /**
     * Repository for managing assignable entities.
     * <p>
     * This repository provides CRUD operations and additional query capabilities for entities
     * that can be assigned or associated with other entities. It is used to interact with the
     * persistence layer for assignable entities.
     * </p>
     */
    @Autowired
    protected AssignableEntityRepository<AE> assignableEntityRepository;

    /**
     * Repository for managing assignments between hierarchical and assignable entities.
     * <p>
     * This repository extends QueryDSL capabilities to handle complex queries related to the
     * assignments between hierarchical entities and assignable entities. It supports querying
     * and managing assignments in a type-safe manner.
     * </p>
     */
    @Autowired
    protected AssignmentRepository<HE, AE, A> assignmentRepository;

    /**
     * Repository for managing metadata associated with assignments.
     * <p>
     * This repository provides CRUD operations for metadata related to assignments. It is used
     * to interact with the persistence layer for managing additional details or attributes
     * associated with assignments.
     * </p>
     */
    @Autowired
    protected AssignmentMetadataRepository assignmentMetadataRepository;

    /**
     * Mapper for converting between hierarchical entities and their respective DTOs.
     * <p>
     * This mapper provides methods for mapping between the hierarchical entity model and its
     * Data Transfer Objects (DTOs). It is used to transform data between different layers
     * of the application.
     * </p>
     */
    @Autowired
    protected HierarchyMapper<HE, HR, H> hierarchyMapper;

    /**
     * Mapper for converting between assignable entities and their respective DTOs.
     * <p>
     * This mapper provides methods for mapping between the assignable entity model and its
     * Data Transfer Objects (DTOs). It is used to transform data between different layers
     * of the application.
     * </p>
     */
    @Autowired
    protected AssignableEntityMapper<AE, R, D> assignableEntityMapper;

    /**
     * Mapper for converting between assignments and their respective DTOs.
     * <p>
     * This mapper provides methods for mapping between the assignment model and its
     * Data Transfer Objects (DTOs). It is used to transform data between different layers
     * of the application, facilitating the conversion of assignment entities and related
     * entities into DTOs for various operations.
     * </p>
     */
    @Autowired
    protected AssignmentMapper<HE, AE, HR, H, R, D> assignmentMapper;


    /**
     * Adds an assignable entity to a hierarchical entity.
     *
     * @param hierarchicalEntity the hierarchical entity
     * @param assignableEntity   the assignable entity
     */
    protected abstract void addEntityToHierarchicalEntity(HE hierarchicalEntity, AE assignableEntity);

    /**
     * Removes an assignable entity from a hierarchical entity.
     *
     * @param hierarchicalEntity the hierarchical entity
     * @param assignableEntity   the assignable entity
     */
    protected abstract void removeEntityFromHierarchicalEntity(HE hierarchicalEntity, AE assignableEntity);


    /**
     * Retrieves the list of hierarchical entities associated with a given assignable entity.
     *
     * @param assignableEntityId the ID of the assignable entity
     * @return the list of hierarchical entity response DTOs
     * @throws EntityNotFoundException if the assignable entity is not found
     */
    public List<H> getHierarchicalEntitiesForAssignableEntity(Long assignableEntityId) {
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        List<HE> hierarchicalEntities = getHierarchicalEntitiesFromAssignableEntity(assignableEntity); // Get the hierarchical entities
        return hierarchicalEntities.stream()
                .map(this::convertToDto) // Convert each hierarchical entity to DTO
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the list of hierarchical entities associated with an assignable entity.
     *
     * @param assignableEntity the assignable entity
     * @return the list of hierarchical entities
     */
    protected List<HE> getHierarchicalEntitiesFromAssignableEntity(AE assignableEntity) {
        return assignmentRepository.findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(getHierarchicalEntityClass(), assignableEntity);
    }

    /**
     * Retrieves a paginated list of hierarchical entities associated with a specified assignable entity.
     *
     * @param assignableEntityId the ID of the assignable entity for which to retrieve hierarchical entities
     * @param pageable           the pagination information to control the result set
     * @return a paginated {@link Page} of hierarchical entity response DTOs
     * @throws EntityNotFoundException if the assignable entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    public PaginatedResponseDto<H> getHierarchicalEntitiesForAssignableEntity(Long assignableEntityId, Pageable pageable) {
        // Retrieve the assignable entity from the repository
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        // Get hierarchical entities related to the assignable entity
        Page<HE> hierarchicalEntities = getHierarchicalEntitiesPageFromAssignableEntity(assignableEntity, pageable);

        // Convert the list of hierarchical entities to a paginated DTO page
        return convertHierarchicalEntitiesToDtoPage(hierarchicalEntities);
    }

    /**
     * Retrieves the page of hierarchical entities associated with an assignable entity.
     *
     * @param assignableEntity the assignable entity
     * @param pageable         the pagination information to control the result set
     * @return the page of hierarchical entities
     */
    protected Page<HE> getHierarchicalEntitiesPageFromAssignableEntity(AE assignableEntity, Pageable pageable) {
        return assignmentRepository.findHierarchicalEntitiesByAssignableEntityAndHierarchicalEntityClass(
                getHierarchicalEntityClass(),
                assignableEntity,
                pageable
        );
    }

    /**
     * Converts a page of hierarchical entities to a paginated response {@link PaginatedResponseDto} of response DTOs.
     *
     * @param hierarchicalEntities a page of hierarchical entities to convert
     * @return a paginated response DTO {@link PaginatedResponseDto} of hierarchical entity response DTOs
     */
    protected PaginatedResponseDto<H> convertHierarchicalEntitiesToDtoPage(Page<HE> hierarchicalEntities) {
        return hierarchyMapper.toDtoPage(hierarchicalEntities);
    }


    /**
     * Converts an assignable entity to its corresponding response DTO.
     *
     * @param assignableEntity the assignable entity
     * @return the response DTO
     */
    protected D convertToDto(AE assignableEntity) {
        return assignableEntityMapper.toDto(assignableEntity);
    }

    /**
     * Converts a hierarchical entity to its corresponding response DTO.
     *
     * @param hierarchicalEntity the hierarchical entity
     * @return the response DTO
     */
    protected H convertToDto(HE hierarchicalEntity) {
        return hierarchyMapper.toDto(hierarchicalEntity, false);

    }

    /**
     * Adds an assignable entity to a hierarchical entity with metadata.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityId   the ID of the assignable entity
     * @param metadataDtos         a list of metadata DTOs to add
     * @return the assignment response DTO containing the hierarchical entity, assignable entity, and metadata
     * @throws EntityNotFoundException if the hierarchical entity or assignable entity is not found
     * @throws AssignmentAlreadyExistsException if an assignment already exists between the specified hierarchical entity and assignable entity
     */
    @Transactional
    public AssignmentResponseDto<H, D> assignEntityToHierarchicalEntity(Long hierarchicalEntityId, Long assignableEntityId, List<AssignmentMetadataRequestDto> metadataDtos) {
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        boolean assignmentExists = assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(hierarchicalEntity, assignableEntity);

        if (assignmentExists) {
            throw new AssignmentAlreadyExistsException("Assignment already exists between the specified hierarchical entity and assignable entity");
        }

        addEntityToHierarchicalEntity(hierarchicalEntity, assignableEntity);
        hierarchicalEntityRepository.save(hierarchicalEntity);

        A assignment = createAssignment(hierarchicalEntity, assignableEntity, metadataDtos);
        assignmentRepository.save(assignment);

        H hierarchicalEntityDto = convertToDto(hierarchicalEntity);
        D assignableEntityDto = convertToDto(assignableEntity);

        List<AssignmentMetadataResponseDto> metadataResponseDtos = assignment.getMetadata().stream()
                .map(AssignmentMetadataMapper::toDto)
                .collect(Collectors.toList());

        return AssignmentResponseDto.<H, D>builder()
                .id(assignment.getId())
                .hierarchicalEntity(hierarchicalEntityDto)
                .assignableEntity(assignableEntityDto)
                .metadata(metadataResponseDtos)
                .build();
    }

    /**
     * Removes an assignable entity from a hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityId   the ID of the assignable entity
     * @return the assignment response DTO
     * @throws EntityNotFoundException if the hierarchical entity or assignable entity is not found
     */
    @Transactional
    public AssignmentResponseDto<H, D> removeEntityFromHierarchicalEntity(Long hierarchicalEntityId, Long assignableEntityId) {
        // Retrieve hierarchical entity
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Retrieve assignable entity
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        // Find the assignment
        A assignment = findAssignment(hierarchicalEntity, assignableEntity);
        if (assignment != null) {
            // Remove metadata associated with the assignment
            assignment.getMetadata().forEach(metadata -> {
                // This could be done in a separate service or method for clarity
                assignmentMetadataRepository.delete(metadata);
            });
            // Delete the assignment
            assignmentRepository.delete(assignment);
        }

        // Remove the assignable entity from the hierarchical entity
        removeEntityFromHierarchicalEntity(hierarchicalEntity, assignableEntity);
        hierarchicalEntityRepository.save(hierarchicalEntity); // Save the updated hierarchical entity

        // Convert entities to DTOs
        H hierarchicalEntityDto = convertToDto(hierarchicalEntity);
        D assignableEntityDto = convertToDto(assignableEntity);

        return new AssignmentResponseDto<>(hierarchicalEntityDto, assignableEntityDto);
    }

    /**
     * Assigns multiple assignable entities to a single hierarchical entity and returns a BulkAssignmentResponseDto.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityIds  a list of IDs of the assignable entities
     * @param metadataDtos         a common list of metadata DTOs to add to each assignment
     * @return a BulkAssignmentResponseDto containing the hierarchical entity and the list of assignable entities with metadata
     * @throws EntityNotFoundException          if the hierarchical entity or any assignable entity is not found
     * @throws AssignmentAlreadyExistsException if any assignment already exists between the specified hierarchical entity and assignable entity
     **/
    @Transactional
    public BulkAssignmentResponseDto<H, D> bulkAssignAssignableEntitiesToHierarchicalEntity(Long hierarchicalEntityId, List<Long> assignableEntityIds, List<AssignmentMetadataRequestDto> metadataDtos) {
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Fetch the assignable entities
        List<AE> assignableEntities = assignableEntityRepository.findAllById(assignableEntityIds);

        // Determine which IDs were not found
        Set<Long> foundEntityIds = assignableEntities.stream()
                .map(AssignableEntity::getId)
                .collect(Collectors.toSet());

        Set<Long> notFoundEntityIds = new HashSet<>(assignableEntityIds);
        notFoundEntityIds.removeAll(foundEntityIds);

        if (!notFoundEntityIds.isEmpty()) {
            throw new EntityNotFoundException("One or more assignable entities not found");
        }

        List<A> newAssignments = new ArrayList<>();

        for (AE assignableEntity : assignableEntities) {
            // Check if assignment already exists
            if (assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(hierarchicalEntity, assignableEntity)) {
                throw new AssignmentAlreadyExistsException("Assignment already exists for hierarchical entity ID: " + hierarchicalEntityId + " and assignable entity ID: " + assignableEntity.getId());
            }

            // Add entity to the hierarchical entity
            addEntityToHierarchicalEntity(hierarchicalEntity, assignableEntity);

            // Create new assignment
            A newAssignment = createAssignment(hierarchicalEntity, assignableEntity, metadataDtos);
            newAssignments.add(newAssignment);
        }

        // Save the updated hierarchical entity
        hierarchicalEntityRepository.save(hierarchicalEntity);

        // Save all new assignments
        assignmentRepository.saveAll(newAssignments);

        List<AssignmentMetadataResponseDto> metadataResponseDtos = null;

        if (newAssignments.size() > 0) {
            metadataResponseDtos = newAssignments.get(0).getMetadata().stream()
                    .map(AssignmentMetadataMapper::toDto)
                    .collect(Collectors.toList());
        }

        // Create response DTO
        BulkAssignmentResponseDto<H, D> responseDto = new BulkAssignmentResponseDto<>(
                List.of(convertToDto(hierarchicalEntity)),
                assignableEntities.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()),
                metadataResponseDtos
        );

        return responseDto;
    }

    /**
     * Removes multiple assignable entities from a single hierarchical entity and returns a BulkAssignmentResponseDto.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityIds  a list of IDs of the assignable entities
     * @return a BulkAssignmentResponseDto containing the hierarchical entity and the list of assignable entities
     * @throws EntityNotFoundException if the hierarchical entity or any assignable entity is not found
     */
    @Transactional
    public BulkAssignmentResponseDto<H, D> bulkRemoveAssignableEntitiesFromHierarchicalEntity(Long hierarchicalEntityId, List<Long> assignableEntityIds) {
        // Retrieve hierarchical entity
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Retrieve assignable entities
        List<AE> assignableEntities = assignableEntityRepository.findAllById(assignableEntityIds);

        // Determine which IDs were not found
        Set<Long> foundEntityIds = assignableEntities.stream()
                .map(AssignableEntity::getId)
                .collect(Collectors.toSet());

        Set<Long> notFoundEntityIds = new HashSet<>(assignableEntityIds);
        notFoundEntityIds.removeAll(foundEntityIds);

        if (!notFoundEntityIds.isEmpty()) {
            throw new EntityNotFoundException("One or more assignable entities not found");
        }

        // Process each assignable entity
        assignableEntities.forEach(assignableEntity -> {
            // Find and remove the assignment
            A assignment = findAssignment(hierarchicalEntity, assignableEntity);
            if (assignment != null) {
                // Remove metadata associated with the assignment
                assignment.getMetadata().forEach(metadata -> {
                    // This could be done in a separate service or method for clarity
                    assignmentMetadataRepository.delete(metadata);
                });
                // Delete the assignment
                assignmentRepository.delete(assignment);
            }
            // Remove the assignable entity from the hierarchical entity
            removeEntityFromHierarchicalEntity(hierarchicalEntity, assignableEntity);
        });

        hierarchicalEntityRepository.save(hierarchicalEntity); // Save the updated hierarchical entity

        // Create response DTO
        BulkAssignmentResponseDto<H, D> responseDto = new BulkAssignmentResponseDto<>(
                List.of(convertToDto(hierarchicalEntity)),
                assignableEntities.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList())
        );

        return responseDto;
    }

    /**
     * Assigns multiple hierarchical entities to a single assignable entity and returns a BulkAssignmentResponseDto.
     *
     * @param assignableEntityId  the ID of the assignable entity
     * @param hierarchicalEntityIds a list of IDs of the hierarchical entities
     * @param metadataDtos         a common list of metadata DTOs to add to each assignment
     * @return a BulkAssignmentResponseDto containing the assignable entity and the list of hierarchical entities with metadata
     * @throws EntityNotFoundException          if the assignable entity or any hierarchical entity is not found
     * @throws AssignmentAlreadyExistsException if any assignment already exists between the specified assignable entity and hierarchical entity
     */
    @Transactional
    public BulkAssignmentResponseDto<H, D> bulkAssignHierarchicalEntitiesToAssignableEntity(Long assignableEntityId, List<Long> hierarchicalEntityIds, List<AssignmentMetadataRequestDto> metadataDtos) {
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        List<HE> hierarchicalEntities = hierarchicalEntityRepository.findAllById(hierarchicalEntityIds);

        // Determine which IDs were not found
        Set<Long> foundEntityIds = hierarchicalEntities.stream()
                .map(HierarchyBaseEntity::getId)
                .collect(Collectors.toSet());

        Set<Long> notFoundEntityIds = new HashSet<>(hierarchicalEntityIds);
        notFoundEntityIds.removeAll(foundEntityIds);

        if (!notFoundEntityIds.isEmpty()) {
            throw new EntityNotFoundException("One or more hierarchical entities not found");
        }

        List<A> newAssignments = new ArrayList<>();

        for (HE hierarchicalEntity : hierarchicalEntities) {
            // Check if assignment already exists
            if (assignmentRepository.existsByHierarchicalEntityAndAssignableEntity(hierarchicalEntity, assignableEntity)) {
                throw new AssignmentAlreadyExistsException("Assignment already exists for hierarchical entity ID: " + hierarchicalEntity.getId() + " and assignable entity ID: " + assignableEntityId);
            }

            // Add hierarchical entity to the assignable entity
            addEntityToHierarchicalEntity(hierarchicalEntity, assignableEntity);

            // Create new assignment
            A newAssignment = createAssignment(hierarchicalEntity, assignableEntity, metadataDtos);
            newAssignments.add(newAssignment);
        }

        // Save the updated assignable entity
        assignableEntityRepository.save(assignableEntity);

        // Save all new assignments
        assignmentRepository.saveAll(newAssignments);

        List<AssignmentMetadataResponseDto> metadataResponseDtos = newAssignments.get(0).getMetadata().stream()
                .map(AssignmentMetadataMapper::toDto)
                .collect(Collectors.toList());

        // Create response DTO
        BulkAssignmentResponseDto<H, D> responseDto = new BulkAssignmentResponseDto<>(
                hierarchicalEntities.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()),
                List.of(convertToDto(assignableEntity)),
                metadataResponseDtos
        );

        return responseDto;
    }

    /**
     * Removes multiple hierarchical entities from a single assignable entity and returns a BulkAssignmentResponseDto.
     *
     * @param assignableEntityId  the ID of the assignable entity
     * @param hierarchicalEntityIds a list of IDs of the hierarchical entities
     * @return a BulkAssignmentResponseDto containing the assignable entity and the list of hierarchical entities
     * @throws EntityNotFoundException if the assignable entity or any hierarchical entity is not found
     */
    @Transactional
    public BulkAssignmentResponseDto<H, D> bulkRemoveHierarchicalEntitiesFromAssignableEntity(Long assignableEntityId, List<Long> hierarchicalEntityIds) {
        // Retrieve assignable entity
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        // Retrieve hierarchical entities
        List<HE> hierarchicalEntities = hierarchicalEntityRepository.findAllById(hierarchicalEntityIds);

        // Determine which IDs were not found
        Set<Long> foundEntityIds = hierarchicalEntities.stream()
                .map(HierarchyBaseEntity::getId)
                .collect(Collectors.toSet());

        Set<Long> notFoundEntityIds = new HashSet<>(hierarchicalEntityIds);
        notFoundEntityIds.removeAll(foundEntityIds);

        if (!notFoundEntityIds.isEmpty()) {
            throw new EntityNotFoundException("One or more hierarchical entities not found");
        }

        // Process each hierarchical entity
        hierarchicalEntities.forEach(hierarchicalEntity -> {
            // Find and remove the assignment
            A assignment = findAssignment(hierarchicalEntity, assignableEntity);
            if (assignment != null) {
                // Remove metadata associated with the assignment
                assignment.getMetadata().forEach(metadata -> {
                    // This could be done in a separate service or method for clarity
                    assignmentMetadataRepository.delete(metadata);
                });
                // Delete the assignment
                assignmentRepository.delete(assignment);
            }
            // Remove the hierarchical entity from the assignable entity
            removeEntityFromHierarchicalEntity(hierarchicalEntity, assignableEntity);
        });

        assignableEntityRepository.save(assignableEntity); // Save the updated assignable entity

        // Create response DTO
        BulkAssignmentResponseDto<H, D> responseDto = new BulkAssignmentResponseDto<>(
                hierarchicalEntities.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()),
                List.of(convertToDto(assignableEntity))
        );

        return responseDto;
    }

    /**
     * Retrieves the list of assignable entities associated with a hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @return the list of assignable entity response DTOs
     * @throws EntityNotFoundException if the hierarchical entity is not found
     */
    public List<D> getAssignableEntitiesByHierarchicalEntity(Long hierarchicalEntityId) {
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        List<AE> assignableEntities = getAssignableEntitiesFromHierarchicalEntity(hierarchicalEntity); // Get the assignable entities
        return assignableEntities.stream()
                .map(this::convertToDto) // Convert each assignable entity to DTO
                .collect(Collectors.toList()); // Collect and return the list of DTOs
    }

    /**
     * Retrieves the list of assignable entities associated with a hierarchical entity.
     *
     * @param hierarchicalEntity the hierarchical entity
     * @return the list of assignable entities
     */
    protected List<AE> getAssignableEntitiesFromHierarchicalEntity(HE hierarchicalEntity) {
        return assignmentRepository.findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(getAssignableEntityClass(), hierarchicalEntity);
    }

    /**
     * Retrieves a paginated list of assignable entities associated with a specified hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity for which to retrieve assignable entities
     * @param pageable             the pagination information to control the result set
     * @return a paginated {@link Page} of assignable entity response DTOs
     * @throws EntityNotFoundException if the hierarchical entity with the given ID is not found
     */
    @Transactional(readOnly = true)
    public PaginatedResponseDto<D> getAssignableEntitiesByHierarchicalEntity(Long hierarchicalEntityId, Pageable pageable) {
        // Retrieve the hierarchical entity from the repository
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Get assignable entities related to the hierarchical entity
        Page<AE> assignableEntities = getAssignableEntitiesPageFromHierarchicalEntity(hierarchicalEntity, pageable);

        // Convert the list of assignable entities to a paginated DTO page
        return convertAssignableEntitiesToDtoPage(assignableEntities);
    }

    /**
     * Retrieves the page of assignable entities associated with a hierarchical entity.
     *
     * @param hierarchicalEntity the hierarchical entity
     * @param pageable           the pagination information to control the result set
     * @return the page of assignable entities
     */
    protected Page<AE> getAssignableEntitiesPageFromHierarchicalEntity(HE hierarchicalEntity, Pageable pageable) {
        return assignmentRepository.findAssignableEntitiesByHierarchicalEntityAndAssignableEntityClass(getAssignableEntityClass(), hierarchicalEntity, pageable);
    }

    /**
     * Converts a list of assignable entities to a paginated {@link Page} of response DTOs.
     *
     * @param assignableEntities a page of assignable entities to convert
     * @return a paginated response DTO {@link PaginatedResponseDto} of assignable entity response DTOs
     */
    protected PaginatedResponseDto<D> convertAssignableEntitiesToDtoPage(Page<AE> assignableEntities) {
        return assignableEntityMapper.toDtoPage(assignableEntities);
    }

    /**
     * Retrieves the count of assignable entities associated with a hierarchical entity.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @return the count response DTO
     * @throws EntityNotFoundException if the hierarchical entity is not found
     */
    public int getAssignableEntityCountByHierarchicalEntity(Long hierarchicalEntityId) {
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        return getAssignableEntitiesFromHierarchicalEntity(hierarchicalEntity).size(); // Return the count of assignable entities
    }

    /**
     * Retrieves the count of hierarchical entities associated with an assignable entity.
     *
     * @param assignableEntityId the ID of the assignable entity
     * @return the count response DTO
     * @throws EntityNotFoundException if the assignable entity is not found
     */
    public int getHierarchicalEntityCountByAssignableEntity(Long assignableEntityId) {
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        return getHierarchicalEntitiesFromAssignableEntity(assignableEntity).size(); // Return the count of hierarchical entities
    }

    /**
     * Retrieves all assignments.
     *
     * @return a list of {@link AssignmentResponseDto} representing all assignments.
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto<H, D>> getAllAssignments() {
        // Retrieve all assignments from the repository
        List<A> assignments = assignmentRepository.findAll();

        // Convert each assignment to its corresponding DTO
        return assignments.stream()
                .map(this::convertToAssignmentResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Converts an assignment entity to its corresponding response DTO.
     *
     * @param assignment the assignment entity to convert
     * @return the {@link AssignmentResponseDto} representing the assignment
     */
    protected AssignmentResponseDto<H, D> convertToAssignmentResponseDto(A assignment) {
        return assignmentMapper.toDto(assignment);
    }


    /**
     * Retrieves all assignments with pagination.
     *
     * @param pageable pagination information
     * @return a page of {@link AssignmentResponseDto} representing the assignments.
     */
    @Transactional(readOnly = true)
    public PaginatedResponseDto<AssignmentResponseDto<H, D>> getAllAssignments(Pageable pageable) {
        // Retrieve paginated assignments from the repository
        Page<A> assignmentsPage = assignmentRepository.findAll(pageable);

        // Convert to paginated assignment response DTOs
        return convertToPaginatedAssignmentResponseDto(assignmentsPage);
    }

    /**
     * Retrieves all assignments for specific hierarchical entity and assignable entity classes.
     *
     * @return a list of {@link AssignmentResponseDto} representing all assignments for the given classes
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto<H, D>> getAssignmentsByEntityClasses() {
        // Retrieve assignments for the given hierarchical entity and assignable entity classes from the repository
        List<A> assignments = assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClass(getHierarchicalEntityClass(), getAssignableEntityClass());

        // Convert each assignment to its corresponding DTO
        return assignments.stream()
                .map(this::convertToAssignmentResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Returns the class type of the hierarchical entity associated with this repository.
     * <p>
     * This method is abstract and should be implemented by subclasses to specify the
     * actual type of the hierarchical entity managed by the repository. It is used
     * primarily to assist in constructing type-safe queries and for other type-specific
     * operations within the repository.
     * </p>
     *
     * @return the {@link Class} object representing the type of the hierarchical entity
     */
    abstract Class<HE> getHierarchicalEntityClass();

    /**
     * Returns the class type of the assignable entity associated with this repository.
     * <p>
     * This method is abstract and should be implemented by subclasses to specify the
     * actual type of the assignable entity managed by the repository. It is used
     * primarily to assist in constructing type-safe queries and for other type-specific
     * operations within the repository.
     * </p>
     *
     * @return the {@link Class} object representing the type of the assignable entity
     */
    abstract Class<AE> getAssignableEntityClass();

    /**
     * Retrieves all assignments with pagination for specific hierarchical entity and assignable entity classes.
     *
     * @param pageable pagination information
     * @return a page of {@link AssignmentResponseDto} representing all assignments for the given classes.
     */
    @Transactional(readOnly = true)
    public PaginatedResponseDto<AssignmentResponseDto<H, D>> getAssignmentsByEntityClasses(Pageable pageable) {
        // Retrieve assignments with pagination from the repository
        Page<A> assignmentsPage = assignmentRepository.findByHierarchicalEntityClassAndAssignableEntityClass(getHierarchicalEntityClass(), getAssignableEntityClass(), pageable);

        // Convert to paginated assignment response DTOs
        return convertToPaginatedAssignmentResponseDto(assignmentsPage);
    }

    /**
     * Converts an assignment entity page to paginated response DTOs.
     *
     * @param assignmentPage the assignment entity page to convert
     * @return the {@link AssignmentResponseDto} representing the paginated assignment response DTOs
     */
    protected PaginatedResponseDto<AssignmentResponseDto<H, D>> convertToPaginatedAssignmentResponseDto(Page<A> assignmentPage) {
        return assignmentMapper.toDtoPage((Page<Assignment<HE, AE>>) assignmentPage);
    }

    /**
     * Updates an assignment based on the provided request DTO.
     *
     * @param requestDto the {@link AssignmentRequestDto} containing update information
     * @return the updated {@link AssignmentResponseDto}
     * @throws EntityNotFoundException if the hierarchical entity or assignable entity is not found
     */
    @Transactional
    public AssignmentResponseDto<H, D> updateAssignment(AssignmentRequestDto requestDto) {
        // Extract data from request DTO
        Long hierarchicalEntityId = requestDto.getHierarchicalEntityId();
        Long assignableEntityId = requestDto.getAssignableEntityId();
        List<AssignmentMetadataRequestDto> metadataRequests = requestDto.getMetadata();

        // Retrieve the hierarchical entity
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Retrieve the assignable entity
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        // Find the existing assignment
        A assignment = findAssignment(hierarchicalEntity, assignableEntity);

        // Update metadata for the assignment
        updateMetadata(assignment, metadataRequests);

        // Save the updated assignment
        A updatedAssignment = assignmentRepository.save(assignment);

        // Convert the updated assignment to DTO
        return convertToAssignmentResponseDto(updatedAssignment);
    }

    /**
     * Updates the metadata for an assignment based on the provided metadata requests.
     *
     * @param assignment       the assignment to update
     * @param metadataRequests a list of {@link AssignmentMetadataRequestDto} containing the metadata updates
     */
    protected void updateMetadata(A assignment, List<AssignmentMetadataRequestDto> metadataRequests) {
        validateMetadata(metadataRequests);

        // Fetch existing metadata for the assignment
        Set<AssignmentMetadata> existingMetadata = assignment.getMetadata();

        // Convert metadata request DTOs to metadata objects
        Map<String, AssignmentMetadata> metadataMap = metadataRequests.stream()
                .map(requestDto -> AssignmentMetadata.builder()
                        .assignment(assignment)
                        .key(requestDto.getKey())
                        .value(requestDto.getValue())
                        .build())
                .collect(Collectors.toMap(AssignmentMetadata::getKey, metadata -> metadata));

        // Update existing metadata or add new metadata
        for (AssignmentMetadata metadata : existingMetadata) {
            if (metadataMap.containsKey(metadata.getKey())) {
                // Update existing metadata value
                metadata.setValue(metadataMap.get(metadata.getKey()).getValue());
                metadataMap.remove(metadata.getKey());
            }
        }

        // Add new metadata entries
        existingMetadata.addAll(metadataMap.values());

        // Set the updated metadata on the assignment
        assignment.setMetadata(existingMetadata);
    }

    /**
     * Creates a new assignment between a hierarchical entity and an assignable entity with optional metadata.
     *
     * @param hierarchicalEntity the hierarchical entity
     * @param assignableEntity   the assignable entity
     * @param metadataDtos       optional metadata for the assignment
     * @return the created assignment
     */
    protected A createAssignment(HE hierarchicalEntity, AE assignableEntity, List<AssignmentMetadataRequestDto> metadataDtos) {
        A assignment = createAssignmentInstance();
        assignment.setHierarchicalEntity(hierarchicalEntity);
        assignment.setAssignableEntity(assignableEntity);

        // Add metadata entries to the assignment using a custom mapper
        if (metadataDtos != null && !metadataDtos.isEmpty() &&
                validateMetadata(metadataDtos)) {
            Set<AssignmentMetadata> metadataEntities = metadataDtos.stream()
                    .map(dto -> AssignmentMetadataMapper.toEntity(dto, assignment))
                    .collect(Collectors.toSet());
            assignment.setMetadata(metadataEntities);
        }

        return assignment;
    }

    /**
     * Creates a new instance of the assignment.
     *
     * @return a new assignment instance
     */
    protected abstract A createAssignmentInstance();

    /**
     * Finds an assignment for the given hierarchical and assignable entities.
     *
     * @param hierarchicalEntity the hierarchical entity
     * @param assignableEntity   the assignable entity
     * @return the assignment
     * @throws EntityNotFoundException if the assignment is not found
     */
    protected A findAssignment(HE hierarchicalEntity, AE assignableEntity) {
        return assignmentRepository.findByHierarchicalEntityAndAssignableEntity(hierarchicalEntity, assignableEntity)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found"));
    }

    /**
     * Validates that the metadata list in the assignment request does not contain duplicates.
     *
     * @param metadata List of metadata to validate.
     */
    public boolean validateMetadata(List<AssignmentMetadataRequestDto> metadata) {
        // Check for duplicate keys
        checkDuplicateKeys(metadata);

        // Validate each metadata entry
        for (AssignmentMetadataRequestDto entry : metadata) {
            // Validate key
            validateKey(entry);

            // Validate value
            validateValue(entry);
        }
        return true;
    }

    private static void validateValue(AssignmentMetadataRequestDto entry) {
        String value = entry.getValue();
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Metadata value cannot be null or empty.");
        }
        if (value.length() > 1024) {
            throw new ValidationException("Metadata value length cannot exceed 1024 characters.");
        }
    }

    private static void validateKey(AssignmentMetadataRequestDto entry) {
        String key = entry.getKey();
        if (key == null || key.trim().isEmpty()) {
            throw new ValidationException("Metadata key cannot be null or empty.");
        }
        if (key.length() > 255) {
            throw new ValidationException("Metadata key length cannot exceed 255 characters.");
        }
        if (!key.matches("[a-zA-Z0-9_]+")) { // Optional: adjust regex as needed
            throw new ValidationException("Metadata key contains invalid characters.");
        }
    }

    private static void checkDuplicateKeys(List<AssignmentMetadataRequestDto> metadata) {
        Map<String, Long> keyCounts = metadata.stream()
                .map(AssignmentMetadataRequestDto::getKey)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        if (keyCounts.values().stream().anyMatch(count -> count > 1)) {
            throw new ValidationException("Metadata list contains duplicate keys.");
        }
    }

    /**
     * Removes metadata from an assignment based on the metadata ID.
     *
     * @param assignmentId The ID of the assignment.
     * @param metadataId   The ID of the metadata to remove.
     */
    @Transactional
    public void removeMetadataById(Long assignmentId, Long metadataId) {
        A assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found"));

        AssignmentMetadata metadata = assignmentMetadataRepository.findById(metadataId)
                .orElseThrow(() -> new MetadataNotFoundException("Metadata not found"));

        assignment.removeMetadata(metadata);
        assignmentMetadataRepository.delete(metadata);

        // Save the updated assignment
        assignmentRepository.save(assignment);
    }

    /**
     * Removes metadata from an assignment based on the metadata key.
     *
     * @param assignmentId The ID of the assignment.
     * @param metadataKey  The key of the metadata to remove.
     */
    @Transactional
    public void removeMetadataByKey(Long assignmentId, String metadataKey) {
        A assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found"));

        Optional<AssignmentMetadata> metadataOptional = assignment.getMetadata().stream()
                .filter(metadata -> metadata.getKey().equals(metadataKey))
                .findFirst();

        if (metadataOptional.isPresent()) {
            AssignmentMetadata metadata = metadataOptional.get();
            assignment.removeMetadata(metadata);
            assignmentMetadataRepository.delete(metadata);

            // Save the updated assignment
            assignmentRepository.save(assignment);
        } else {
            throw new MetadataNotFoundException("Metadata with key '" + metadataKey + "' not found");
        }
    }

    /**
     * Removes metadata from an assignment by hierarchical entity ID, assignable entity ID, and metadata ID.
     * <p>
     * This method first checks the existence of both the hierarchical and assignable entities, then finds the
     * corresponding assignment, and finally removes the metadata with the specified ID.
     * </p>
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityId   the ID of the assignable entity
     * @param metadataId           the ID of the metadata to be removed
     * @throws EntityNotFoundException     if the hierarchical entity or assignable entity is not found
     * @throws AssignmentNotFoundException if the assignment is not found
     */
    @Transactional
    public void removeMetadataByHierarchicalAndAssignableEntity(Long hierarchicalEntityId, Long assignableEntityId, Long metadataId) {
        // Check if the hierarchical entity exists
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Check if the assignable entity exists
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        // Find the assignment
        A assignment = assignmentRepository
                .findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found"));

        // Find and remove the metadata by ID
        removeMetadataById(assignment.getId(), metadataId);
    }

    /**
     * Removes metadata from an assignment by hierarchical entity ID, assignable entity ID, and metadata key.
     * <p>
     * This method first checks the existence of both the hierarchical and assignable entities, then finds the
     * corresponding assignment, and finally removes the metadata with the specified key.
     * </p>
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityId   the ID of the assignable entity
     * @param metadataKey          the key of the metadata to be removed
     * @throws EntityNotFoundException     if the hierarchical entity or assignable entity is not found
     * @throws AssignmentNotFoundException if the assignment is not found
     */
    @Transactional
    public void removeMetadataByHierarchicalAndAssignableEntity(Long hierarchicalEntityId, Long assignableEntityId, String metadataKey) {
        // Check if the hierarchical entity exists
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Check if the assignable entity exists
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        // Find the assignment
        A assignment = assignmentRepository
                .findByHierarchicalEntityIdAndAssignableEntityId(hierarchicalEntityId, assignableEntityId)
                .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found"));

        // Find and remove the metadata by key
        removeMetadataByKey(assignment.getId(), metadataKey);
    }

    /**
     * Retrieves a list of assignments associated with a specific hierarchical entity ID.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @return a list of assignments as DTOs associated with the specified hierarchical entity
     * @throws EntityNotFoundException if the hierarchical entity with the given ID is not found
     */
    public List<AssignmentResponseDto<H, D>> getAssignmentsByHierarchicalEntity(Long hierarchicalEntityId) {
        // Check if the hierarchical entity exists
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Retrieve assignments associated with the hierarchical entity and map to DTOs
        return assignmentRepository
                .findByHierarchicalEntityClassAndAssignableEntityClassAndHierarchicalEntity(
                        getHierarchicalEntityClass(),
                        getAssignableEntityClass(),
                        hierarchicalEntity
                )
                .stream()
                .map(assignmentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a paginated list of assignments associated with a specific hierarchical entity ID.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param pageable             the pagination information
     * @return a page of assignments as DTOs associated with the specified hierarchical entity
     * @throws EntityNotFoundException if the hierarchical entity with the given ID is not found
     */
    public PaginatedResponseDto<AssignmentResponseDto<H, D>> getAssignmentsByHierarchicalEntity(Long hierarchicalEntityId, Pageable pageable) {
        // Check if the hierarchical entity exists
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Retrieve assignments associated with the hierarchical entity and map to DTOs
        return assignmentMapper.toDtoPage(
                assignmentRepository
                        .findByHierarchicalEntityClassAndAssignableEntityClassAndHierarchicalEntity(
                                getHierarchicalEntityClass(),
                                getAssignableEntityClass(),
                                hierarchicalEntity,
                                pageable
                        )
        );
    }

    /**
     * Retrieves a list of assignments associated with a specific assignable entity ID.
     *
     * @param assignableEntityId the ID of the assignable entity
     * @return a list of assignments as DTOs associated with the specified assignable entity
     * @throws EntityNotFoundException if the assignable entity with the given ID is not found
     */
    public List<AssignmentResponseDto<H, D>> getAssignmentsByAssignableEntity(Long assignableEntityId) {
        // Check if the assignable entity exists
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        // Retrieve assignments associated with the assignable entity and map to DTOs
        return assignmentRepository.
                findByHierarchicalEntityClassAndAssignableEntityClassAndAssignableEntity(
                        getHierarchicalEntityClass(),
                        getAssignableEntityClass(),
                        assignableEntity
                )
                .stream()
                .map(assignmentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a paginated list of assignments associated with a specific assignable entity ID.
     *
     * @param assignableEntityId the ID of the assignable entity
     * @param pageable           the pagination information
     * @return a page of assignments as DTOs associated with the specified assignable entity
     * @throws EntityNotFoundException if the assignable entity with the given ID is not found
     */
    public PaginatedResponseDto<AssignmentResponseDto<H, D>> getAssignmentsByAssignableEntity(Long assignableEntityId, Pageable pageable) {
        // Check if the assignable entity exists
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        // Retrieve assignments associated with the assignable entity and map to DTOs
        return assignmentMapper.toDtoPage(
                assignmentRepository
                        .findByHierarchicalEntityClassAndAssignableEntityClassAndAssignableEntity(
                                getHierarchicalEntityClass(),
                                getAssignableEntityClass(),
                                assignableEntity,
                                pageable
                        )
        );
    }

    /**
     * Retrieves an assignment associated with a specific hierarchical entity ID and assignable entity ID.
     *
     * @param hierarchicalEntityId the ID of the hierarchical entity
     * @param assignableEntityId   the ID of the assignable entity
     * @return the assignment as a DTO associated with the specified hierarchical and assignable entities
     * @throws EntityNotFoundException if the assignment is not found for the given hierarchical and assignable entity IDs
     */
    public AssignmentResponseDto<H, D> getAssignmentByHierarchicalAndAssignableEntity(Long hierarchicalEntityId, Long assignableEntityId) {
        // Check if the hierarchical entity exists
        HE hierarchicalEntity = hierarchicalEntityRepository.findById(hierarchicalEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Hierarchical entity not found"));

        // Check if the assignable entity exists
        AE assignableEntity = assignableEntityRepository.findById(assignableEntityId)
                .orElseThrow(() -> new EntityNotFoundException("Assignable entity not found"));

        // Retrieve the assignment associated with the hierarchical and assignable entities
        A assignment = assignmentRepository.findByHierarchicalEntityAndAssignableEntity(hierarchicalEntity, assignableEntity).orElseThrow(() -> new EntityNotFoundException("Assignment not found for the given hierarchical entity ID: "
                + hierarchicalEntityId + " and assignable entity ID: " + assignableEntityId));

        // Convert the assignment to DTO
        return assignmentMapper.toDto(assignment);

    }
}
