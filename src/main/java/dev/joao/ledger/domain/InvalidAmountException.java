package dev.joao.ledger.domain;

public class InvalidAmountException extends DomainException {
    public InvalidAmountException(String message){
        super(message);
    }    
}
