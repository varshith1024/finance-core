CREATE TABLE transfers (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_account_id      UUID        NOT NULL REFERENCES accounts(id),
    destination_account_id UUID        NOT NULL REFERENCES accounts(id),
    amount_minor           BIGINT      NOT NULL CHECK (amount_minor > 0),
    currency               CHAR(3)     NOT NULL CHECK (currency = 'INR'),
    status                 VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','COMPLETED','FAILED')),
    failure_reason         VARCHAR(50),
    idempotency_key        VARCHAR(100) NOT NULL,
    created_by_user_id     UUID        NOT NULL REFERENCES users(id),
    ledger_transaction_id  UUID UNIQUE REFERENCES ledger_transactions(id),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (source_account_id <> destination_account_id),
    -- COMPLETED if and only if money actually moved in the ledger
    CHECK ((status = 'COMPLETED') = (ledger_transaction_id IS NOT NULL))
);
CREATE INDEX idx_transfers_source  ON transfers(source_account_id);
CREATE INDEX idx_transfers_creator ON transfers(created_by_user_id);

CREATE TABLE idempotency_records (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope         VARCHAR(100) NOT NULL,          -- the user id: keys are per user
    idem_key      VARCHAR(100) NOT NULL,
    request_hash  VARCHAR(64)  NOT NULL,          -- SHA-256 of the request body
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('IN_PROGRESS','COMPLETED')),
    response_code INT,
    resource_id   UUID,                           -- the transfer this key produced
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_idempotency_scope_key UNIQUE (scope, idem_key)
);

CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ,
    attempts       INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published_at IS NULL;