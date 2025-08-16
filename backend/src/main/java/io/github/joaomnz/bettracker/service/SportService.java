package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.sport.SportRequestDTO;
import io.github.joaomnz.bettracker.exceptions.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Sport;
import io.github.joaomnz.bettracker.repository.SportRepository;
import org.springframework.stereotype.Service;

@Service
public class SportService {
    private final SportRepository sportRepository;

    public SportService(SportRepository sportRepository) {
        this.sportRepository = sportRepository;
    }

    public Sport create(SportRequestDTO request, Bettor currentBettor){
        Sport newSport = new Sport();
        newSport.setName(request.name());
        newSport.setBettor(currentBettor);
        return sportRepository.save(newSport);
    }

    public Sport findByIdAndBettor(Long sportId, Bettor currentBettor){
        return sportRepository.findByIdAndBettor(sportId, currentBettor)
                .orElseThrow(() -> new ResourceNotFoundException("Sport not found with id " + sportId + " for this bettor."));
    }
}
