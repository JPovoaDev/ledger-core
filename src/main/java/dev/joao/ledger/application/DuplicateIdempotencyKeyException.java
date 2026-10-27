package dev.joao.ledger.application;

public class DuplicateIdempotencyKeyException  extends RuntimeException{
    public DuplicateIdempotencyKeyException(String clientId, String idempotencyKey){
        super("Idempotency key already used by client " + clientId + ": " + idempotencyKey); 
    }
}
