package dev.joao.ledger.domain;

public class InvalidTransferException extends DomainException {
    public InvalidTransferException(String message){
        super(message);
    }
    
}
