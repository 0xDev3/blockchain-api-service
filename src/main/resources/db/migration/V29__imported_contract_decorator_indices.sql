ALTER TABLE blockchain_api_service.imported_contract_decorator
    ADD COLUMN contract_tags       VARCHAR[]                NOT NULL DEFAULT '{}',
    ADD COLUMN contract_implements VARCHAR[]                NOT NULL DEFAULT '{}',
    ADD COLUMN imported_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

CREATE INDEX imported_contract_decorator_project_id ON blockchain_api_service.imported_contract_decorator(project_id);
CREATE INDEX imported_contract_decorator_contract_tags
    ON blockchain_api_service.imported_contract_decorator USING gin(contract_tags);
CREATE INDEX imported_contract_decorator_contract_implements
    ON blockchain_api_service.imported_contract_decorator USING gin(contract_implements);
CREATE INDEX imported_contract_decorator_imported_at ON blockchain_api_service.imported_contract_decorator(imported_at);
