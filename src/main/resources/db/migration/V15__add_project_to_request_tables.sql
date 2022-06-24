-- dummy user needed for migrations
INSERT INTO blockchain_api_service.user_identifier(id, user_identifier, identifier_type)
VALUES ('00000000-0000-0000-0000-000000000000', '0x0000000000000000000000000000000000000000', 'ETH_WALLET_ADDRESS');

-- dummy project needed for migrations
INSERT INTO blockchain_api_service.project(id, owner_id, issuer_contract_address, redirect_url, chain_id, created_at)
VALUES ('00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000',
        '00000000-0000-0000-0000-000000000000', '', 0, now());

ALTER TABLE blockchain_api_service.erc20_send_request
    ADD COLUMN project_id UUID NOT NULL REFERENCES blockchain_api_service.project(id)
        DEFAULT '00000000-0000-0000-0000-000000000000';
ALTER TABLE blockchain_api_service.erc20_lock_request
    ADD COLUMN project_id UUID NOT NULL REFERENCES blockchain_api_service.project(id)
        DEFAULT '00000000-0000-0000-0000-000000000000';
ALTER TABLE blockchain_api_service.erc20_balance_request
    ADD COLUMN project_id UUID NOT NULL REFERENCES blockchain_api_service.project(id)
        DEFAULT '00000000-0000-0000-0000-000000000000';

CREATE INDEX erc20_send_request_project_id ON blockchain_api_service.erc20_send_request(project_id);
CREATE INDEX erc20_lock_request_project_id ON blockchain_api_service.erc20_lock_request(project_id);
CREATE INDEX erc20_balance_request_project_id ON blockchain_api_service.erc20_balance_request(project_id);

ALTER TABLE blockchain_api_service.erc20_send_request
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE blockchain_api_service.erc20_lock_request
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE blockchain_api_service.erc20_balance_request
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

CREATE INDEX erc20_send_request_created_at ON blockchain_api_service.erc20_send_request(created_at);
CREATE INDEX erc20_lock_request_created_at ON blockchain_api_service.erc20_lock_request(created_at);
CREATE INDEX erc20_balance_request_created_at ON blockchain_api_service.erc20_balance_request(created_at);
