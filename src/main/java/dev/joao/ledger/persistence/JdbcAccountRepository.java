package dev.joao.ledger.persistence;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import dev.joao.ledger.application.AccountRepository;
import dev.joao.ledger.application.PersistenceException;
import dev.joao.ledger.domain.Account;
import dev.joao.ledger.domain.AccountKind;
import dev.joao.ledger.domain.AccountStatus;
import dev.joao.ledger.domain.Money;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class JdbcAccountRepository implements AccountRepository {

    private final Connection connection;

    public JdbcAccountRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Account account) {
        String sql = """
                    INSERT INTO account
                        (id, owner_ref, currency, kind, status, balance_minor, created_at)
                        VALUES(?,?,?,?,?,?,?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, account.id());
            statement.setString(2, account.ownerRef());
            statement.setString(3, account.balance().currency().getCurrencyCode());
            statement.setString(4, account.kind().name());
            statement.setString(5, account.status().name());
            statement.setLong(6, account.balance().minorUnits());
            statement.setObject(7, OffsetDateTime.ofInstant(account.createdAt(), ZoneOffset.UTC));
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new PersistenceException("Failed to insert a account " + account.id(), e);
        }

    }

    @Override
    public Optional<Account> findById(UUID id) {
        String sql = """
                SELECT * FROM account
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(toAccount(rs));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find account", e);
        }
    }

    @Override
    public Optional<Account> findSystemAccount(Currency currency) {
        String sql = """
                SELECT * FROM account
                 WHERE kind = 'SYSTEM' AND currency = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currency.getCurrencyCode());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(toAccount(rs));
            }

        } catch (SQLException e) {
            throw new PersistenceException("Failed to find account", e);
        }
    }

    @Override
    public void updateBalance(UUID id, Money balance) {
        String sql = """
                UPDATE account
                SET balance_minor = ?
                WHERE id = ? AND currency = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, balance.minorUnits());
            statement.setObject(2, id);
            statement.setString(3, balance.currency().getCurrencyCode());

            int rowsUpdated = statement.executeUpdate();
            if (rowsUpdated == 0) {
                throw new PersistenceException("Account not found or currency dont match", null);
            }

        } catch (SQLException e) {
            throw new PersistenceException("Failed to update the balance", e);
        }
    }

    private Account toAccount(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        String ownerRef = rs.getString("owner_ref");
        Currency currency = Currency.getInstance(rs.getString("currency"));
        AccountKind kind = AccountKind.valueOf(rs.getString("kind"));
        AccountStatus status = AccountStatus.valueOf(rs.getString("status"));
        Money balanceMinor = Money.of(rs.getLong("balance_minor"), currency.getCurrencyCode());
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);

        return new Account(id, ownerRef, kind, status, balanceMinor, createdAt.toInstant());

    }
}
