package tn.engn.assignmentapi.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.engn.assignmentapi.dto.*;
import tn.engn.assignmentapi.model.AssignableEntity;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.assignmentapi.model.AssignmentMetadata;
import tn.engn.assignmentapi.repository.AssignableEntityRepository;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.mapper.HierarchyMapper;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.repository.HierarchyBaseRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract implementation of the {@link AssignmentMapper} interface.
 * This class provides common mapping logic between {@link Assignment} entities and their corresponding DTOs.
 * Concrete implementations must define how to instantiate specific {@link Assignment} entities.
 *
 * @param <E>  the type of the hierarchical entity
 * @param <A>  the type of the assignable entity
 * @param <HR> the type of the hierarchical entity request DTO
 * @param <H>  the type of the hierarchical entity response DTO
 * @param <R>  the type of the assignable entity request DTO
 * @param <D>  the type of the assignable entity response DTO
 */
public abstract class AssignmentMapperImpl<E extends HierarchyBaseEntity<E>,
        A extends AssignableEntity<A>,
        HR extends HierarchyRequestDto,
        H extends HierarchyResponseDto,
        R extends AssignableEntityRequestDto,
        D extends AssignableEntityResponseDto>
        implements AssignmentMapper<E, A, HR, H, R, D> {

    // Mappers for converting between hierarchical and assignable entities and their corresponding DTOs
    private final HierarchyMapper<E, HR, H> hierarchyMapper;
    private final AssignableEntityMapper<A, R, D> assignableEntityMapper;
    private final HierarchyBaseRepository<E> hierarchyEntityRepository;
    private final AssignableEntityRepository<A> assignableEntityRepository;

    /**
     * Constructor for {@link AssignmentMapper}.
     * Initializes the mappers required for converting hierarchical and assignable entities.
     *
     * @param hierarchyMapper        the mapper for hierarchical entities
     * @param assignableEntityMapper the mapper for assignable entities
     * @param hierarchyEntityRepository the repository for hierarchical entities
     * @param assignableEntityRepository the repository for assignable entities
     */
    public AssignmentMapperImpl(HierarchyMapper<E, HR, H> hierarchyMapper,
                                AssignableEntityMapper<A, R, D> assignableEntityMapper,
                                HierarchyBaseRepository<E> hierarchyEntityRepository,
                                AssignableEntityRepository<A> assignableEntityRepository) {
        this.hierarchyMapper = hierarchyMapper;
        this.assignableEntityMapper = assignableEntityMapper;
        this.hierarchyEntityRepository = hierarchyEntityRepository;
        this.assignableEntityRepository = assignableEntityRepository;
    }

    /**
     * Abstract method to create a new instance of a specific {@link Assignment} subclass.
     * Concrete subclasses must implement this method to instantiate the correct {@link Assignment} type.
     *
     * @return a new instance of a concrete {@link Assignment} subclass
     */
    protected abstract Assignment<E, A> createAssignmentInstance();

    /**
     * Converts an {@link AssignmentRequestDto} to an {@link Assignment} entity.
     * This method maps the DTO fields to the corresponding entity fields.
     *
     * @param dto the {@link AssignmentRequestDto} to convert
     * @return the corresponding {@link Assignment} entity
     * @throws IllegalArgumentException if the hierarchical entity ID or assignable entity ID is invalid
     */
    @Override
    public Assignment<E, A> toEntity(AssignmentRequestDto dto) {
        if (dto == null) {
            return null;
        }

        // Create a new Assignment entity instance using the factory method
        Assignment<E, A> assignment = createAssignmentInstance();

        // Fetch the hierarchical entity using the repository
        E hierarchicalEntity = hierarchyEntityRepository.findById(dto.getHierarchicalEntityId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid hierarchical entity ID: " + dto.getHierarchicalEntityId()));
        assignment.setHierarchicalEntity(hierarchicalEntity);

        // Fetch the assignable entity using the repository
        A assignableEntity = assignableEntityRepository.findById(dto.getAssignableEntityId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid assignable entity ID: " + dto.getAssignableEntityId()));
        assignment.setAssignableEntity(assignableEntity);

        // Convert list of metadata DTOs to AssignmentMetadata entities
        if (dto.getMetadata() != null) {
            dto.getMetadata().forEach(metadataDto -> {
                AssignmentMetadata metadata = new AssignmentMetadata();
                metadata.setKey(metadataDto.getKey());
                metadata.setValue(metadataDto.getValue());
                assignment.addMetadata(metadata);
            });
        }

        return assignment;
    }

    /**
     * Converts an {@link Assignment} entity to an {@link AssignmentResponseDto}.
     * This method maps the entity fields to the corresponding DTO fields.
     *
     * @param assignment the {@link Assignment} entity to convert
     * @return the corresponding {@link AssignmentResponseDto}
     */
    @Override
    public AssignmentResponseDto<H, D> toDto(Assignment<E, A> assignment) {
        if (assignment == null) {
            return null;
        }

        // Create a new AssignmentResponseDto and set its properties based on the entity fields
        return AssignmentResponseDto.<H, D>builder()
                .id(assignment.getId())
                .hierarchicalEntity(hierarchyMapper.toDto(assignment.getHierarchicalEntity(), false))
                .assignableEntity(assignableEntityMapper.toDto(assignment.getAssignableEntity()))
                .metadata(assignment.getMetadata().stream()
                        .map(metadata -> AssignmentMetadataResponseDto.builder()
                                .id(metadata.getId())
                                .key(metadata.getKey())
                                .value(metadata.getValue())
                                .build()
                        )
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Converts a list of {@link Assignment} entities to a list of {@link AssignmentResponseDto}.
     * This method is useful when converting multiple entities at once.
     *
     * @param assignments the list of {@link Assignment} entities to convert
     * @return the corresponding list of {@link AssignmentResponseDto}
     */
    @Override
    public List<AssignmentResponseDto<H, D>> toDtoList(List<Assignment<E, A>> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return List.of();
        }

        // Map each Assignment entity to an AssignmentResponseDto
        return assignments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of {@link Assignment} entities to a {@link PaginatedResponseDto} containing a list of {@link AssignmentResponseDto}.
     * This method is useful when dealing with paginated data and needs to include pagination metadata.
     *
     * @param assignments   the list of {@link Assignment} entities to convert
     * @param pageable      the pagination information
     * @param totalElements the total number of elements across all pages
     * @return the {@link PaginatedResponseDto} containing the list of {@link AssignmentResponseDto} and pagination details
     */
    @Override
    public PaginatedResponseDto<AssignmentResponseDto<H, D>> toPaginatedDtoList(List<Assignment<E, A>> assignments,
                                                                                Pageable pageable,
                                                                                long totalElements) {
        // Convert the list of assignments to a list of DTOs
        List<AssignmentResponseDto<H, D>> dtos = toDtoList(assignments);

        // Create and return a PaginatedResponseDto containing the DTO list and pagination details
        return PaginatedResponseDto.<AssignmentResponseDto<H, D>>builder()
                .content(dtos)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()))
                .build();
    }

    /**
     * Converts a {@link Page} of {@link Assignment} entities to a {@link PaginatedResponseDto} containing a list of {@link AssignmentResponseDto}.
     * This method simplifies the conversion of paginated data directly from the repository.
     *
     * @param page the {@link Page} of {@link Assignment} entities
     * @return the {@link PaginatedResponseDto} containing the list of {@link AssignmentResponseDto} and pagination details
     */
    @Override
    public PaginatedResponseDto<AssignmentResponseDto<H, D>> toDtoPage(Page<Assignment<E, A>> page) {
        if (page == null) {
            return null;
        }

        // Convert the Page's content to a list of DTOs
        List<AssignmentResponseDto<H, D>> content = toDtoList(page.getContent());

        // Create and return a PaginatedResponseDto with the DTO list and pagination metadata
        return PaginatedResponseDto.<AssignmentResponseDto<H, D>>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
