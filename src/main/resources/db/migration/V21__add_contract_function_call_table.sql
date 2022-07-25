CREATE TABLE blockchain_api_service.contract_function_call_request (
    id                           UUID                     PRIMARY KEY,
    deployed_contract_id         UUID
                                 REFERENCES blockchain_api_service.contract_deployment_request(id),
    contract_address             VARCHAR                  NOT NULL,
    function_name                VARCHAR                  NOT NULL,
    function_params              JSON                     NOT NULL,
    eth_amount                   NUMERIC(78)              NOT NULL,
    chain_id                     BIGINT                   NOT NULL,
    redirect_url                 VARCHAR                  NOT NULL,
    project_id                   UUID                     NOT NULL REFERENCES blockchain_api_service.project(id),
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    arbitrary_data               JSON,
    screen_before_action_message VARCHAR,
    screen_after_action_message  VARCHAR,
    caller_address               VARCHAR,
    tx_hash                      VARCHAR
);

CREATE INDEX contract_function_call_request_project_id
    ON blockchain_api_service.contract_function_call_request(project_id);
CREATE INDEX contract_function_call_request_created_at
    ON blockchain_api_service.contract_function_call_request(created_at);
CREATE INDEX contract_function_call_request_deployed_contract_id
    ON blockchain_api_service.contract_function_call_request(deployed_contract_id);
CREATE INDEX contract_function_call_request_contract_address
    ON blockchain_api_service.contract_function_call_request(contract_address);
CREATE INDEX contract_function_call_request_caller_address
    ON blockchain_api_service.contract_function_call_request(caller_address);
