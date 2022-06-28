ALTER TABLE blockchain_api_service.erc20_send_request ALTER COLUMN token_address DROP NOT NULL;
ALTER TABLE blockchain_api_service.erc20_balance_request ALTER COLUMN token_address DROP NOT NULL;
