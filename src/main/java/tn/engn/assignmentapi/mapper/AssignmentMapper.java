package tn.engn.assignmentapi.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.engn.assignmentapi.dto.*;
import tn.engn.assignmentapi.model.AssignableEntity;
import tn.engn.assignmentapi.model.Assignment;
import tn.engn.hierarchicalentityapi.dto.HierarchyRequestDto;
import tn.engn.hierarchicalentityapi.dto.HierarchyResponseDto;
import tn.engn.hierarchicalentityapi.dto.PaginatedResponseDto;
import tn.engn.hierarchicalentityapi.model.HierarchyBaseEntity;

import java.util.List;

/**
 * Interface for mapping between Assignment entities and their corresponding DTOs.
 * This interface defines methods to convert between Assignment entities and DTOs,
 * as well as handling collections and paginated results.
 *
 * @param <HE> the type of the hierarchical entity
 * @param <AE> the type of the assignable entity
 * @param <HR> the type of the hierarchical entity request DTO
 * @param <H> the type of the hierarchical entity response DTO
 * @param <R>  the type of the assignable entity request DTO
 * @param <D> the type of the assignable entity response DTO
 */
public interface AssignmentMapper<
        HE extends HierarchyBaseEntity<HE>,
        AE extends AssignableEntity<AE>,
        HR extends HierarchyRequestDto,
        H extends HierarchyResponseDto,
        R extends AssignableEntityRequestDto,
        D extends AssignableEntityResponseDto> {

    /**
     * Converts an AssignmentRequestDto to an Assignment entity.
     * This method maps the fields from the DTO to the corresponding entity fields.
     *
     * @param dto the AssignmentRequestDto containing the data to be mapped
     * @return the corresponding Assignment entity
     */
    Assignment<HE, AE> toEntity(AssignmentRequestDto dto);

    /**
     * Converts an Assignment entity to an AssignmentResponseDto.
     * This method maps the fields from the entity to the corresponding DTO fields.
     *
     * @param assignment the Assignment entity to be converted
     * @return the corresponding AssignmentResponseDto
     */
    AssignmentResponseDto<H, D> toDto(Assignment<HE, AE> assignment);

    /**
     * Converts a list of Assignment entities to a list of AssignmentResponseDto.
     * This method is useful when converting multiple entities at once.
     *
     * @param assignments the list of Assignment entities to convert
     * @return the corresponding list of AssignmentResponseDto
     */
    List<AssignmentResponseDto<H, D>> toDtoList(List<Assignment<HE, AE>> assignments);

    /**
     * Converts a list of Assignment entities to a PaginatedResponseDto.
     * This method is useful when dealing with paginated data and needs to include pagination metadata.
     *
     * @param assignments    the list of Assignment entities to convert
     * @param pageable       the pagination information
     * @param totalElements  the total number of elements across all pages
     * @return the PaginatedResponseDto containing the list of AssignmentResponseDto and pagination details
     */
    PaginatedResponseDto<AssignmentResponseDto<H, D>> toPaginatedDtoList(List<Assignment<HE, AE>> assignments,
                                                                         Pageable pageable,
                                                                         long totalElements);

    /**
     * Converts a Page of Assignment entities to PaginatedResponseDto containing a list of AssignmentResponseDto.
     * This method simplifies the conversion of paginated data directly from the repository.
     *
     * @param page the Page of Assignment entities
     * @return the PaginatedResponseDto containing the list of AssignmentResponseDto and pagination details
     */
    PaginatedResponseDto<AssignmentResponseDto<H, D>> toDtoPage(Page<Assignment<HE, AE>> page);
}
