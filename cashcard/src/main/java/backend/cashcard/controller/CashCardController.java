package backend.cashcard.controller;

import backend.cashcard.entity.CashCard;
import backend.cashcard.repository.CashCardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/cashcards")
class CashCardController {

    private final CashCardRepository cashCardRepository;

    /**
     * constructor injection
     * Classes with a single constructor can omit the @Autowired annotation.
     * */
    private CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping("/{id}")
    private ResponseEntity<CashCard> findById(@PathVariable Long id, Principal principal) {
        System.err.println("endpoint reached: http://localhost:8081/cashcards/99");

        //Optional<CashCard> cashCardOptional = cashCardRepository.findById(id);
        //return cashCardOptional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());

        /*
        * principal.getName() will return the username provided from Basic Auth.
        */
        Optional<CashCard> cashCardOptional = Optional.ofNullable(cashCardRepository.findByIdAndOwner(id, principal.getName()));

        if(cashCardOptional.isPresent()){
            return ResponseEntity.ok(cashCardOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /*@GetMapping("allCashCards")
    private ResponseEntity<Iterable<CashCard>> findAll() {
        return ResponseEntity.ok(cashCardRepository.findAll());
    }*/

    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable, Principal principal) {
        int size = pageable.getPageSize();
        int number = pageable.getPageNumber();
        Sort sorting = pageable.getSort();
        String metaData = "?page=" + number + "&size=" + size + "&sort=" + sorting;
        System.err.println("endpoint reached: http://localhost:8081/cashcards" + metaData);

        /*
            Page<CashCard> page = cashCardRepository.findAll(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
                ));
        */

        Page<CashCard> page = cashCardRepository.findByOwner(
                principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
                )
        );
        return ResponseEntity.ok(page.getContent());
    }

    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb, Principal principal) {
        CashCard cashCardWithOwner = new CashCard(newCashCardRequest.getId(), newCashCardRequest.getAmount(), principal.getName());
        //CashCard savedCashCard = cashCardRepository.save(newCashCardRequest);
        CashCard savedCashCardWithOwner = cashCardRepository.save(cashCardWithOwner);
        URI locationOfNewCashCard = ucb
                .path("cashcards/{id}")
                .buildAndExpand(cashCardWithOwner.getId())
                .toUri();

        System.err.println("endpoint reached: "+ locationOfNewCashCard);
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @PutMapping("/{id}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long id, @RequestBody CashCard cashCardUpdate, Principal principal){
        CashCard cashCard = cashCardRepository.findByIdAndOwner(id,principal.getName());
        if(cashCard == null) return ResponseEntity.notFound().build();

        CashCard updatedCashcard = new CashCard(cashCard.getId(), cashCardUpdate.getAmount(),principal.getName());
        cashCardRepository.save(updatedCashcard);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long id, Principal principal) {
        //cashCardRepository.deleteById(id);
        if (cashCardRepository.existsByIdAndOwner(id, principal.getName())) {
            cashCardRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

}
