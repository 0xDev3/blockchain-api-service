CREATE TABLE blockchain_api_service.unsigned_verification_message (
    id             UUID                     PRIMARY KEY,
    wallet_address VARCHAR                  NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX unsigned_verification_message_valid_until
    ON blockchain_api_service.unsigned_verification_message(valid_until);
