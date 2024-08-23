package tn.engn.hierarchicalentityapi.mapper;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;
import tn.engn.hierarchicalentityapi.model.QHierarchyBaseEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract implementation of the HierarchyMapper interface.
 *
 * @param <E>  the entity type managed by this mapper, extending HierarchyBaseEntity
 * @param <RD> the request DTO type used for creating or updating the entity
 * @param <SD> the response DTO type used for retrieving the entity
 */
@Slf4j
public abstract class AbstractHierarchyMapper<E extends HierarchyBaseEntity<E>, RD extends HierarchyRequestDto, SD extends HierarchyResponseDto>
        implements HierarchyMapper<E, RD, SD> {

    @Autowired
    private JPAQueryFactory jpaQueryFactory;

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
        entity.setParentId(dto.getParentEntityId());

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

        SD responseDto = createNewResponseDtoInstance();
        responseDto.setId(entity.getId());
        responseDto.setName(entity.getName());
        responseDto.setParentEntityId(entity.getParentId());

        setSubEntities(responseDto, entity);

        return responseDto;
    }

    /**
     * Converts an entity to a response DTO with an option to fetch sub-entities.
     *
     * @param entity           the entity
     * @param fetchSubEntities flag indicating whether to fetch sub-entities
     * @return the response DTO
     */
    @Override
    public SD toDto(E entity, boolean fetchSubEntities) {
        if (entity == null) {
            return null;
        }

        SD responseDto = createNewResponseDtoInstance();
        responseDto.setId(entity.getId());
        responseDto.setName(entity.getName());
        responseDto.setParentEntityId(entity.getParentId());

        setSubEntities(responseDto, entity, fetchSubEntities);

        return responseDto;
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
     * @param fetchSubEntities flag indicating whether to fetch sub-entities
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
     * @param fetchSubEntities flag indicating whether to fetch sub-entities
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
     * Converts a Page of entities to PaginatedResponseDto containing a list of DTOs.
     *
     * @param page the Page of entities
     * @return the PaginatedResponseDto containing the list of DTOs
     */
    @Override
    public PaginatedResponseDto<SD> toDtoPage(Page<E> page) {
        if (page == null) {
            return null;
        }

        List<SD> content = toDtoList(page.getContent());

        return PaginatedResponseDto.<SD>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
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
        QHierarchyBaseEntity qEntity = QHierarchyBaseEntity.hierarchyBaseEntity;
        List<HierarchyBaseEntity<?>> subEntitiesList = jpaQueryFactory.selectFrom(qEntity)
                .where(qEntity.parentId.eq(entity.getId()))
                .fetch();


        if (shouldFetchSubEntities()) {
            responseDto.setSubEntitiesCount(subEntitiesList.size());
            responseDto.setSubEntities(
                    subEntitiesList.stream()
                            .map(e -> toDto((E) e))
                            .collect(Collectors.toList())
            );
        } else {
            responseDto.setSubEntities(null);
        }
    }

    /**
     * Sets sub-entities (children) in the response DTO with an option to fetch sub-entities.
     *
     * @param responseDto      the response DTO to populate with sub-entities
     * @param entity           the entity from which to fetch sub-entities
     * @param fetchSubEntities flag indicating whether to fetch sub-entities recursively
     */
    @Override
    public void setSubEntities(SD responseDto, E entity, boolean fetchSubEntities) {
        QHierarchyBaseEntity qEntity = QHierarchyBaseEntity.hierarchyBaseEntity;
        List<HierarchyBaseEntity<?>> subEntitiesList = jpaQueryFactory.selectFrom(qEntity)
                .where(qEntity.parentId.eq(entity.getId()))
                .fetch();

        if (shouldFetchSubEntities()) {
            responseDto.setSubEntitiesCount(subEntitiesList.size());

            if (fetchSubEntities) {
                responseDto.setSubEntities(
                        subEntitiesList.stream()
                                .map(e -> toDto((E) e))
                                .collect(Collectors.toList())
                );
            } else {
                responseDto.setSubEntities(
                        subEntitiesList.stream()
                                .map(e -> toDtoWithoutSubEntities((E) e))
                                .collect(Collectors.toList())
                );
            }
        } else {
            responseDto.setSubEntities(null);
        }
    }

    /**
     * Converts an entity to a response DTO without fetching sub-entities.
     *
     * @param entity the entity
     * @return the response DTO
     */
    protected SD toDtoWithoutSubEntities(E entity) {
        if (entity == null) {
            return null;
        }

        SD responseDto = createNewResponseDtoInstance();
        responseDto.setId(entity.getId());
        responseDto.setName(entity.getName());
        responseDto.setParentEntityId(entity.getParentId());

        return responseDto;
    }

    /**
     * Determine whether to fetch sub-entities based on some application-level logic or flag.
     * This method is meant to be overridden by subclasses to implement the logic
     * that decides whether sub-entities should be fetched by default.
     *
     * @return boolean indicating whether to fetch sub-entities
     */
    protected abstract boolean shouldFetchSubEntities();

    /**
     * Creates a new instance of the entity type.
     *
     * @return a new entity instance
     */
    protected abstract E createNewEntityInstance();

    /**
     * Creates a new instance of the response DTO type.
     *
     * @return a new response DTO instance
     */
    protected abstract SD createNewResponseDtoInstance();

    /**
     * Casts an entity to its specific type.
     *
     * @param entity the entity to cast
     * @return the cast entity
     */
    @SuppressWarnings("unchecked")
    public E castToSpecificEntity(HierarchyBaseEntity entity) {
        return (E) entity;
    }
}
