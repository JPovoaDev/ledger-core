CREATE TABLE account (
    id            UUID        PRIMARY KEY,
    owner_ref     TEXT        NOT NULL,
    currency      CHAR(3)     NOT NULL,
    kind          TEXT        NOT NULL DEFAULT 'USER'
                  CHECK (kind IN ('USER', 'SYSTEM')),
    status        TEXT        NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    balance_minor BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT balance_non_negative_for_users
        CHECK (kind = 'SYSTEM' OR balance_minor >= 0)
);

CREATE TABLE transfer (
    id                UUID        PRIMARY KEY,
    client_id         TEXT        NOT NULL,
    idempotency_key   TEXT        NOT NULL,
    request_hash      TEXT        NOT NULL,
    source_account_id UUID        NOT NULL REFERENCES account (id),
    dest_account_id   UUID        NOT NULL REFERENCES account (id),
    amount_minor      BIGINT      NOT NULL CHECK (amount_minor > 0),
    currency          CHAR(3)     NOT NULL,
    status            TEXT        NOT NULL
                      CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    failure_reason    TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_transfer_idempotency UNIQUE (client_id, idempotency_key),
    CONSTRAINT chk_distinct_accounts CHECK (source_account_id <> dest_account_id)
);

CREATE TABLE ledger_entry (
    id           UUID        PRIMARY KEY,
    transfer_id  UUID        NOT NULL REFERENCES transfer (id),
    account_id   UUID        NOT NULL REFERENCES account (id),
    direction    TEXT        NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount_minor BIGINT      NOT NULL CHECK (amount_minor > 0),
    currency     CHAR(3)     NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_entry_account ON ledger_entry (account_id, created_at, id);
CREATE INDEX idx_ledger_entry_transfer ON ledger_entry (transfer_id);