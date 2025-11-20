package com.mousty.gymbro.generic;

import com.mousty.gymbro.pagination.Connection;
import com.mousty.gymbro.pagination.PageInfo;
import com.mousty.gymbro.response.MessageResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
public class GenericService<E,D,M extends GenericMapper<E,D> ,R extends JpaRepository<E, UUID>> {

    private final M mapper;
    private final R repository;

    public Connection<D> getAll(Pageable pageable){
        final Page<E> page = repository.findAll(pageable);
        final List<D> listDTO = page
                .map(mapper::toDTO)
                .toList();
        PageInfo info = new PageInfo(page.hasNext(), page.hasPrevious(),
                page.getNumberOfElements(), page.getTotalPages(), page.getNumber());

        return new Connection<>(listDTO, info, page.getTotalElements());
    }

    public D getById(UUID id, String errorMessage){
        final E t = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(errorMessage));
        return mapper.toDTO(t);
    }

    @Transactional
    public MessageResponse delete(@NotNull UUID id, String errorMessage, String successMessage) {
        if(!repository.existsById(id)) {
            throw new NoSuchElementException(errorMessage);
        }
        repository.deleteById(id);
        return MessageResponse.builder()
                        .message(successMessage)
                        .timestamp(Instant.now())
                .build();
    }

}
