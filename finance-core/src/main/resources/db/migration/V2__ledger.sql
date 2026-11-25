-- Tighten accounts (V1 had no checks)
ALTER TABLE accounts ADD CONSTRAINT chk_account_type
    CHECK (account_type IN ('WALLET','SYSTEM_FUNDING'));
ALTER TABLE accounts ADD CONSTRAINT chk_account_owner
    CHECK ((account_type = 'WALLET' AND owner_user_id IS NOT NULL)
        OR (account_type = 'SYSTEM_FUNDING' AND owner_user_id IS NULL));
ALTER TABLE accounts ADD CONSTRAINT chk_account_currency CHECK (currency = 'INR');

-- The one system account that issues simulated money (fixed id so code can find it)
INSERT INTO accounts (id, owner_user_id, account_type, currency, status)
VALUES ('00000000-0000-0000-0000-000000000001', NULL, 'SYSTEM_FUNDING', 'INR', 'ACTIVE');

-- Journals
CREATE TABLE ledger_transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference        VARCHAR(100) NOT NULL UNIQUE,      -- business reference, blocks double posting
    transaction_type VARCHAR(30)  NOT NULL,
    status           VARCHAR(20)  NOT NULL CHECK (status IN ('POSTED')),
    currency         CHAR(3)      NOT NULL CHECK (currency = 'INR'),
    description      VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    posted_at        TIMESTAMPTZ  NOT NULL,
    reversal_of_id   UUID REFERENCES ledger_transactions(id)
);

-- Entries
CREATE TABLE ledger_entries (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ledger_transaction_id UUID        NOT NULL REFERENCES ledger_transactions(id),
    account_id            UUID        NOT NULL REFERENCES accounts(id),
    direction             VARCHAR(6)  NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    amount_minor          BIGINT      NOT NULL CHECK (amount_minor > 0),   -- paise, always positive
    currency              CHAR(3)     NOT NULL CHECK (currency = 'INR'),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_entries_account ON ledger_entries(account_id);
CREATE INDEX idx_entries_txn     ON ledger_entries(ledger_transaction_id);

-- Protection 1: append-only
CREATE FUNCTION prevent_ledger_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Ledger is append-only: % on % is not allowed', TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entries_immutable
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION prevent_ledger_mutation();

CREATE TRIGGER trg_ledger_transactions_immutable
    BEFORE UPDATE OR DELETE ON ledger_transactions
    FOR EACH ROW EXECUTE FUNCTION prevent_ledger_mutation();

-- Protection 2: every journal must balance, checked at COMMIT
CREATE FUNCTION check_journal_balanced() RETURNS trigger AS $$
DECLARE
    diff BIGINT;
    cnt  INT;
BEGIN
    SELECT COALESCE(SUM(CASE WHEN direction = 'DEBIT' THEN amount_minor ELSE -amount_minor END), 0),
           COUNT(*)
      INTO diff, cnt
      FROM ledger_entries
     WHERE ledger_transaction_id = NEW.ledger_transaction_id;

    IF cnt < 2 OR diff <> 0 THEN
        RAISE EXCEPTION 'Journal % is not balanced (entries=%, debits-credits=%)',
            NEW.ledger_transaction_id, cnt, diff;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_journal_balanced
    AFTER INSERT ON ledger_entries
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_journal_balanced();