CREATE TABLE blockchain_api_service.contract_deployment_request (
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
    deployer_address             VARCHAR,
    tx_hash                      VARCHAR
);

CREATE INDEX contract_deployment_request_contract_id ON blockchain_api_service.contract_deployment_request(contract_id);
CREATE INDEX contract_deployment_request_project_id ON blockchain_api_service.contract_deployment_request(project_id);
CREATE INDEX contract_deployment_request_created_at ON blockchain_api_service.contract_deployment_request(created_at);
CREATE INDEX contract_deployment_contract_address
    ON blockchain_api_service.contract_deployment_request(contract_address);
CREATE INDEX contract_deployment_deployer_address
    ON blockchain_api_service.contract_deployment_request(deployer_address);

CREATE INDEX contract_deployment_request_contract_tags
    ON blockchain_api_service.contract_deployment_request USING gin(contract_tags);
CREATE INDEX contract_deployment_request_contract_implements
    ON blockchain_api_service.contract_deployment_request USING gin(contract_implements);
