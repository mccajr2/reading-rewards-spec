-- Migrate reward_options to typed value model (MONEY / NON_MONEY)

-- Drop old amount-positive constraint before renaming the column
ALTER TABLE reward_options DROP CONSTRAINT IF EXISTS reward_option_amount_positive;

-- Rename legacy `amount` column to `money_amount`
ALTER TABLE reward_options RENAME COLUMN amount TO money_amount;

-- Make money_amount nullable (required only for MONEY type)
ALTER TABLE reward_options ALTER COLUMN money_amount DROP NOT NULL;

-- Add value type discriminator column, default existing rows to MONEY
ALTER TABLE reward_options ADD COLUMN value_type VARCHAR(10) NOT NULL DEFAULT 'MONEY';

-- Add currency code for monetary rewards; backfill existing rows
ALTER TABLE reward_options ADD COLUMN currency_code VARCHAR(10);
UPDATE reward_options SET currency_code = 'USD' WHERE value_type = 'MONEY';

-- Add non-monetary value fields
ALTER TABLE reward_options ADD COLUMN non_money_quantity DOUBLE PRECISION;
ALTER TABLE reward_options ADD COLUMN non_money_unit_label VARCHAR(40);
