package io.github.joaomnz.bettracker.mapper;

import io.github.joaomnz.bettracker.dto.bet.BetResponseDTO;
import io.github.joaomnz.bettracker.dto.bet.CreateBetRequestDTO;
import io.github.joaomnz.bettracker.model.*;
import io.github.joaomnz.bettracker.model.enums.BetStatus;
import io.github.joaomnz.bettracker.model.enums.StakeType;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface BetMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bettor", source = "bettor")
    @Mapping(target = "bookmaker", source = "bookmaker")
    @Mapping(target = "tipster", source = "tipster")
    @Mapping(target = "sport", source = "sport")
    @Mapping(target = "competition", source = "competition")
    Bet toEntity(CreateBetRequestDTO requestDTO,
                 Bookmaker bookmaker,
                 Tipster tipster,
                 Sport sport,
                 Competition competition,
                 Bettor bettor);

    @AfterMapping
    default void applyCreationDefaults(CreateBetRequestDTO requestDTO, @MappingTarget Bet bet){
        if(requestDTO.stakeType() == null) bet.setStakeType(StakeType.VALUE);
        if(requestDTO.status() == null) bet.setStatus(BetStatus.PENDING);
        if(requestDTO.eventDate() == null) bet.setEventDate(LocalDateTime.now());
    }

    @Mapping(target = "bookmaker", source = "bet.bookmaker")
    @Mapping(target = "tipster", source = "bet.tipster")
    @Mapping(target = "sport", source = "bet.sport")
    @Mapping(target = "competition", source = "bet.competition")
    BetResponseDTO toDto(Bet bet);

    default String fromBookmaker(Bookmaker bookmaker) {
        return bookmaker != null ? bookmaker.getName() : null;
    }

    default String fromTipster(Tipster tipster) {
        return tipster != null ? tipster.getName() : null;
    }

    default String fromSport(Sport sport) {
        return sport != null ? sport.getName() : null;
    }

    default String fromCompetition(Competition competition) {
        return competition != null ? competition.getName() : null;
    }
}
