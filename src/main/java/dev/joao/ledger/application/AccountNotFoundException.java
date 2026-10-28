package dev.joao.ledger.application;

import java.util.UUID;

public final class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID accountId) {
        super("Account not found: " + accountId);
    }
}   