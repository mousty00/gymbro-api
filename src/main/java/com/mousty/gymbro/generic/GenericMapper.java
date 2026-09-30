package com.mousty.gymbro.generic;

import java.util.List;

public interface GenericMapper<ENTITY,DTO> {
    DTO toDTO(ENTITY t);
    ENTITY toEntity(DTO d);


    default List<ENTITY> toEntityList(final List<DTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(this::toEntity)
                .toList();
    }

    default List<DTO> toDTOList(final List<ENTITY> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }


}
