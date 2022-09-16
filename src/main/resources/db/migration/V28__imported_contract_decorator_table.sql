ALTER TABLE blockchain_api_service.contract_deployment_request
    ADD COLUMN imported BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE blockchain_api_service.imported_contract_decorator (
    id            UUID    PRIMARY KEY,
    project_id    UUID    NOT NULL REFERENCES blockchain_api_service.project(id),
    contract_id   VARCHAR NOT NULL,
    manifest_json JSON    NOT NULL,
    artifact_json JSON    NOT NULL,
    info_markdown VARCHAR NOT NULL,
    UNIQUE (project_id, contract_id)
)
