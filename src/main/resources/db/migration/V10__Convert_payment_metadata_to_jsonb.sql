-- Store payment metadata in structured jsonb columns.

ALTER TABLE payment_requests
    ALTER COLUMN papers TYPE jsonb USING
        CASE
            WHEN papers IS NULL OR btrim(papers) = '' THEN NULL
            ELSE papers::jsonb
        END,
    ALTER COLUMN bank_info TYPE jsonb USING
        CASE
            WHEN bank_info IS NULL OR btrim(bank_info) = '' THEN NULL
            ELSE bank_info::jsonb
        END,
    ALTER COLUMN bank_note TYPE jsonb USING
        CASE
            WHEN bank_note IS NULL OR btrim(bank_note) = '' THEN NULL
            ELSE bank_note::jsonb
        END;
