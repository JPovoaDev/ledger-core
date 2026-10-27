package dev.joao.ledger.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import dev.joao.ledger.application.LedgerEntryRepository;
import dev.joao.ledger.application.PersistenceException;
import dev.joao.ledger.domain.Direction;
import dev.joao.ledger.domain.LedgerEntry;
import dev.joao.ledger.domain.Money;

public class JdbcLedgerEntryRepository implements LedgerEntryRepository {

    private final Connection connection;

    public JdbcLedgerEntryRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(LedgerEntry entry) {
        String sql = """
                INSERT INTO ledger_entry
                (id, transfer_id, account_id, direction, amount_minor, currency, created_at)
                VALUES (?,?,?,?,?,?,?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, entry.id());
            statement.setObject(2, entry.transferId());
            statement.setObject(3, entry.accountId());
            statement.setString(4, entry.direction().name());
            statement.setLong(5, entry.amount().minorUnits());
            statement.setString(6, entry.amount().currency().getCurrencyCode());
            statement.setObject(7, OffsetDateTime.ofInstant(entry.createdAt(), ZoneOffset.UTC));
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new PersistenceException("Failed to insert a entry", e);
        }

    }

    @Override
    public List<LedgerEntry> findByTransferId(UUID transferId) {

        String sql = """
                SELECT id, transfer_id, account_id, direction, amount_minor, currency, created_at
                FROM ledger_entry
                WHERE transfer_id = ?
                ORDER BY created_at, id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transferId);

            ArrayList<LedgerEntry> rsList = new ArrayList<LedgerEntry>();

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rsList.add(toEntry(rs));
                }
                return rsList;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find entry", e);
        }
    }

    private LedgerEntry toEntry(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        UUID transferId = rs.getObject("transfer_id", UUID.class);
        UUID accountId = rs.getObject("account_id", UUID.class);
        Direction direction = Direction.valueOf(rs.getString("direction"));
        Money amount = Money.of(rs.getLong("amount_minor"), rs.getString("currency"));
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);

        return new LedgerEntry(id, transferId, accountId, direction, amount, createdAt.toInstant());

    }

}
