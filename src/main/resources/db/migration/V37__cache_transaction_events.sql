CREATE TYPE blockchain_api_service.EVENT_LOG AS (
    log_data   VARCHAR,
    log_topics VARCHAR[]
);

DELETE FROM blockchain_api_service.fetch_transaction_info_cache WHERE 1 = 1;

ALTER TABLE blockchain_api_service.fetch_transaction_info_cache
    ADD COLUMN event_logs EVENT_LOG[] NOT NULL DEFAULT '{}';
