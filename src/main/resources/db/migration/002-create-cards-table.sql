--liquibase formatted sql

--changeset bank:002-create-cards-table
CREATE TABLE cards (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    pan_cipher      TEXT            NOT NULL,
    expiry_date     DATE            NOT NULL,
    status          VARCHAR(32)     NOT NULL,
    balance         NUMERIC(19, 2)  NOT NULL DEFAULT 0.00,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cards_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_cards_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'EXPIRED')),
    CONSTRAINT ck_cards_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_cards_user_id ON cards (user_id);
CREATE INDEX idx_cards_status ON cards (status);
