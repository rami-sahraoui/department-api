package tn.engn.hierarchicalentityapi.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents a paginated response containing a list of content, pagination details, and total elements.
 *
 * @param <T> the type of content in the paginated response
 */
@Data
@Builder
public class PaginatedResponseDto<T> {

    /**
     * The content (list of items) of the current page.
     */
    private List<T> content;

    /**
     * The number of the current page.
     */
    private int page;

    /**
     * The size of the current page.
     */
    private int size;

    /**
     * The total number of elements across all pages.
     */
    private long totalElements;

    /**
     * The total number of pages.
     */
    private int totalPages;
}
