package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerResponseDTO;
import io.github.joaomnz.bettracker.model.Bookmaker;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookmakerMapper {
    BookmakerResponseDTO toDto(Bookmaker bookmaker);
}
