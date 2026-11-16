-- KCS pending queue filters status then orders by submitted time.
CREATE INDEX idx_sample_transfers_status_submitted ON sample_transfers (status, submitted_at);

-- KCS aggregates completed decisions by the time of receipt/rejection.
CREATE INDEX idx_sample_transfers_received_at ON sample_transfers (received_at);
