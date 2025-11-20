package com.mousty.gymbro.pagination;

import lombok.Builder;

@Builder
public record PageInfo(boolean hasNext,
                       boolean hasPrevious,
                       int numberOfElements,
                       int totalPages,
                       int currentPage) {
}