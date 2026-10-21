
CREATE UNIQUE INDEX uq_one_system_account_per_currency
    ON account (currency)
    WHERE kind = 'SYSTEM';

INSERT INTO account (id, owner_ref, currency, kind) VALUES
    ('00000000-0000-0000-0000-000000000001', 'SYSTEM', 'EUR', 'SYSTEM'),
    ('00000000-0000-0000-0000-000000000002', 'SYSTEM', 'USD', 'SYSTEM');
