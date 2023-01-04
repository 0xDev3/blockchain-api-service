CREATE DOMAIN blockchain_api_service.USER_ID AS UUID;

ALTER TABLE blockchain_api_service.user_identifier
    ALTER COLUMN id TYPE USER_ID;
ALTER TABLE blockchain_api_service.promo_code_usage
    ALTER COLUMN user_id TYPE USER_ID;
ALTER TABLE blockchain_api_service.api_write_call
    ALTER COLUMN user_id TYPE USER_ID;
ALTER TABLE blockchain_api_service.api_read_call
    ALTER COLUMN user_id TYPE USER_ID;

CREATE DOMAIN blockchain_api_service.PROJECT_ID AS UUID;

ALTER TABLE blockchain_api_service.project
    ALTER COLUMN id TYPE PROJECT_ID,
    ALTER COLUMN owner_id TYPE USER_ID;

CREATE DOMAIN blockchain_api_service.MULTI_PAYMENT_TEMPLATE_ID AS UUID;

ALTER TABLE blockchain_api_service.multi_payment_template
    ALTER COLUMN id TYPE MULTI_PAYMENT_TEMPLATE_ID,
    ALTER COLUMN user_id TYPE USER_ID;

CREATE DOMAIN blockchain_api_service.MULTI_PAYMENT_TEMPLATE_ITEM_ID AS UUID;

ALTER TABLE blockchain_api_service.multi_payment_template_item
    ALTER COLUMN id TYPE MULTI_PAYMENT_TEMPLATE_ITEM_ID,
    ALTER COLUMN template_id TYPE MULTI_PAYMENT_TEMPLATE_ID;

CREATE DOMAIN blockchain_api_service.IMPORTED_CONTRACT_DECORATOR_ID AS UUID;

ALTER TABLE blockchain_api_service.imported_contract_decorator
    ALTER COLUMN id TYPE IMPORTED_CONTRACT_DECORATOR_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.FETCH_TRANSACTION_INFO_CACHE_ID AS UUID;

ALTER TABLE blockchain_api_service.fetch_transaction_info_cache
    ALTER COLUMN id TYPE FETCH_TRANSACTION_INFO_CACHE_ID;

CREATE DOMAIN blockchain_api_service.FETCH_ERC20_ACCOUNT_BALANCE_CACHE_ID AS UUID;

ALTER TABLE blockchain_api_service.fetch_erc20_account_balance_cache
    ALTER COLUMN id TYPE FETCH_ERC20_ACCOUNT_BALANCE_CACHE_ID;

CREATE DOMAIN blockchain_api_service.FETCH_ACCOUNT_BALANCE_CACHE_ID AS UUID;

ALTER TABLE blockchain_api_service.fetch_account_balance_cache
    ALTER COLUMN id TYPE FETCH_ACCOUNT_BALANCE_CACHE_ID;

CREATE DOMAIN blockchain_api_service.CONTRACT_DEPLOYMENT_TRANSACTION_CACHE_ID AS UUID;

ALTER TABLE blockchain_api_service.contract_deployment_transaction_cache
    ALTER COLUMN id TYPE CONTRACT_DEPLOYMENT_TRANSACTION_CACHE_ID;

CREATE DOMAIN blockchain_api_service.ERC20_LOCK_REQUEST_ID AS UUID;

ALTER TABLE blockchain_api_service.erc20_lock_request
    ALTER COLUMN id TYPE ERC20_LOCK_REQUEST_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.CONTRACT_METADATA_ID AS UUID;

ALTER TABLE blockchain_api_service.contract_metadata
    ALTER COLUMN id TYPE CONTRACT_METADATA_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.CONTRACT_DEPLOYMENT_REQUEST_ID AS UUID;

ALTER TABLE blockchain_api_service.contract_deployment_request
    ALTER COLUMN id TYPE CONTRACT_DEPLOYMENT_REQUEST_ID,
    ALTER COLUMN contract_metadata_id TYPE CONTRACT_METADATA_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.CONTRACT_FUNCTION_CALL_REQUEST_ID AS UUID;

ALTER TABLE blockchain_api_service.contract_function_call_request
    ALTER COLUMN id TYPE CONTRACT_FUNCTION_CALL_REQUEST_ID,
    ALTER COLUMN deployed_contract_id TYPE CONTRACT_DEPLOYMENT_REQUEST_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.CONTRACT_ARBITRARY_CALL_REQUEST_ID AS UUID;

ALTER TABLE blockchain_api_service.contract_arbitrary_call_request
    ALTER COLUMN id TYPE CONTRACT_ARBITRARY_CALL_REQUEST_ID,
    ALTER COLUMN deployed_contract_id TYPE CONTRACT_DEPLOYMENT_REQUEST_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.AUTHORIZATION_REQUEST_ID AS UUID;

ALTER TABLE blockchain_api_service.authorization_request
    ALTER COLUMN id TYPE AUTHORIZATION_REQUEST_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.ASSET_SEND_REQUEST_ID AS UUID;

ALTER TABLE blockchain_api_service.asset_send_request
    ALTER COLUMN id TYPE ASSET_SEND_REQUEST_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.ASSET_MULTI_SEND_REQUEST_ID AS UUID;

ALTER TABLE blockchain_api_service.asset_multi_send_request
    ALTER COLUMN id TYPE ASSET_MULTI_SEND_REQUEST_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.ASSET_BALANCE_REQUEST_ID AS UUID;

ALTER TABLE blockchain_api_service.asset_balance_request
    ALTER COLUMN id TYPE ASSET_BALANCE_REQUEST_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.API_USAGE_PERIOD_ID AS UUID;

ALTER TABLE blockchain_api_service.api_usage_period
    ALTER COLUMN id TYPE API_USAGE_PERIOD_ID,
    ALTER COLUMN user_id TYPE USER_ID;

CREATE DOMAIN blockchain_api_service.API_KEY_ID AS UUID;

ALTER TABLE blockchain_api_service.api_key
    ALTER COLUMN id TYPE API_KEY_ID,
    ALTER COLUMN project_id TYPE PROJECT_ID;

CREATE DOMAIN blockchain_api_service.ADDRESS_BOOK_ID AS UUID;

ALTER TABLE blockchain_api_service.address_book
    ALTER COLUMN id TYPE ADDRESS_BOOK_ID,
    ALTER COLUMN user_id TYPE USER_ID;
