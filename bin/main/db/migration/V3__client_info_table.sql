CREATE TABLE blockchain_api_service.client_info (
    client_id    VARCHAR(100) PRIMARY KEY,
    chain_id     BIGINT       NOT NULL,
    redirect_url VARCHAR      NOT NULL
);
