CREATE TABLE blockchain_api_service.erc20_lock_request (
    id                           UUID        PRIMARY KEY,
    chain_id                     BIGINT      NOT NULL,
    redirect_url                 VARCHAR     NOT NULL,
    token_address                VARCHAR     NOT NULL,
    token_amount                 NUMERIC(78) NOT NULL,
    lock_contract_address        VARCHAR     NOT NULL,
    token_sender_address         VARCHAR,
    arbitrary_data               JSON,
    tx_hash                      VARCHAR,
    screen_before_action_message VARCHAR,
    screen_after_action_message  VARCHAR
);
