CREATE TABLE blockchain_api_service.api_usage_period (
    project_id                UUID                     NOT NULL REFERENCES blockchain_api_service.project(id),
    additional_write_requests INT                      NOT NULL,
    additional_read_requests  INT                      NOT NULL,
    start_date                TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date                  TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (end_date > start_date)
);

CREATE INDEX api_usage_period_project_id_end_date ON blockchain_api_service.api_usage_period(project_id, end_date);

CREATE TYPE blockchain_api_service.REQUEST_METHOD AS ENUM (
    'GET', 'HEAD', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'TRACE'
);

CREATE TABLE blockchain_api_service.api_write_call (
    project_id     UUID                     NOT NULL REFERENCES blockchain_api_service.project(id),
    request_method REQUEST_METHOD           NOT NULL,
    request_path   VARCHAR                  NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX api_write_call_project_id_created_at ON blockchain_api_service.api_write_call(project_id, created_at);

CREATE TABLE blockchain_api_service.api_read_call (
    project_id   UUID                     NOT NULL REFERENCES blockchain_api_service.project(id),
    request_path VARCHAR                  NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX api_read_call_project_id_created_at ON blockchain_api_service.api_read_call(project_id, created_at);
