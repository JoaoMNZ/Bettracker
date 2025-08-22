package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.sport.SportResponseDTO;
import io.github.joaomnz.bettracker.model.Sport;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SportMapper {
    SportResponseDTO toDto(Sport sport);
}
