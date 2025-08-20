package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.tipster.TipsterRequestDTO;
import io.github.joaomnz.bettracker.exceptions.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Tipster;
import io.github.joaomnz.bettracker.repository.TipsterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TipsterService {
    private final TipsterRepository tipsterRepository;

    public TipsterService(TipsterRepository tipsterRepository) {
        this.tipsterRepository = tipsterRepository;
    }

    public Tipster findByIdAndBettor(Long id, Bettor curentBettor){
        return tipsterRepository.findByIdAndBettor(id, curentBettor)
                .orElseThrow(() -> new ResourceNotFoundException("Tipster not found with id " + id + " for this bettor."));
    }

    public Page<Tipster> findAllByBettor(Bettor currentBettor, Pageable pageable){
        return tipsterRepository.findAllByBettor(currentBettor, pageable);
    }

    public Tipster create(TipsterRequestDTO request, Bettor currentBettor){
        Tipster newTipster = new Tipster();
        newTipster.setName(request.name());
        newTipster.setBettor(currentBettor);
        return tipsterRepository.save(newTipster);
    }

    public Tipster update(Long id, TipsterRequestDTO request, Bettor currentBettor){
        Tipster tipsterToUpdate = findByIdAndBettor(id, currentBettor);

        tipsterToUpdate.setName(request.name());

        return tipsterRepository.save(tipsterToUpdate);
    }

    public void delete(Long id, Bettor currentBettor){
        Tipster tipsterToDelete = findByIdAndBettor(id, currentBettor);

        tipsterRepository.delete(tipsterToDelete);
    }
}
