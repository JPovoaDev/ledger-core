package dev.joao.ledger.integration;

import java.time.Instant;
import java.util.UUID;

import dev.joao.ledger.domain.Account;
import dev.joao.ledger.domain.AccountKind;
import dev.joao.ledger.domain.AccountStatus;
import dev.joao.ledger.domain.Money;
import dev.joao.ledger.domain.Transfer;
import dev.joao.ledger.domain.TransferStatus;

final class TestFixtures {
    static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private TestFixtures() {
    }

    static Account userAccount(String currency, long balanceMinor) {
        return new Account(UUID.randomUUID(), "owner-1", AccountKind.USER,
                AccountStatus.ACTIVE, Money.of(balanceMinor, currency), NOW);
    }

    static Transfer transfer(UUID source, UUID dest, String clientId, String idempotencyKey) {
        return new Transfer(UUID.randomUUID(), clientId, idempotencyKey, "hash-" + idempotencyKey,
                source, dest, Money.of(500, "EUR"), TransferStatus.COMPLETED, null, NOW);
    }
}