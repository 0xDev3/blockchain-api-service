CREATE TABLE blockchain_api_service.fetch_account_balance_cache (
    id             UUID                     PRIMARY KEY,
    chain_id       BIGINT                   NOT NULL,
    custom_rpc_url VARCHAR                  NOT NULL,
    wallet_address VARCHAR                  NOT NULL,
    block_number   NUMERIC(78)              NOT NULL,
    timestamp      TIMESTAMP WITH TIME ZONE NOT NULL,
    asset_amount   NUMERIC(78)              NOT NULL
);

CREATE UNIQUE INDEX fetch_account_balance_cache_get_index ON blockchain_api_service.fetch_account_balance_cache(
    chain_id, custom_rpc_url, wallet_address, block_number
);

CREATE TABLE blockchain_api_service.fetch_erc20_account_balance_cache (
    id               UUID                     PRIMARY KEY,
    chain_id         BIGINT                   NOT NULL,
    custom_rpc_url   VARCHAR                  NOT NULL,
    contract_address VARCHAR                  NOT NULL,
    wallet_address   VARCHAR                  NOT NULL,
    block_number     NUMERIC(78)              NOT NULL,
    timestamp        TIMESTAMP WITH TIME ZONE NOT NULL,
    asset_amount     NUMERIC(78)              NOT NULL
);

CREATE UNIQUE INDEX fetch_erc20_account_balance_cache_get_index
    ON blockchain_api_service.fetch_erc20_account_balance_cache(
        chain_id, custom_rpc_url, contract_address, wallet_address, block_number
    );

CREATE TABLE blockchain_api_service.fetch_transaction_info_cache (
    id                        UUID                     PRIMARY KEY,
    chain_id                  BIGINT                   NOT NULL,
    custom_rpc_url            VARCHAR                  NOT NULL,
    tx_hash                   VARCHAR                  NOT NULL,
    from_address              VARCHAR                  NOT NULL,
    to_address                VARCHAR                  NOT NULL,
    deployed_contract_address VARCHAR,
    tx_data                   BYTEA                    NOT NULL,
    value_amount              NUMERIC(78)              NOT NULL,
    block_number              NUMERIC(78)              NOT NULL,
    timestamp                 TIMESTAMP WITH TIME ZONE NOT NULL,
    success                   BOOLEAN                  NOT NULL
);

CREATE UNIQUE INDEX fetch_transaction_info_cache_get_index ON blockchain_api_service.fetch_transaction_info_cache(
    chain_id, custom_rpc_url, tx_hash
);
