package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.competition.CompetitionRequestDTO;
import io.github.joaomnz.bettracker.exceptions.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.Competition;
import io.github.joaomnz.bettracker.model.Sport;
import io.github.joaomnz.bettracker.repository.CompetitionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CompetitionService {
    private final CompetitionRepository competitionRepository;

    public CompetitionService(CompetitionRepository competitionRepository) {
        this.competitionRepository = competitionRepository;
    }

    public Competition findByIdAndSport(Long id, Sport parentSport){
        return competitionRepository.findByIdAndSport(id, parentSport)
                .orElseThrow(() -> new ResourceNotFoundException("Competition not found with id " + id + " for this sport."));
    }

    public Page<Competition> findAllBySport(Sport parentSport, Pageable pageable){
        return competitionRepository.findAllBySport(parentSport, pageable);
    }

    public Competition create(CompetitionRequestDTO request, Sport parentSport){
        Competition newCompetition = new Competition();
        newCompetition.setName(request.name());
        newCompetition.setSport(parentSport);
        newCompetition.setBettor(parentSport.getBettor());
        return competitionRepository.save(newCompetition);
    }

    public Competition update(Long id, CompetitionRequestDTO request, Sport parentSport){
        Competition competitionToUpdate = findByIdAndSport(id, parentSport);
        competitionToUpdate.setName(request.name());
        return competitionRepository.save(competitionToUpdate);
    }
}
