package dev.joao.ledger.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.postgresql.util.PSQLException;

import dev.joao.ledger.application.DuplicateIdempotencyKeyException;
import dev.joao.ledger.application.PersistenceException;
import dev.joao.ledger.application.TransferRepository;
import dev.joao.ledger.domain.Money;
import dev.joao.ledger.domain.Transfer;
import dev.joao.ledger.domain.TransferStatus;

public class JdbcTransferRepository implements TransferRepository {
    private final Connection connection;

    public JdbcTransferRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Transfer transfer) {
        String sql = """
                INSERT INTO transfer
                (id, client_id, idempotency_key, request_hash, source_account_id, dest_account_id, amount_minor, currency, status, failure_reason, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, transfer.id());
            statement.setString(2, transfer.clientId());
            statement.setString(3, transfer.idempotencyKey());
            statement.setString(4, transfer.requestHash());
            statement.setObject(5, transfer.sourceAccountId());
            statement.setObject(6, transfer.destAccountId());
            statement.setLong(7, transfer.amount().minorUnits());
            statement.setString(8, transfer.amount().currency().getCurrencyCode());
            statement.setString(9, transfer.status().name());
            statement.setString(10, transfer.failureReason());
            statement.setObject(11, OffsetDateTime.ofInstant(transfer.createdAt(), ZoneOffset.UTC));
            statement.executeUpdate();

        } catch (SQLException e) {
            if (isIdempotencyKeyViolation(e)) {
                throw new DuplicateIdempotencyKeyException(transfer.clientId(), transfer.idempotencyKey());
            }
            throw new PersistenceException("Failed to insert transfer " + transfer.id(), e);
        }
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        String sql = """
                SELECT * FROM transfer
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(toTransfer(rs));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to find transfer" + id, e);
        }
    }

    private Transfer toTransfer(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        String clientId = rs.getString("client_id");
        String idempotencyKey = rs.getString("idempotency_key");
        String requestHash = rs.getString("request_hash");
        UUID sourceAccountId = rs.getObject("source_account_id", UUID.class);
        UUID destAccountId = rs.getObject("dest_account_id", UUID.class);
        Money amount = Money.of(rs.getLong("amount_minor"), rs.getString("currency"));
        TransferStatus status = TransferStatus.valueOf(rs.getString("status"));
        String failureReason = rs.getString("failure_reason");
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);

        return new Transfer(id, clientId, idempotencyKey, requestHash, sourceAccountId, destAccountId, amount,
                status, failureReason, createdAt.toInstant());

    }

    // to make sure that the idempotencykey is not duplicated the 23505 is a uniqueness violation
    // since the primary key also gives the 23505, and they are not the same we separete them.
    private static boolean isIdempotencyKeyViolation(SQLException e) {
        return "23505".equals(e.getSQLState())
                && e instanceof PSQLException pg
                && pg.getServerErrorMessage() != null
                && "uq_transfer_idempotency".equals(pg.getServerErrorMessage().getConstraint());
    }

}
