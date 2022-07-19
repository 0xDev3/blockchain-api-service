CREATE TABLE blockchain_api_service.contract_metadata (
    id                  UUID           PRIMARY KEY,
    contract_id         VARCHAR UNIQUE NOT NULL,
    contract_tags       VARCHAR[]      NOT NULL,
    contract_implements VARCHAR[]      NOT NULL
);

CREATE INDEX contract_metadata_contract_tags
    ON blockchain_api_service.contract_metadata USING gin(contract_tags);
CREATE INDEX contract_metadata_contract_implements
    ON blockchain_api_service.contract_metadata USING gin(contract_implements);

CREATE TABLE blockchain_api_service.contract_deployment_request (
    id                           UUID                     PRIMARY KEY,
    alias                        VARCHAR                  NOT NULL,
    contract_metadata_id         UUID                     NOT NULL
                                 REFERENCES blockchain_api_service.contract_metadata(id),
    contract_data                BYTEA                    NOT NULL,
    initial_eth_amount           NUMERIC(78)              NOT NULL,
    chain_id                     BIGINT                   NOT NULL,
    redirect_url                 VARCHAR                  NOT NULL,
    project_id                   UUID                     NOT NULL REFERENCES blockchain_api_service.project(id),
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    arbitrary_data               JSON,
    screen_before_action_message VARCHAR,
    screen_after_action_message  VARCHAR,
    contract_address             VARCHAR,
    deployer_address             VARCHAR,
    tx_hash                      VARCHAR,
    CONSTRAINT contract_deployment_request_per_project_unique_alias UNIQUE (project_id, alias)
);

CREATE INDEX contract_deployment_request_project_id ON blockchain_api_service.contract_deployment_request(project_id);
CREATE INDEX contract_deployment_request_created_at ON blockchain_api_service.contract_deployment_request(created_at);
CREATE INDEX contract_deployment_request_contract_metadata_id
    ON blockchain_api_service.contract_deployment_request(contract_metadata_id);
CREATE INDEX contract_deployment_request_contract_address
    ON blockchain_api_service.contract_deployment_request(contract_address);
CREATE INDEX contract_deployment_request_deployer_address
    ON blockchain_api_service.contract_deployment_request(deployer_address);
