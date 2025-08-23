package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.competition.CompetitionResponseDTO;
import io.github.joaomnz.bettracker.model.Competition;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompetitionMapper {
    CompetitionResponseDTO toDto(Competition competition);
}
