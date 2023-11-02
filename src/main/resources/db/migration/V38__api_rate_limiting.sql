CREATE TABLE blockchain_api_service.api_usage_period (
    id                        UUID                     PRIMARY KEY,
    user_id                   UUID                     NOT NULL REFERENCES blockchain_api_service.user_identifier(id),
    allowed_write_requests    BIGINT                   NOT NULL,
    allowed_read_requests     BIGINT                   NOT NULL,
    used_write_requests       BIGINT                   NOT NULL,
    used_read_requests        BIGINT                   NOT NULL,
    start_date                TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date                  TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (end_date > start_date)
);

CREATE INDEX api_usage_period_user_id_end_date
    ON blockchain_api_service.api_usage_period(user_id, end_date);
CREATE INDEX api_usage_period_user_id_start_date_end_date
    ON blockchain_api_service.api_usage_period(user_id, start_date, end_date);

CREATE TYPE blockchain_api_service.REQUEST_METHOD AS ENUM (
    'GET', 'HEAD', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS', 'TRACE'
);

CREATE TABLE blockchain_api_service.api_write_call (
    user_id        UUID                     NOT NULL REFERENCES blockchain_api_service.user_identifier(id),
    request_method REQUEST_METHOD           NOT NULL,
    request_path   VARCHAR                  NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX api_write_call_user_id_created_at ON blockchain_api_service.api_write_call(user_id, created_at);

CREATE TABLE blockchain_api_service.api_read_call (
    user_id      UUID                     NOT NULL REFERENCES blockchain_api_service.user_identifier(id),
    request_path VARCHAR                  NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX api_read_call_user_id_created_at ON blockchain_api_service.api_read_call(user_id, created_at);
