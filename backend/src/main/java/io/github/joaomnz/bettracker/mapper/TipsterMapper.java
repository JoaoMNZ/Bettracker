package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.tipster.TipsterResponseDTO;
import io.github.joaomnz.bettracker.model.Tipster;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TipsterMapper {
    TipsterResponseDTO toDto(Tipster tipster);
}
