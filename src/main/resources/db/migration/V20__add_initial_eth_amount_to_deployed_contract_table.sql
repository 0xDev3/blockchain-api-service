ALTER TABLE blockchain_api_service.contract_deployment_request
    ADD COLUMN initial_eth_amount NUMERIC(78) NOT NULL DEFAULT 0;
