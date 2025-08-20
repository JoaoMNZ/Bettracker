package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.bet.CreateBetRequestDTO;
import io.github.joaomnz.bettracker.dto.bet.UpdateBetRequestDTO;
import io.github.joaomnz.bettracker.exceptions.ResourceNotFoundException;
import io.github.joaomnz.bettracker.mapper.BetMapper;
import io.github.joaomnz.bettracker.model.*;
import io.github.joaomnz.bettracker.repository.BetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BetService {
    private final BetRepository betRepository;

    private final BetMapper betMapper;

    public BetService(BetRepository betRepository, BetMapper betMapper) {
        this.betRepository = betRepository;
        this.betMapper = betMapper;
    }

    public Bet findByIdAndBettor(Long id, Bettor currentBettor){
        return betRepository.findByIdAndBettor(id, currentBettor)
                .orElseThrow(() -> new ResourceNotFoundException("Bet not found with id " + id + " for this bettor."));
    }

    public Page<Bet> findAllByBettor(Bettor currentBettor, Pageable pageable){
        return betRepository.findAllByBettor(currentBettor, pageable);
    }

    public Bet create(CreateBetRequestDTO request,
                      Bookmaker associatedBookmaker,
                      Tipster associatedTipster,
                      Sport associatedSport,
                      Competition associatedCompetition,
                      Bettor currentBettor){
        Bet newBet = betMapper.toEntity(request, associatedBookmaker, associatedTipster, associatedSport, associatedCompetition, currentBettor);
        return betRepository.save(newBet);
    }

    public Bet update(Long betId,
                       UpdateBetRequestDTO request,
                       Bookmaker associatedBookmaker,
                       Tipster associatedTipster,
                       Sport associatedSport,
                       Competition associatedCompetition,
                       Bettor currentBettor){
        Bet betToUpdate = findByIdAndBettor(betId, currentBettor);

        if(request.title() != null) betToUpdate.setTitle(request.title());
        if(request.selection() != null) betToUpdate.setSelection(request.selection());
        if(request.stake() != null) betToUpdate.setStake(request.stake());
        if(request.stakeType() != null) betToUpdate.setStakeType(request.stakeType());
        if(request.odds() != null) betToUpdate.setOdds(request.odds());
        if(request.status() != null) betToUpdate.setStatus(request.status());
        if(request.eventDate() != null) betToUpdate.setEventDate(request.eventDate());
        if(associatedBookmaker != null) betToUpdate.setBookmaker(associatedBookmaker);
        if(associatedTipster != null) betToUpdate.setTipster(associatedTipster);
        if(associatedSport != null) betToUpdate.setSport(associatedSport);
        if(associatedCompetition != null) betToUpdate.setCompetition(associatedCompetition);

        return betRepository.save(betToUpdate);
    }

    public void delete(Long id, Bettor currentBettor){
        Bet betToDelete = findByIdAndBettor(id, currentBettor);

        betRepository.delete(betToDelete);
    }
}
