CREATE TABLE blockchain_api_service.send_erc20_request (
    id                  UUID        PRIMARY KEY,
    chain_id            BIGINT      NOT NULL,
    redirect_url        VARCHAR     NOT NULL,
    token_address       VARCHAR     NOT NULL,
    amount              NUMERIC(78) NOT NULL,
    from_address        VARCHAR,
    to_address          VARCHAR     NOT NULL,
    arbitrary_data      JSON,
    send_screen_title   VARCHAR,
    send_screen_message VARCHAR,
    send_screen_logo    VARCHAR,
    tx_hash             VARCHAR
);
