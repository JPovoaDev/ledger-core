package dev.joao.ledger.domain;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}