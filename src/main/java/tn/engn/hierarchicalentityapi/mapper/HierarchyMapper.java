package tn.engn.hierarchicalentityapi.mapper;

import org.springframework.data.domain.Pageable;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

import java.util.List;

/**
 * Mapper interface for converting between entity and DTO.
 *
 * @param <E>  the entity type managed by this service
 * @param <RD> the request DTO type used for creating or updating the entity
 * @param <SD> the response DTO type used for retrieving the entity
 */
public interface HierarchyMapper<E extends HierarchyBaseEntity, RD extends HierarchyRequestDto, SD extends HierarchyResponseDto> {

    /**
     * Converts a request DTO to an entity.
     *
     * @param dto the request DTO
     * @return the entity
     */
    E toEntity(RD dto);

    /**
     * Converts an entity to a response DTO.
     *
     * @param entity the entity
     * @return the response DTO
     */
    SD toDto(E entity);

    /**
     * Converts an entity to its corresponding response DTO.
     *
     * @param entity           the entity to convert
     * @param fetchSubEntities flag indicating whether to fetch detailed sub-entities
     * @return the response DTO representing the entity
     */
    SD toDto(E entity, boolean fetchSubEntities);

    /**
     * Converts a list of entities to a list of response DTOs.
     *
     * @param entities the list of entities
     * @return the list of response DTOs
     */
    List<SD> toDtoList(List<E> entities);

    /**
     * Converts a list of entities to a list of response DTOs with an option to fetch sub-entities.
     *
     * @param entities         the list of entities
     * @param fetchSubEntities flag indicating whether to fetch detailed sub-entities for each entity
     * @return the list of response DTOs
     */
    List<SD> toDtoList(List<E> entities, boolean fetchSubEntities);

    /**
     * Converts a list of entities to a paginated response DTO with an option to fetch sub-entities.
     *
     * @param entities         the list of entities
     * @param pageable         the pagination information
     * @param totalElements    the total number of elements
     * @param fetchSubEntities flag indicating whether to fetch detailed sub-entities for each entity
     * @return the paginated response DTO
     */
    PaginatedResponseDto<SD> toPaginatedDtoList(List<E> entities, Pageable pageable, long totalElements, boolean fetchSubEntities);

    /**
     * Sets the sub-entities for a response DTO.
     *
     * @param responseDto the response DTO
     * @param entity      the entity
     */
    void setSubEntities(SD responseDto, E entity);

    /**
     * Sets sub-entities (children) in the response DTO with an option to fetch sub-entities.
     *
     * @param responseDto      the response DTO to populate with sub-entities
     * @param entity           the entity from which to fetch sub-entities
     * @param fetchSubEntities flag indicating whether to fetch sub-entities
     */
     void setSubEntities(SD responseDto, E entity, boolean fetchSubEntities);

    /**
     * Casts a HierarchyBaseEntity to the specific type E.
     *
     * @param baseEntity The base entity to cast.
     * @return The entity of type E.
     */
    E castToSpecificEntity(HierarchyBaseEntity baseEntity);
}