package tn.engn.hierarchicalentityapi.mapper;

import org.springframework.data.domain.Pageable;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract implementation of the HierarchyMapper interface.
 *
 * @param <E>  the entity type managed by this service, extending HierarchyBaseEntity
 * @param <RD> the request DTO type used for creating or updating the entity
 * @param <SD> the response DTO type used for retrieving the entity
 */
public abstract class AbstractHierarchyMapper<E extends HierarchyBaseEntity<E>, RD extends HierarchyRequestDto, SD extends HierarchyResponseDto>
        implements HierarchyMapper<E, RD, SD> {

    /**
     * Converts a request DTO to an entity.
     *
     * @param dto the request DTO
     * @return the entity
     */
    @Override
    public E toEntity(RD dto) {
        if (dto == null) {
            return null;
        }

        E entity = createNewEntityInstance();
        entity.setId(dto.getId());
        entity.setName(dto.getName());

        // Map parent entity if parentEntityId is provided
        if (dto.getParentEntityId() != null) {
            E parentEntity = createNewEntityInstance();
            parentEntity.setId(dto.getParentEntityId());
            entity.setParentEntity(parentEntity);
        }

        return entity;
    }

    /**
     * Converts an entity to a response DTO.
     *
     * @param entity the entity
     * @return the response DTO
     */
    @Override
    public SD toDto(E entity) {
        if (entity == null) {
            return null;
        }

        SD dto = createNewResponseDtoInstance();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setParentEntityId(entity.getParentEntity() != null ? entity.getParentEntity().getId() : null);
        dto.setSubEntitiesCount(entity.getSubEntities().size());

        // Fetch and set sub-entities if the flag is true
        if (shouldFetchSubEntities()) {
            setSubEntities(dto, entity);
        }

        return dto;
    }

    /**
     * Converts an entity to its corresponding response DTO.
     *
     * @param entity           the entity to convert
     * @param fetchSubEntities flag indicating whether to fetch detailed sub-entities
     * @return the response DTO representing the entity
     */
    @Override
    public SD toDto(E entity, boolean fetchSubEntities) {
        if (entity == null) {
            return null;
        }

        SD dto = createNewResponseDtoInstance(); // Create a new instance of response DTO
        dto.setId(entity.getId()); // Set ID from entity to DTO
        dto.setName(entity.getName()); // Set name from entity to DTO
        dto.setParentEntityId(entity.getParentEntity() != null ? entity.getParentEntity().getId() : null); // Set parent entity ID from entity to DTO
        dto.setSubEntitiesCount(entity.getSubEntities().size());

        // Fetch and set sub-entities if fetchSubEntities is true
        if (fetchSubEntities) {
            setSubEntities(dto, entity);
        }

        return dto; // Return the populated response DTO
    }

    /**
     * Converts a list of entities to a list of response DTOs.
     *
     * @param entities the list of entities
     * @return the list of response DTOs
     */
    @Override
    public List<SD> toDtoList(List<E> entities) {
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Converts a list of entities to a list of response DTOs with an option to fetch sub-entities.
     *
     * @param entities         the list of entities
     * @param fetchSubEntities flag indicating whether to fetch detailed sub-entities for each entity
     * @return the list of response DTOs
     */
    @Override
    public List<SD> toDtoList(List<E> entities, boolean fetchSubEntities) {
        return entities.stream().map(entity -> toDto(entity, fetchSubEntities)).collect(Collectors.toList());
    }

    /**
     * Converts a list of entities to a paginated response DTO with an option to fetch sub-entities.
     *
     * @param entities         the list of entities
     * @param pageable         the pagination information
     * @param totalElements    the total number of elements
     * @param fetchSubEntities flag indicating whether to fetch detailed sub-entities for each entity
     * @return the paginated response DTO
     */
    @Override
    public PaginatedResponseDto<SD> toPaginatedDtoList(List<E> entities, Pageable pageable, long totalElements, boolean fetchSubEntities) {
        List<SD> dtos = entities.stream().map(entity -> toDto(entity, fetchSubEntities)).collect(Collectors.toList());

        return PaginatedResponseDto.<SD>builder()
                .content(dtos)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()))
                .build();
    }

    /**
     * Sets sub-entities (children) in the response DTO.
     *
     * @param responseDto the response DTO to populate with sub-entities
     * @param entity      the entity from which to fetch sub-entities
     */
    @Override
    public void setSubEntities(SD responseDto, E entity) {
        if (shouldFetchSubEntities()) {
            List<SD> subEntities = entity.getSubEntities().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            responseDto.setSubEntities(subEntities);
        } else {
            responseDto.setSubEntities(null); // Optional: Set sub-entities to null if only count is desired
        }
    }

    /**
     * Determine whether to fetch sub-entities or just count based on some flag or logic.
     *
     * @return boolean indicating whether to fetch sub-entities
     */
    protected abstract boolean shouldFetchSubEntities();

    /**
     * Create a new instance of the entity class.
     *
     * @return a new instance of the entity class
     */
    protected abstract E createNewEntityInstance();

    /**
     * Create a new instance of the response DTO class.
     *
     * @return a new instance of the response DTO class
     */
    protected abstract SD createNewResponseDtoInstance();

    /**
     * Casts a HierarchyBaseEntity to the specific type E.
     *
     * @param baseEntity The base entity to cast.
     * @return The entity of type E.
     */
    @Override
    public E castToSpecificEntity(HierarchyBaseEntity baseEntity) {
        return (E) baseEntity;
    }
}
