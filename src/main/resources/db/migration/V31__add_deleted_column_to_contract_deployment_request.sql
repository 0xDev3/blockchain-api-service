ALTER TABLE blockchain_api_service.contract_deployment_request
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX contract_deployment_request_deleted ON blockchain_api_service.contract_deployment_request(deleted);
