package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerRequestDTO;
import io.github.joaomnz.bettracker.exceptions.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.repository.BookmakerRepository;
import org.springframework.stereotype.Service;

@Service
public class BookmakerService {
    private final BookmakerRepository bookmakerRepository;

    public BookmakerService(BookmakerRepository bookmakerRepository) {
        this.bookmakerRepository = bookmakerRepository;
    }

    public Bookmaker create(BookmakerRequestDTO request, Bettor currentBettor){
        Bookmaker newBookmaker = new Bookmaker();
        newBookmaker.setName(request.name());
        newBookmaker.setBettor(currentBettor);
        return bookmakerRepository.save(newBookmaker);
    }

    public Bookmaker findByIdAndBettor(Long bookmakerId, Bettor currentBettor){
        return bookmakerRepository.findByIdAndBettor(bookmakerId, currentBettor)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmaker not found with id " + bookmakerId + " for this bettor."));
    }
}
