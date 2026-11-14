package com.example.app.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort
) {
    public static <T> PagedResponse<T> from(Page<T> source, String sort) {
        return new PagedResponse<>(source.getContent(), source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages(), source.isFirst(), source.isLast(), sort);
    }
}
