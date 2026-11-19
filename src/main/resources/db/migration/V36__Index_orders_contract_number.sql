-- Hot lookup for warehouse outbound create-by-contract and contract filters
CREATE INDEX IF NOT EXISTS idx_orders_contract_number ON orders (contract_number);
