CREATE TABLE blockchain_api_service.contract_deployment_transaction_cache (
    id               UUID        PRIMARY KEY,
    chain_id         BIGINT      NOT NULL,
    custom_rpc_url   VARCHAR     NOT NULL,
    contract_address VARCHAR     NOT NULL,
    tx_hash          VARCHAR,
    from_address     VARCHAR,
    tx_data          BYTEA,
    value_amount     NUMERIC(78),
    contract_binary  BYTEA       NOT NULL,
    block_number     NUMERIC(78),
    event_logs       EVENT_LOG[] NOT NULL
);

CREATE UNIQUE INDEX contract_deployment_transaction_cache_get_index
    ON blockchain_api_service.contract_deployment_transaction_cache(chain_id, custom_rpc_url, contract_address);
