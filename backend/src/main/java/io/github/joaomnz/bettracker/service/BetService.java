package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.bet.CreateBetRequestDTO;
import io.github.joaomnz.bettracker.model.*;
import io.github.joaomnz.bettracker.model.enums.BetStatus;
import io.github.joaomnz.bettracker.model.enums.StakeType;
import io.github.joaomnz.bettracker.repository.BetRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BetService {
    private final BetRepository betRepository;

    public BetService(BetRepository betRepository) {
        this.betRepository = betRepository;
    }

    public Bet create(CreateBetRequestDTO request,
                      Bookmaker associatedBookmaker,
                      Tipster associatedTipster,
                      Sport associatedSport,
                      Competition associatedCompetition,
                      Bettor currentBettor){
        StakeType stakeType = request.stakeType() != null ? request.stakeType() : StakeType.VALUE;
        BetStatus status = request.status() != null ? request.status() : BetStatus.PENDING;
        LocalDateTime eventDate = request.eventDate() != null ? request.eventDate() : LocalDateTime.now();

        Bet newBet = new Bet();
        newBet.setTitle(request.title());
        newBet.setSelection(request.selection());
        newBet.setStake(request.stake());
        newBet.setStakeType(stakeType);
        newBet.setOdds(request.odds());
        newBet.setStatus(status);
        newBet.setEventDate(eventDate);
        newBet.setBettor(currentBettor);
        newBet.setBookmaker(associatedBookmaker);
        newBet.setTipster(associatedTipster);
        newBet.setSport(associatedSport);
        newBet.setCompetition(associatedCompetition);

        return betRepository.save(newBet);
    }
}
