ALTER TABLE blockchain_api_service.contract_metadata
    ADD COLUMN project_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

ALTER TABLE blockchain_api_service.contract_metadata
    DROP CONSTRAINT contract_metadata_contract_id_key;

ALTER TABLE blockchain_api_service.contract_metadata
    ADD CONSTRAINT contract_metadata_contract_id_project_id UNIQUE (contract_id, project_id);
