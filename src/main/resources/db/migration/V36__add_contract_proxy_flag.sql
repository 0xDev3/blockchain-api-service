ALTER TABLE blockchain_api_service.contract_deployment_request
    ADD COLUMN proxy BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE blockchain_api_service.contract_deployment_request
    ADD COLUMN implementation_contract_address VARCHAR NULL;
