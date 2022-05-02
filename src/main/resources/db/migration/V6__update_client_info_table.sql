ALTER TABLE blockchain_api_service.client_info ALTER COLUMN chain_id DROP NOT NULL;
ALTER TABLE blockchain_api_service.client_info ALTER COLUMN redirect_url DROP NOT NULL;

ALTER TABLE blockchain_api_service.client_info RENAME COLUMN redirect_url TO send_redirect_url;

ALTER TABLE blockchain_api_service.client_info ADD COLUMN balance_redirect_url VARCHAR;
ALTER TABLE blockchain_api_service.client_info ADD COLUMN token_address VARCHAR;
