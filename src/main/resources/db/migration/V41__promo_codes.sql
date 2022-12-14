CREATE TABLE blockchain_api_service.promo_code (
    code           VARCHAR                  PRIMARY KEY,
    write_requests BIGINT                   NOT NULL,
    read_requests  BIGINT                   NOT NULL,
    num_of_usages  BIGINT                   NOT NULL,
    valid_until    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX promo_code_valid_until_index ON blockchain_api_service.promo_code(valid_until);

CREATE TABLE blockchain_api_service.promo_code_usage (
    user_id UUID                     NOT NULL REFERENCES blockchain_api_service.user_identifier(id),
    code    VARCHAR                  NOT NULL REFERENCES blockchain_api_service.promo_code(code),
    used_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX promo_code_usage_user_id_code_index ON blockchain_api_service.promo_code_usage(user_id, code);
