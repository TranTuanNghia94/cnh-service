-- Add paid percentage for payment request progress display.

ALTER TABLE payment_requests
    ADD COLUMN IF NOT EXISTS paid_percentage DECIMAL(5,2) NOT NULL DEFAULT 0;

UPDATE payment_requests
SET paid_percentage = CASE
    WHEN COALESCE(total_amount, 0) <= 0 THEN 0
    ELSE ROUND((COALESCE(paid_amount, 0) * 100.0 / total_amount)::numeric, 2)
END;
