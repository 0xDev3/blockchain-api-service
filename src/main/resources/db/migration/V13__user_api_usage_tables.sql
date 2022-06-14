CREATE TYPE blockchain_api_service.USER_IDENTIFIER_TYPE AS ENUM ('ETH_WALLET_ADDRESS');

CREATE TABLE blockchain_api_service.user_identifier (
    id              UUID                 PRIMARY KEY,
    user_identifier VARCHAR              NOT NULL,
    identifier_type USER_IDENTIFIER_TYPE NOT NULL,
    UNIQUE (user_identifier, identifier_type)
);

CREATE INDEX user_identifier_user_identifier_identifier_type
    ON blockchain_api_service.user_identifier(user_identifier, identifier_type);

CREATE TABLE blockchain_api_service.project (
    id                      UUID    PRIMARY KEY,
    owner_id                UUID    NOT NULL REFERENCES blockchain_api_service.user_identifier(id),
    issuer_contract_address VARCHAR NOT NULL UNIQUE,
    redirect_url            VARCHAR NOT NULL,
    chain_id                BIGINT  NOT NULL,
    custom_rpc_url          VARCHAR
);

CREATE INDEX project_owner_id ON blockchain_api_service.project(owner_id);

CREATE TABLE blockchain_api_service.api_key (
    id         UUID    PRIMARY KEY,
    project_id UUID    NOT NULL REFERENCES blockchain_api_service.project(id),
    api_key    VARCHAR NOT NULL UNIQUE
);

CREATE INDEX api_key_project_id ON blockchain_api_service.api_key(project_id);
CREATE INDEX api_key_api_key ON blockchain_api_service.api_key(api_key);
