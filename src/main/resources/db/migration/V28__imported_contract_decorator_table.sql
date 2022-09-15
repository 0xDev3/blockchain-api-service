ALTER TABLE blockchain_api_service.contract_deployment_request
    ADD COLUMN imported BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE blockchain_api_service.imported_contract_decorator (
    id            UUID    PRIMARY KEY,
    contract_id   VARCHAR NOT NULL UNIQUE,
    manifest_json JSON    NOT NULL,
    artifact_json JSON    NOT NULL,
    info_markdown VARCHAR NOT NULL
)
