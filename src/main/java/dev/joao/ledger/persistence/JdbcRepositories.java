package dev.joao.ledger.persistence;

import java.sql.Connection;

import dev.joao.ledger.application.AccountRepository;
import dev.joao.ledger.application.LedgerEntryRepository;
import dev.joao.ledger.application.Repositories;
import dev.joao.ledger.application.TransferRepository;

public final class JdbcRepositories implements Repositories {
    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerEntryRepository entries;

    public JdbcRepositories(Connection connection) {
        this.accounts = new JdbcAccountRepository(connection);
        this.transfers = new JdbcTransferRepository(connection);
        this.entries = new JdbcLedgerEntryRepository(connection);
    }

    @Override 
    public AccountRepository accounts(){
        return accounts;
    }

    @Override
    public TransferRepository transfers() {
        return transfers;
    }

    @Override
    public LedgerEntryRepository entries() {
        return entries;
    }
    

}