-- Contact message persistence for Supabase/Postgres.
-- Run once in environments that already had TradePilot tables before contact storage.

CREATE TABLE IF NOT EXISTS contact_messages (
    id bigserial PRIMARY KEY,
    name varchar(120) NOT NULL,
    email varchar(180) NOT NULL,
    subject varchar(180) NOT NULL,
    message varchar(5000) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'NEW',
    email_sent boolean NOT NULL DEFAULT false,
    email_error varchar(1000),
    remote_address varchar(120),
    user_agent varchar(500),
    admin_notes varchar(5000),
    lead_source varchar(120),
    replied_at timestamptz,
    last_viewed_at timestamptz,
    reply_body varchar(5000),
    reply_subject varchar(180),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE contact_messages
    ADD COLUMN IF NOT EXISTS admin_notes varchar(5000),
    ADD COLUMN IF NOT EXISTS lead_source varchar(120),
    ADD COLUMN IF NOT EXISTS replied_at timestamptz,
    ADD COLUMN IF NOT EXISTS last_viewed_at timestamptz,
    ADD COLUMN IF NOT EXISTS reply_body varchar(5000),
    ADD COLUMN IF NOT EXISTS reply_subject varchar(180);

UPDATE contact_messages
SET status = 'OPEN'
WHERE status = 'READ';

DO $$
DECLARE
    constraint_record record;
BEGIN
    FOR constraint_record IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'contact_messages'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE contact_messages DROP CONSTRAINT IF EXISTS %I', constraint_record.conname);
    END LOOP;
END $$;

ALTER TABLE contact_messages
    ADD CONSTRAINT chk_contact_messages_status
    CHECK (status IN ('NEW', 'OPEN', 'REPLIED', 'ARCHIVED'));

CREATE INDEX IF NOT EXISTS idx_contact_messages_status
    ON contact_messages (status);

CREATE INDEX IF NOT EXISTS idx_contact_messages_created_at
    ON contact_messages (created_at);

CREATE INDEX IF NOT EXISTS idx_contact_messages_status_created_at
    ON contact_messages (status, created_at);
