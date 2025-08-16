package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.tipster.TipsterRequestDTO;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Tipster;
import io.github.joaomnz.bettracker.repository.TipsterRepository;
import org.springframework.stereotype.Service;

@Service
public class TipsterService {
    private final TipsterRepository tipsterRepository;

    public TipsterService(TipsterRepository tipsterRepository) {
        this.tipsterRepository = tipsterRepository;
    }

    public Tipster create(TipsterRequestDTO request, Bettor currentBettor){
        Tipster newTipster = new Tipster();
        newTipster.setName(request.name());
        newTipster.setBettor(currentBettor);
        return tipsterRepository.save(newTipster);
    }
}
