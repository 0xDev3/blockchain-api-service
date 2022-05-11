ALTER TABLE blockchain_api_service.erc20_send_request RENAME COLUMN send_screen_before_action_message
    TO screen_before_action_message;
ALTER TABLE blockchain_api_service.erc20_send_request RENAME COLUMN send_screen_after_action_message
    TO screen_after_action_message;

ALTER TABLE blockchain_api_service.erc20_balance_request RENAME COLUMN balance_screen_before_action_message
    TO screen_before_action_message;
ALTER TABLE blockchain_api_service.erc20_balance_request RENAME COLUMN balance_screen_after_action_message
    TO screen_after_action_message;
