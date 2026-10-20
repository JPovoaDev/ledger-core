package dev.joao.ledger.domain;

public class InsufficientFundsException extends DomainException {
    public InsufficientFundsException (String message){
        super(message);
    }
    
}
