package dev.joao.ledger.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import dev.joao.ledger.domain.Account;
import dev.joao.ledger.domain.LedgerEntry;
import dev.joao.ledger.domain.Money;
import dev.joao.ledger.domain.Transfer;
import dev.joao.ledger.domain.TransferStatus;

public class DepositService {

    private static final String CLIENT_ID = "default-client";
    private final TransactionRunner runner;
    private final Clock clock;
    private final UuidGenerator uuids;

    public DepositService(TransactionRunner runner, Clock clock, UuidGenerator uuids) {
        this.runner = Objects.requireNonNull(runner, "runner");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.uuids = Objects.requireNonNull(uuids, "uuids");
    }

    public Transfer deposit(UUID targetAccountId, Money amount, String idempotencyKey) {
        Objects.requireNonNull(targetAccountId, "targetAccountId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        return runner.run(repositories -> {
            // Find the target account if not found throws an exeception
            Account target = repositories.accounts().findById(targetAccountId)
                    .orElseThrow(() -> new AccountNotFoundException(targetAccountId));

            Account system = repositories.accounts().findSystemAccount(amount.currency()).orElseThrow(
                    () -> new IllegalStateException("System account not found for currency" + amount.currency()));

            Account creditedTarget = target.credit(amount);   
            Account debitedSystem = system.debit(amount);

            Instant now = clock.instant();
            UUID transferId = uuids.generate();

            String hash = calculateHash(targetAccountId, amount);

            Transfer transfer = new Transfer(transferId, CLIENT_ID, idempotencyKey, hash, system.id(), target.id(),
                    amount, TransferStatus.COMPLETED, null, now);

            UUID debitId = uuids.generate();
            UUID creditId = uuids.generate();

            List<LedgerEntry> entries = transfer.toEntries(debitId, creditId, now);
            repositories.transfers().insert(transfer);

            for (LedgerEntry entry : entries) {
                repositories.entries().insert(entry);
            }
            repositories.accounts().updateBalance(system.id(), debitedSystem.balance());
            repositories.accounts().updateBalance(target.id(), creditedTarget.balance());
            return transfer;
        }

        );
    }

    private String calculateHash(UUID targetAccountId, Money amount) {
        String request = targetAccountId + ":" + amount.minorUnits() + ":" + amount.currency().getCurrencyCode();

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(request.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No such algorithm such as SHA-256", e);

        }
    }
}