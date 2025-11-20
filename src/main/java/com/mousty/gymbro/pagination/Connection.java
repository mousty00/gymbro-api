package com.mousty.gymbro.pagination;

import lombok.Builder;

import java.util.List;

@Builder
public record Connection<D>(
        List<D> results,
        PageInfo pageInfo,
        Long totalCount
) { }
