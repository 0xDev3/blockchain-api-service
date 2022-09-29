CREATE TABLE blockchain_api_service.asset_multi_send_request (
    id                                   UUID                     PRIMARY KEY,
    chain_id                             BIGINT                   NOT NULL,
    redirect_url                         VARCHAR                  NOT NULL,
    token_address                        VARCHAR,
    disperse_contract_address            VARCHAR                  NOT NULL,
    asset_amounts                        NUMERIC(78)[]            NOT NULL,
    asset_recipient_addresses            VARCHAR[]                NOT NULL,
    item_names                           VARCHAR[]                NOT NULL,
    asset_sender_address                 VARCHAR,
    arbitrary_data                       JSON,
    approve_tx_hash                      VARCHAR,
    send_tx_hash                         VARCHAR,
    approve_screen_before_action_message VARCHAR,
    approve_screen_after_action_message  VARCHAR,
    send_screen_before_action_message    VARCHAR,
    send_screen_after_action_message     VARCHAR,
    project_id                           UUID                     NOT NULL REFERENCES blockchain_api_service.project,
    created_at                           TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (
        array_length(asset_amounts, 1) = array_length(asset_recipient_addresses, 1) AND
        array_length(asset_amounts, 1) = array_length(item_names, 1)
    )
);

CREATE INDEX asset_multi_send_request_project_id ON blockchain_api_service.asset_multi_send_request(project_id);
CREATE INDEX asset_multi_send_request_created_at ON blockchain_api_service.asset_multi_send_request(created_at);
CREATE INDEX asset_multi_send_request_asset_sender_address
    ON blockchain_api_service.asset_multi_send_request(asset_sender_address);
