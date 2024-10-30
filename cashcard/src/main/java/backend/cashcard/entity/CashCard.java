package backend.cashcard.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class CashCard {
    @Id Long id;
    Double amount;
    String owner;

    public CashCard(Long id, Double amount) {
        this.id = id;
        this.amount = amount;
    }

    public CashCard(Long id, Double amount, String owner) {
        this.id = id;
        this.amount = amount;
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public CashCard() {
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "CashCard{" +
                "id=" + id +
                ", amount=" + amount +
                '}';
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}

/*
*  Java records automatically generate a canonical constructor, but Spring Data JPA
*  requires a no-argument (default) constructor to instantiate entities
*  when fetching from the database. Since records don’t generate a
*  default constructor, you’ll encounter this error.
* */
