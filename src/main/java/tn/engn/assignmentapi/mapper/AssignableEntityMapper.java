package tn.engn.assignmentapi.mapper;

import org.springframework.data.domain.Page;
import tn.engn.assignmentapi.dto.AssignableEntityRequestDto;
import tn.engn.assignmentapi.dto.AssignableEntityResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;

import java.util.List;

/**
 * Interface representing a generic mapper for assignable entities.
 *
 * @param <A> the type of the assignable entity
 * @param <R> the type of the request DTO
 * @param <D> the type of the response DTO
 */
public interface AssignableEntityMapper<A, R extends AssignableEntityRequestDto, D extends AssignableEntityResponseDto> {

    /**
     * Converts a request DTO to an entity.
     *
     * @param dto the request DTO
     * @return the entity
     */
    A toEntity(R dto);

    /**
     * Converts an entity to a response DTO.
     *
     * @param entity the entity
     * @return the response DTO
     */
    D toDto(A entity);

    /**
     * Converts a list of entities to a list of response DTOs.
     *
     * @param entities the list of entities
     * @return the list of response DTOs
     */
    List<D> toDtoList(List<A> entities);

    /**
     * Converts a Page of assignable entities to PaginatedResponseDto containing a list of Response DTO.
     * @param page the Page of assignable entities
     * @return the PaginatedResponseDto containing the list of Response DTO
     */
    PaginatedResponseDto<D> toDtoPage(Page<A> page);
}
