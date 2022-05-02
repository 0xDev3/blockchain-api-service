DROP TABLE blockchain_api_service.signed_verification_message;
DROP TABLE blockchain_api_service.unsigned_verification_message;

CREATE TABLE blockchain_api_service.erc20_balance_request (
    id                       UUID        PRIMARY KEY,
    chain_id                 BIGINT      NOT NULL,
    redirect_url             VARCHAR     NOT NULL,
    token_address            VARCHAR     NOT NULL,
    block_number             NUMERIC(78),
    requested_wallet_address VARCHAR,
    arbitrary_data           JSON,
    send_screen_title        VARCHAR,
    send_screen_message      VARCHAR,
    send_screen_logo         VARCHAR,
    actual_wallet_address    VARCHAR,
    signed_message           VARCHAR
);
