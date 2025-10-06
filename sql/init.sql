CREATE TABLE IF NOT EXISTS shop_points (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           world VARCHAR(100) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    type ENUM('SHOP','SELL') NOT NULL,
    yaw FLOAT DEFAULT 0,
    pitch FLOAT DEFAULT 0,
    creator_uuid CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_loc (world, x, y, z)
    );

CREATE TABLE IF NOT EXISTS shop_transactions (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                 player_uuid CHAR(36) NOT NULL,
    direction ENUM('BUY','SELL') NOT NULL,
    gross_amount DECIMAL(18,2) NOT NULL,
    fee_amount DECIMAL(18,2) NOT NULL,
    net_amount DECIMAL(18,2) NOT NULL,
    items_json JSON NOT NULL,
    point_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_time (player_uuid, created_at),
    FOREIGN KEY (point_id) REFERENCES shop_points(id) ON DELETE SET NULL
    );

CREATE TABLE IF NOT EXISTS shop_inventory (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              key_hash VARCHAR(128) NOT NULL,
    material VARCHAR(64) NOT NULL,
    quantity BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_key (key_hash),
    KEY idx_material (material)
    );