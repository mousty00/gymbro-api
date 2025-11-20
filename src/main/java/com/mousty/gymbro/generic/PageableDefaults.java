package com.mousty.gymbro.generic;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public enum PageableDefaults {
    INSTANCE;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 15;

    public Pageable create(Integer page, Integer size) {
        return PageRequest.of(
                page != null ? page : DEFAULT_PAGE,
                size != null ? size : DEFAULT_SIZE
        );
    }

    public Pageable createDefault() {
        return PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
    }
}