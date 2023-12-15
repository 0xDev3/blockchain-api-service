DELETE FROM blockchain_api_service.fetch_transaction_info_cache WHERE 1 = 1;

ALTER TABLE blockchain_api_service.fetch_transaction_info_cache ADD COLUMN raw_transaction_receipt VARCHAR;
