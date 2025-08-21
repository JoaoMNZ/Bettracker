package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.bookmaker.BookmakerRequestDTO;
import io.github.joaomnz.bettracker.exceptions.ResourceNotFoundException;
import io.github.joaomnz.bettracker.model.Bettor;
import io.github.joaomnz.bettracker.model.Bookmaker;
import io.github.joaomnz.bettracker.repository.BookmakerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookmakerService {
    private final BookmakerRepository bookmakerRepository;

    public BookmakerService(BookmakerRepository bookmakerRepository) {
        this.bookmakerRepository = bookmakerRepository;
    }

    public Bookmaker findByIdAndBettor(Long id, Bettor currentBettor){
        return bookmakerRepository.findByIdAndBettor(id, currentBettor)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmaker not found with id " + id + " for this bettor."));
    }

    public Page<Bookmaker> findAllByBettor(Bettor currentBettor, Pageable pageable){
        return bookmakerRepository.findAllByBettor(currentBettor, pageable);
    }

    public Bookmaker create(BookmakerRequestDTO request, Bettor currentBettor){
        Bookmaker newBookmaker = new Bookmaker();
        newBookmaker.setName(request.name());
        newBookmaker.setBettor(currentBettor);
        return bookmakerRepository.save(newBookmaker);
    }

    public Bookmaker update(Long id, BookmakerRequestDTO request, Bettor currentBettor){
        Bookmaker bookmakerToUpdate = findByIdAndBettor(id, currentBettor);
        bookmakerToUpdate.setName(request.name());
        return bookmakerRepository.save(bookmakerToUpdate);
    }

}
