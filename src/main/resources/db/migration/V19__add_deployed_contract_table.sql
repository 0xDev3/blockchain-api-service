CREATE TABLE blockchain_api_service.deployed_contract (
    id                           UUID                     PRIMARY KEY,
    contract_id                  VARCHAR                  NOT NULL,
    contract_data                BYTEA                    NOT NULL,
    contract_tags                VARCHAR[]                NOT NULL,
    contract_implements          VARCHAR[]                NOT NULL,
    chain_id                     BIGINT                   NOT NULL,
    redirect_url                 VARCHAR                  NOT NULL,
    project_id                   UUID                     NOT NULL REFERENCES blockchain_api_service.project(id),
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    arbitrary_data               JSON,
    screen_before_action_message VARCHAR,
    screen_after_action_message  VARCHAR,
    contract_address             VARCHAR,
    tx_hash                      VARCHAR
);

CREATE INDEX deployed_contract_contract_id ON blockchain_api_service.deployed_contract(contract_id);
CREATE INDEX deployed_contract_project_id ON blockchain_api_service.deployed_contract(project_id);
CREATE INDEX deployed_contract_created_at ON blockchain_api_service.deployed_contract(created_at);

CREATE INDEX deployed_contract_contract_tags ON blockchain_api_service.deployed_contract USING gin(contract_tags);
CREATE INDEX deployed_contract_contract_implements
    ON blockchain_api_service.deployed_contract USING gin(contract_implements);
