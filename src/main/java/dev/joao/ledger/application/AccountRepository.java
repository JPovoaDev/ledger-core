package dev.joao.ledger.application;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import dev.joao.ledger.domain.Account;
import dev.joao.ledger.domain.Money;

public interface AccountRepository {
    void insert(Account account);

    Optional<Account> findById(UUID id);
    Optional<Account> findSystemAccount(Currency currency);

    void updateBalance(UUID id, Money amount);
}