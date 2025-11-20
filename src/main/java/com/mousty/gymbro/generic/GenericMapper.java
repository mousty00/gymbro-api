package com.mousty.gymbro.generic;

import java.util.List;

public interface GenericMapper<ENTITY,DTO> {
    DTO toDTO(ENTITY t);
    ENTITY toEntity(DTO d);


    default List<ENTITY> toEntityList(final List<DTO> dtos) {
        return dtos.stream()
                .map(this::toEntity)
                .toList();
    }

    default List<DTO> toDTOList(final List<ENTITY> entities) {
        return entities.stream()
                .map(this::toDTO)
                .toList();
    }


}
