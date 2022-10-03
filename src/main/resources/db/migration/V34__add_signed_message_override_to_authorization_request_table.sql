ALTER TABLE blockchain_api_service.authorization_request
    ADD COLUMN message_to_sign_override VARCHAR,
    ADD COLUMN store_indefinitely       BOOLEAN NOT NULL DEFAULT TRUE;
