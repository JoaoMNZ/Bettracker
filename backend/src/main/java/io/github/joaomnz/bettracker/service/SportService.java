package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.sport.SportRequestDTO;
import io.github.joaomnz.bettracker.exceptions.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Sport;
import io.github.joaomnz.bettracker.repository.SportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SportService {
    private final SportRepository sportRepository;

    public SportService(SportRepository sportRepository) {
        this.sportRepository = sportRepository;
    }

    public Sport findByIdAndBettor(Long id, Bettor currentBettor){
        return sportRepository.findByIdAndBettor(id, currentBettor)
                .orElseThrow(() -> new ResourceNotFoundException("Sport not found with id " + id + " for this bettor."));
    }

    public Page<Sport> findAllByBettor(Bettor currentBettor, Pageable pageable){
        return sportRepository.findAllByBettor(currentBettor, pageable);
    }

    public Sport create(SportRequestDTO request, Bettor currentBettor){
        Sport newSport = new Sport();
        newSport.setName(request.name());
        newSport.setBettor(currentBettor);
        return sportRepository.save(newSport);
    }

    public Sport update(Long id, SportRequestDTO request, Bettor currentBettor){
        Sport sportToUpdate = findByIdAndBettor(id, currentBettor);

        sportToUpdate.setName(request.name());

        return sportRepository.save(sportToUpdate);
    }
}
