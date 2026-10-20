package dev.joao.ledger.domain;

public class AccountNotActiveException extends DomainException{
    public AccountNotActiveException(String message){
        super(message);
    } 
}
