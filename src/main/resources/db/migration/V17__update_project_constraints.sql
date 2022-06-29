ALTER TABLE blockchain_api_service.project
    DROP CONSTRAINT project_issuer_contract_address_key;
ALTER TABLE blockchain_api_service.project
    ADD CONSTRAINT project_unique_issuer_per_chain UNIQUE (issuer_contract_address, chain_id);
