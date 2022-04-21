ALTER TABLE blockchain_api_service.send_erc20_request RENAME COLUMN amount       TO token_amount;
ALTER TABLE blockchain_api_service.send_erc20_request RENAME COLUMN from_address TO token_sender_address;
ALTER TABLE blockchain_api_service.send_erc20_request RENAME COLUMN to_address   TO token_recipient_address;
