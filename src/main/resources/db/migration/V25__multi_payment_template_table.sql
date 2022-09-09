CREATE TABLE blockchain_api_service.multi_payment_template (
    id                           UUID                     PRIMARY KEY,
    template_name                VARCHAR                  NOT NULL,
    chain_id                     BIGINT                   NOT NULL,
    user_id                      UUID                     NOT NULL
                                 REFERENCES blockchain_api_service.user_identifier(id),
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                   TIMESTAMP WITH TIME ZONE
);

CREATE INDEX multi_payment_template_created_at ON blockchain_api_service.multi_payment_template(created_at);
CREATE INDEX multi_payment_template_user_id ON blockchain_api_service.multi_payment_template(user_id);

CREATE TABLE blockchain_api_service.multi_payment_template_item (
    id             UUID                     PRIMARY KEY,
    template_id    UUID                     NOT NULL REFERENCES blockchain_api_service.multi_payment_template(id),
    wallet_address VARCHAR                  NOT NULL,
    item_name      VARCHAR,
    token_address  VARCHAR,
    asset_amount   NUMERIC(78)              NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX multi_payment_template_item_template_id ON blockchain_api_service.multi_payment_template_item(template_id);
CREATE INDEX multi_payment_template_item_created_at ON blockchain_api_service.multi_payment_template_item(created_at);
