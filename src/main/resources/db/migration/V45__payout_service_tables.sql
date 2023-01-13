CREATE DOMAIN blockchain_api_service.MERKLE_TREE_ROOT_ID AS UUID;

CREATE TABLE blockchain_api_service.merkle_tree_root (
    id                     MERKLE_TREE_ROOT_ID                  PRIMARY KEY,
    chain_id               BIGINT                               NOT NULL,
    asset_contract_address VARCHAR                              NOT NULL,
    block_number           NUMERIC(78)                          NOT NULL,
    merkle_hash            VARCHAR                              NOT NULL,
    hash_fn                blockchain_api_service.HASH_FUNCTION NOT NULL
);

CREATE UNIQUE INDEX merkle_tree_root_idx
    ON blockchain_api_service.merkle_tree_root(chain_id, asset_contract_address, merkle_hash);

CREATE DOMAIN blockchain_api_service.MERKLE_TREE_LEAF_ID AS UUID;

CREATE TABLE blockchain_api_service.merkle_tree_leaf_node (
    id             MERKLE_TREE_LEAF_ID PRIMARY KEY,
    merkle_root    MERKLE_TREE_ROOT_ID NOT NULL REFERENCES blockchain_api_service.merkle_tree_root(id),
    wallet_address VARCHAR             NOT NULL,
    asset_amount   NUMERIC(78)         NOT NULL
);

CREATE INDEX merkle_tree_leaf_node_root_idx ON blockchain_api_service.merkle_tree_leaf_node(merkle_root);
CREATE INDEX merkle_tree_leaf_node_address_idx ON blockchain_api_service.merkle_tree_leaf_node(wallet_address);
CREATE UNIQUE INDEX merkle_tree_leaf_node_exists_idx
    ON blockchain_api_service.merkle_tree_leaf_node(merkle_root, wallet_address);

CREATE DOMAIN blockchain_api_service.ASSET_SNAPSHOT_ID AS UUID;

ALTER TYPE blockchain_api_service.SNAPSHOT_STATUS        RENAME TO ASSET_SNAPSHOT_STATUS;
ALTER TYPE blockchain_api_service.SNAPSHOT_FAILURE_CAUSE RENAME TO ASSET_SNAPSHOT_FAILURE_CAUSE;

CREATE TABLE blockchain_api_service.asset_snapshot (
    id                       ASSET_SNAPSHOT_ID                             PRIMARY KEY,
    project_id               PROJECT_ID                                    NOT NULL
                             REFERENCES blockchain_api_service.project(id),
    name                     VARCHAR                                       NOT NULL,
    chain_id                 BIGINT                                        NOT NULL,
    asset_contract_address   VARCHAR                                       NOT NULL,
    block_number             NUMERIC(78)                                   NOT NULL,
    ignored_holder_addresses VARCHAR[]                                     NOT NULL,
    status                   ASSET_SNAPSHOT_STATUS                         NOT NULL,
    result_tree              MERKLE_TREE_ROOT_ID                           NULL
                             REFERENCES blockchain_api_service.merkle_tree_root(id),
    tree_ipfs_hash           VARCHAR                                       NULL,
    total_asset_amount       NUMERIC(78)                                   NULL,
    failure_cause            ASSET_SNAPSHOT_FAILURE_CAUSE                  NULL
);

CREATE INDEX asset_snapshot_project_id_status_idx ON blockchain_api_service.asset_snapshot(project_id, status);
CREATE INDEX asset_snapshot_status_idx ON blockchain_api_service.asset_snapshot(status);
