ALTER TABLE blockchain_api_service.erc20_balance_request
    RENAME CONSTRAINT erc20_balance_request_pkey TO asset_balance_request_pkey;
ALTER TABLE blockchain_api_service.erc20_balance_request
    RENAME CONSTRAINT erc20_balance_request_project_id_fkey TO asset_balance_request_project_id_fkey;

ALTER INDEX erc20_balance_request_created_at RENAME TO asset_balance_request_created_at;
ALTER INDEX erc20_balance_request_project_id RENAME TO asset_balance_request_project_id;

ALTER TABLE blockchain_api_service.erc20_balance_request
    RENAME TO asset_balance_request;

ALTER TABLE blockchain_api_service.erc20_send_request
    RENAME CONSTRAINT send_erc20_request_pkey TO asset_send_request_pkey;
ALTER TABLE blockchain_api_service.erc20_send_request
    RENAME CONSTRAINT erc20_send_request_project_id_fkey TO asset_send_request_project_id_fkey;

ALTER INDEX erc20_send_request_created_at RENAME TO asset_send_request_created_at;
ALTER INDEX erc20_send_request_project_id RENAME TO asset_send_request_project_id;
ALTER INDEX erc20_send_request_token_recipient_address RENAME TO asset_send_request_asset_recipient_address;
ALTER INDEX erc20_send_request_token_sender_address RENAME TO asset_send_request_asset_sender_address;

ALTER TABLE blockchain_api_service.erc20_send_request
    RENAME COLUMN token_amount TO asset_amount;
ALTER TABLE blockchain_api_service.erc20_send_request
    RENAME COLUMN token_sender_address TO asset_sender_address;
ALTER TABLE blockchain_api_service.erc20_send_request
    RENAME COLUMN token_recipient_address TO asset_recipient_address;

ALTER TABLE blockchain_api_service.erc20_send_request
    RENAME TO asset_send_request;
