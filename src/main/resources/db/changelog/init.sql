--liquibase formatted sql

--changeset mohamdi:init-sql/1

--------------------------------------------------------------------------------
-- Sequences
--------------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS media_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS client_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS pro_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS pro_wallet_tx_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS category_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS zone_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS customer_request_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS request_media_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS lead_offer_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS job_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS rating_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS notification_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS notification_attribute_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS translation_seq_id START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS app_config_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS user_device_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS user_otp_expiration_id_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE IF NOT EXISTS online_transaction_sequence START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS payment_id_seq START WITH 1 INCREMENT BY 1;


CREATE TABLE IF NOT EXISTS media (
                                     id          BIGINT PRIMARY KEY DEFAULT nextval('media_id_seq'),
    type        VARCHAR(50),
    link        TEXT,
    thumbnail   TEXT,
    key_name    VARCHAR(255),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
    );


CREATE TABLE IF NOT EXISTS client (
                                      id                  BIGINT PRIMARY KEY DEFAULT nextval('client_id_seq'),
    tel                 VARCHAR(20),
    username            VARCHAR(255),
    email               VARCHAR(255),
    first_name          VARCHAR(255),
    last_name           VARCHAR(255),
    logo_id             BIGINT,
    user_id             BIGINT,
    customer_id         VARCHAR(255),
    archived            BOOLEAN DEFAULT FALSE,
    wallet              INTEGER DEFAULT 0,
    cc_only             BOOLEAN DEFAULT FALSE,
    is_deleted          BOOLEAN DEFAULT FALSE,
    is_active           BOOLEAN DEFAULT TRUE,
    is_tel_verified     BOOLEAN DEFAULT FALSE,
    nationality         VARCHAR(100),
    nationality_code    VARCHAR(10),
    birth_date          DATE,
    guest               BOOLEAN DEFAULT FALSE,
    gender              VARCHAR(20),
    ads_accepted        BOOLEAN DEFAULT FALSE,
    referral_code       BIGINT,
    referred_by         BIGINT,
    referral_counter    BIGINT DEFAULT 0,
    login_provider      VARCHAR(50),
    created_at          TIMESTAMP,
    updated_at          TIMESTAMP
    );

ALTER TABLE client
    ADD CONSTRAINT fk_client_logo
        FOREIGN KEY (logo_id) REFERENCES media(id);

CREATE INDEX IF NOT EXISTS idx_client_tel ON client(tel);


CREATE TABLE IF NOT EXISTS category (
                                                id            BIGINT PRIMARY KEY DEFAULT nextval('category_id_seq'),
    code          VARCHAR(50) NOT NULL,     -- PLUMBING/ELECTRICITY/HVAC
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    icon_media_id BIGINT,
    lead_type     VARCHAR(20) DEFAULT 'FIXED',  -- FIXED or PERCENTAGE
    lead_cost     NUMERIC(10,2),
    match_limit   INTEGER DEFAULT 3,
    active        BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    archived        BOOLEAN DEFAULT FALSE,
    CONSTRAINT uq_category_code UNIQUE (code)
    );

ALTER TABLE category
    ADD CONSTRAINT fk_category_icon
        FOREIGN KEY (icon_media_id) REFERENCES media(id);

CREATE TABLE IF NOT EXISTS zone (
                                    id          BIGINT PRIMARY KEY DEFAULT nextval('zone_id_seq'),
    name        VARCHAR(255) NOT NULL,
    city        VARCHAR(255),
    country     VARCHAR(100) DEFAULT 'Mauritania',
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT uq_zone_name UNIQUE (name)
    );


CREATE TABLE IF NOT EXISTS pro (
                                   id                     BIGINT PRIMARY KEY DEFAULT nextval('pro_id_seq'),
    tel                    VARCHAR(20) NOT NULL,
    full_name              VARCHAR(255),
    email                  VARCHAR(255),

    trade_id               BIGINT NOT NULL,
    base_zone_id           BIGINT,

    -- KYC
    cni_front_media_id     BIGINT,
    cni_back_media_id      BIGINT,
    selfie_media_id        BIGINT,
    kyc_status             VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED
    approved_at            TIMESTAMP,
    approved_by            BIGINT,

    -- Operational
    online                 BOOLEAN DEFAULT FALSE,
    rating_avg             NUMERIC(3,2) DEFAULT 5.00,
    rating_count           BIGINT DEFAULT 0,
    jobs_completed         BIGINT DEFAULT 0,

    -- Wallet (prepaid)
    wallet_balance     BIGINT DEFAULT 0,
    low_balance_threshold BIGINT DEFAULT 50,

    is_active              BOOLEAN DEFAULT TRUE,
    is_deleted             BOOLEAN DEFAULT FALSE,

    created_at             TIMESTAMP,
    updated_at             TIMESTAMP,

    CONSTRAINT uq_pro_tel UNIQUE (tel)
    );

ALTER TABLE pro
    ADD CONSTRAINT fk_pro_trade
        FOREIGN KEY (trade_id) REFERENCES category(id);

ALTER TABLE pro
    ADD CONSTRAINT fk_pro_base_zone
        FOREIGN KEY (base_zone_id) REFERENCES zone(id) ON DELETE SET NULL;

ALTER TABLE pro
    ADD CONSTRAINT fk_pro_cni_front
        FOREIGN KEY (cni_front_media_id) REFERENCES media(id) ON DELETE SET NULL;

ALTER TABLE pro
    ADD CONSTRAINT fk_pro_cni_back
        FOREIGN KEY (cni_back_media_id) REFERENCES media(id) ON DELETE SET NULL;

ALTER TABLE pro
    ADD CONSTRAINT fk_pro_selfie
        FOREIGN KEY (selfie_media_id) REFERENCES media(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_pro_tel ON pro(tel);
CREATE INDEX IF NOT EXISTS idx_pro_online ON pro(online);
CREATE INDEX IF NOT EXISTS idx_pro_trade ON pro(trade_id);


CREATE TABLE IF NOT EXISTS pro_wallet_transaction (
                                                      id                BIGINT PRIMARY KEY DEFAULT nextval('pro_wallet_tx_id_seq'),
    pro_id            BIGINT NOT NULL,
    type              VARCHAR(30) NOT NULL,   -- CREDIT/DEBIT/REFUND/ADJUSTMENT
    amount        BIGINT NOT NULL,
    reason            VARCHAR(100),           -- LEAD_PURCHASE / RECHARGE / FREE_LEADS / ...
    reference_type    VARCHAR(50),            -- REQUEST / LEAD / PAYMENT / ONLINE_TRANSACTION
    reference_id      BIGINT,
    balance_after BIGINT,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP
    );

ALTER TABLE pro_wallet_transaction
    ADD CONSTRAINT fk_pro_wallet_tx_pro
        FOREIGN KEY (pro_id) REFERENCES pro(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_pro_wallet_tx_pro_id ON pro_wallet_transaction(pro_id);
CREATE INDEX IF NOT EXISTS idx_pro_wallet_tx_created_at ON pro_wallet_transaction(created_at);


CREATE TABLE IF NOT EXISTS customer_request (
                                                id                  BIGINT PRIMARY KEY DEFAULT nextval('customer_request_id_seq'),
    client_id            BIGINT,
    category_id          BIGINT NOT NULL,

    latitude            NUMERIC(10,7),
    longitude           NUMERIC(10,7),
    address_text        TEXT,
    landmark            VARCHAR(255),

    description_text    TEXT,
    voice_note_media_id BIGINT,
    status              VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- OPEN/BROADCASTED/ASSIGNED/CANCELLED/EXPIRED/DONE
    urgent              BOOLEAN DEFAULT TRUE,

    broadcasted_at      TIMESTAMP,
    expires_at          TIMESTAMP,

    created_at          TIMESTAMP,
    updated_at          TIMESTAMP
    );

ALTER TABLE customer_request
    ADD CONSTRAINT fk_customer_request_client
        FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE SET NULL;

ALTER TABLE customer_request
    ADD CONSTRAINT fk_customer_request_category
        FOREIGN KEY (category_id) REFERENCES category(id);

ALTER TABLE customer_request
    ADD CONSTRAINT fk_customer_request_voice_note
        FOREIGN KEY (voice_note_media_id) REFERENCES media(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_customer_request_status ON customer_request(status);
CREATE INDEX IF NOT EXISTS idx_customer_request_category ON customer_request(category_id);
CREATE INDEX IF NOT EXISTS idx_customer_request_created_at ON customer_request(created_at);
CREATE INDEX IF NOT EXISTS idx_customer_request_geo ON customer_request(latitude, longitude);

CREATE TABLE IF NOT EXISTS request_media (
                                             id          BIGINT PRIMARY KEY DEFAULT nextval('request_media_id_seq'),
    request_id  BIGINT NOT NULL,
    media_id    BIGINT NOT NULL,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
    );

ALTER TABLE request_media
    ADD CONSTRAINT fk_request_media_request
        FOREIGN KEY (request_id) REFERENCES customer_request(id) ON DELETE CASCADE;

ALTER TABLE request_media
    ADD CONSTRAINT fk_request_media_media
        FOREIGN KEY (media_id) REFERENCES media(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_request_media_request_id ON request_media(request_id);


CREATE TABLE IF NOT EXISTS lead_offer (
                                          id          BIGINT PRIMARY KEY DEFAULT nextval('lead_offer_id_seq'),
    request_id  BIGINT NOT NULL,
    pro_id      BIGINT NOT NULL,

    distance_km NUMERIC(6,2),
    price   BIGINT NOT NULL DEFAULT 50,

    status      VARCHAR(30) NOT NULL DEFAULT 'OFFERED', -- OFFERED/ACCEPTED/MISSED/EXPIRED/CANCELLED
    offered_at  TIMESTAMP,
    expires_at  TIMESTAMP,

    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,

    CONSTRAINT uq_lead_offer_request_pro UNIQUE (request_id, pro_id)
    );

ALTER TABLE lead_offer
    ADD CONSTRAINT fk_lead_offer_request
        FOREIGN KEY (request_id) REFERENCES customer_request(id) ON DELETE CASCADE;

ALTER TABLE lead_offer
    ADD CONSTRAINT fk_lead_offer_pro
        FOREIGN KEY (pro_id) REFERENCES pro(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_lead_offer_request_id ON lead_offer(request_id);
CREATE INDEX IF NOT EXISTS idx_lead_offer_pro_id ON lead_offer(pro_id);
CREATE INDEX IF NOT EXISTS idx_lead_offer_status ON lead_offer(status);


CREATE TABLE IF NOT EXISTS job (
                                   id            BIGINT PRIMARY KEY DEFAULT nextval('job_id_seq'),
    request_id    BIGINT NOT NULL,
    lead_offer_id BIGINT NOT NULL,
    pro_id        BIGINT NOT NULL,
    client_id     BIGINT,

    status        VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS/DONE/CANCELLED/NO_SHOW
    started_at    TIMESTAMP,
    done_at       TIMESTAMP,

    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,

    CONSTRAINT uq_job_request UNIQUE (request_id),
    CONSTRAINT uq_job_lead_offer UNIQUE (lead_offer_id)
    );

ALTER TABLE job
    ADD CONSTRAINT fk_job_request
        FOREIGN KEY (request_id) REFERENCES customer_request(id) ON DELETE CASCADE;

ALTER TABLE job
    ADD CONSTRAINT fk_job_lead_offer
        FOREIGN KEY (lead_offer_id) REFERENCES lead_offer(id) ON DELETE CASCADE;

ALTER TABLE job
    ADD CONSTRAINT fk_job_pro
        FOREIGN KEY (pro_id) REFERENCES pro(id) ON DELETE CASCADE;

ALTER TABLE job
    ADD CONSTRAINT fk_job_client
        FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_job_pro_id ON job(pro_id);
CREATE INDEX IF NOT EXISTS idx_job_status ON job(status);


CREATE TABLE IF NOT EXISTS rating (
                                      id         BIGINT PRIMARY KEY DEFAULT nextval('rating_id_seq'),
    job_id     BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    client_id  BIGINT,
    pro_id     BIGINT NOT NULL,

    stars      INTEGER NOT NULL CHECK (stars >= 1 AND stars <= 5),
    comment    TEXT,

    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT uq_rating_job UNIQUE (job_id)
    );

ALTER TABLE rating
    ADD CONSTRAINT fk_rating_job
        FOREIGN KEY (job_id) REFERENCES job(id) ON DELETE CASCADE;

ALTER TABLE rating
    ADD CONSTRAINT fk_rating_request
        FOREIGN KEY (request_id) REFERENCES customer_request(id) ON DELETE CASCADE;

ALTER TABLE rating
    ADD CONSTRAINT fk_rating_client
        FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE SET NULL;

ALTER TABLE rating
    ADD CONSTRAINT fk_rating_pro
        FOREIGN KEY (pro_id) REFERENCES pro(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_rating_pro_id ON rating(pro_id);



-- OnlineTransaction: one record per provider operation
CREATE TABLE IF NOT EXISTS online_transaction (
                                                  id             BIGINT PRIMARY KEY DEFAULT nextval('online_transaction_sequence'),
    operation_id   VARCHAR(255),          -- Sedad:idFacture, Masrivi:purchaseRef, Bankily:operationId
    transaction_id VARCHAR(255),
    customer_name  VARCHAR(255),
    amount         INTEGER NOT NULL,
    payment_ref    VARCHAR(255),          -- receipt / numeroRecu / etc
    client_phone   VARCHAR(30),
    status         VARCHAR(50),
    error_message  TEXT,
    payment_code   VARCHAR(255),          -- Sedad only
    error_code     VARCHAR(50),
    bank_type      VARCHAR(30) NOT NULL,  -- Sedad / Masrivi / Bankily
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_online_tx_operation_id ON online_transaction(operation_id);
CREATE INDEX IF NOT EXISTS idx_online_tx_bank_type ON online_transaction(bank_type);


CREATE TABLE IF NOT EXISTS payment (
                                       id                    BIGINT PRIMARY KEY DEFAULT nextval('payment_id_seq'),

    online_transaction_id BIGINT UNIQUE,          -- @OneToOne
    amount                INTEGER,                -- base amount
    wallet_amount         INTEGER,                -- portion paid by internal wallet (if you use it later)
    total                 INTEGER,                -- amount + wallet_amount
    payment_method_type   VARCHAR(50),            -- BANKILY / SEDAD / MASRIVI / WALLET_BANKILY / ...
    payment_purpose       VARCHAR(50),            -- RECHARGE_WALLET / LEAD_PACKAGE / ...
    status                VARCHAR(50),            -- SUCCEEDED / IN_PROGRESS / FAILED / ...

    main_payment_id       BIGINT,                 -- split payments support (optional, from your model)

    payment_media_id      BIGINT,
    payment_ref           VARCHAR(255),

    pro_id                BIGINT,
    client_id             BIGINT,                 -- optional if client can pay for something later
    admin_id              BIGINT,

    created_at            TIMESTAMP,
    updated_at            TIMESTAMP
    );

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_online_transaction
        FOREIGN KEY (online_transaction_id) REFERENCES online_transaction(id) ON DELETE SET NULL;

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_main_payment
        FOREIGN KEY (main_payment_id) REFERENCES payment(id) ON DELETE SET NULL;

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_media
        FOREIGN KEY (payment_media_id) REFERENCES media(id) ON DELETE SET NULL;

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_pro
        FOREIGN KEY (pro_id) REFERENCES pro(id) ON DELETE SET NULL;

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_client
        FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_payment_status ON payment(status);
CREATE INDEX IF NOT EXISTS idx_payment_purpose ON payment(payment_purpose);
CREATE INDEX IF NOT EXISTS idx_payment_pro_id ON payment(pro_id);
CREATE INDEX IF NOT EXISTS idx_payment_client_id ON payment(client_id);


CREATE TABLE IF NOT EXISTS user_device (
                                           id           BIGINT PRIMARY KEY DEFAULT nextval('user_device_id_seq'),
    token        VARCHAR(255),
    os_type      VARCHAR(20),
    lang         VARCHAR(10),
    profile_type VARCHAR(50), -- CLIENT / PRO / ADMIN
    client_id    BIGINT,
    pro_id       BIGINT,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP
    );

ALTER TABLE user_device
    ADD CONSTRAINT fk_user_device_client
        FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE SET NULL;

ALTER TABLE user_device
    ADD CONSTRAINT fk_user_device_pro
        FOREIGN KEY (pro_id) REFERENCES pro(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS user_otp_expiration (
                                                   id                BIGINT PRIMARY KEY DEFAULT nextval('user_otp_expiration_id_seq'),
    username          VARCHAR(255),
    method            VARCHAR(50), -- SMS / WHATSAPP / EMAIL
    otp               VARCHAR(20),
    expiration_time   TIMESTAMP,
    next_resend_time  TIMESTAMP,
    is_used           BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP
    );


CREATE TABLE IF NOT EXISTS notification (
                                            id             BIGINT PRIMARY KEY DEFAULT nextval('notification_id_seq'),
    read_at        TIMESTAMP,
    content        TEXT,
    title          VARCHAR(255),
    type           VARCHAR(50),
    business_id    BIGINT,
    served_app     VARCHAR(50),
    read_by        BIGINT,
    partner_id     BIGINT,
    read           BOOLEAN DEFAULT FALSE,
    channel_name   VARCHAR(255),
    channel_token  VARCHAR(255),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS notification_attribute (
                                                      id               BIGINT PRIMARY KEY DEFAULT nextval('notification_attribute_id_seq'),
    key              VARCHAR(255),
    value            TEXT,
    notification_id  BIGINT NOT NULL
    );

ALTER TABLE notification_attribute
    ADD CONSTRAINT fk_notification_attribute_notification
        FOREIGN KEY (notification_id) REFERENCES notification(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS client_notification (
                                                   client_id       BIGINT NOT NULL,
                                                   notification_id BIGINT NOT NULL,
                                                   PRIMARY KEY (client_id, notification_id)
    );

ALTER TABLE client_notification
    ADD CONSTRAINT fk_client_notification_client
        FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE CASCADE;

ALTER TABLE client_notification
    ADD CONSTRAINT fk_client_notification_notification
        FOREIGN KEY (notification_id) REFERENCES notification(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS pro_notification (
                                                pro_id          BIGINT NOT NULL,
                                                notification_id BIGINT NOT NULL,
                                                PRIMARY KEY (pro_id, notification_id)
    );

ALTER TABLE pro_notification
    ADD CONSTRAINT fk_pro_notification_pro
        FOREIGN KEY (pro_id) REFERENCES pro(id) ON DELETE CASCADE;

ALTER TABLE pro_notification
    ADD CONSTRAINT fk_pro_notification_notification
        FOREIGN KEY (notification_id) REFERENCES notification(id) ON DELETE CASCADE;


CREATE TABLE IF NOT EXISTS app_config_bundle (
                                                 id            BIGINT PRIMARY KEY DEFAULT nextval('app_config_id_seq'),
    app           VARCHAR(20) NOT NULL,
    platform      VARCHAR(20) NOT NULL,
    country       VARCHAR(2),
    min_version   VARCHAR(20),
    max_version   VARCHAR(20),
    is_active     BOOLEAN DEFAULT TRUE,
    config_json   JSONB,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    hash          VARCHAR(255),
    description   TEXT
    );

CREATE INDEX IF NOT EXISTS idx_app_config_bundle_is_active ON app_config_bundle(is_active);
CREATE INDEX IF NOT EXISTS idx_app_config_bundle_hash ON app_config_bundle(hash);


CREATE TABLE IF NOT EXISTS translation_values (
                                                  id                 BIGINT PRIMARY KEY DEFAULT nextval('translation_seq_id'),
    language_code      VARCHAR(10)  NOT NULL,
    translation_key    VARCHAR(255) NOT NULL,
    translation_value  TEXT         NOT NULL,
    context            VARCHAR(50),
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    CONSTRAINT uq_translation_lang_key UNIQUE (language_code, translation_key)
    );

CREATE SEQUENCE IF NOT EXISTS address_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS address (
                                       id                 BIGINT PRIMARY KEY DEFAULT nextval('address_id_seq'),
    name               VARCHAR(255),
    latitude           NUMERIC(10,7),
    longitude          NUMERIC(10,7),
    country            VARCHAR(100),
    postal_code        VARCHAR(50),
    timezone           VARCHAR(50),
    formatted_address  TEXT,
    tel                VARCHAR(30),
    custom_locality    VARCHAR(255),
    description        TEXT,
    title              VARCHAR(255),
    route              VARCHAR(255),
    building_number    INTEGER,
    building_name      VARCHAR(255),
    archived           BOOLEAN DEFAULT FALSE,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_address_archived ON address(archived);


CREATE TABLE IF NOT EXISTS client_address_relation (
                                                       client_id  BIGINT NOT NULL,
                                                       address_id BIGINT NOT NULL,
                                                       PRIMARY KEY (client_id, address_id)
    );

ALTER TABLE client_address_relation
    ADD CONSTRAINT fk_client_address_client
    FOREIGN KEY (client_id) REFERENCES client(id) ON DELETE CASCADE;

ALTER TABLE client_address_relation
    ADD CONSTRAINT fk_client_address_address
    FOREIGN KEY (address_id) REFERENCES address(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_client_address_client_id ON client_address_relation(client_id);
CREATE INDEX IF NOT EXISTS idx_client_address_address_id ON client_address_relation(address_id);


ALTER TABLE category
    ADD COLUMN IF NOT EXISTS workflow_type VARCHAR(20) DEFAULT 'LEAD_OFFER';

UPDATE category SET workflow_type = 'LEAD_OFFER' WHERE workflow_type IS NULL;

--changeset sallahli:fix-schema-archived-columns
ALTER TABLE media ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT FALSE;
ALTER TABLE zone ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT FALSE;
ALTER TABLE pro ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT FALSE;

--changeset mohamdi:init-sql/2
ALTER TABLE media ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT FALSE;

--changeset mohamdi:init-sql/3
ALTER TABLE media
    ADD COLUMN IF NOT EXISTS duration_millis BIGINT;

ALTER TABLE media
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(255);

ALTER TABLE media
    ADD COLUMN IF NOT EXISTS size_bytes BIGINT;

--changeset mohamdi:init-sql/4
ALTER TABLE zone ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT FALSE;

--changeset mohamdi:init-sql/5
ALTER TABLE pro ADD COLUMN IF NOT EXISTS current_latitude DOUBLE PRECISION;
ALTER TABLE pro ADD COLUMN IF NOT EXISTS current_longitude DOUBLE PRECISION;
ALTER TABLE pro ADD COLUMN IF NOT EXISTS location_updated_at TIMESTAMP;

--changeset mohamdi:init-sql/6
ALTER TABLE pro ADD COLUMN IF NOT EXISTS first_name VARCHAR(255);
ALTER TABLE pro ADD COLUMN IF NOT EXISTS last_name VARCHAR(255);
ALTER TABLE pro ADD COLUMN IF NOT EXISTS profile_photo VARCHAR(255);
ALTER TABLE pro ADD COLUMN IF NOT EXISTS trade_doc_media_id BIGINT;
ALTER TABLE pro ADD COLUMN IF NOT EXISTS kyc_submitted_at TIMESTAMP;

ALTER TABLE pro
    DROP CONSTRAINT IF EXISTS fk_pro_trade_doc;

ALTER TABLE pro
    ADD CONSTRAINT fk_pro_trade_doc
        FOREIGN KEY (trade_doc_media_id) REFERENCES media(id) ON DELETE SET NULL;

--changeset mohamdi:init-sql/7
ALTER TABLE pro ADD COLUMN IF NOT EXISTS username VARCHAR(255);
ALTER TABLE pro ADD CONSTRAINT uq_pro_username UNIQUE (username);

--changeset mohamdi:init-sql/8
ALTER TABLE client ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

--changeset mohamdi:init-sql/9
ALTER TABLE customer_request ADD COLUMN IF NOT EXISTS address_id BIGINT;
ALTER TABLE customer_request ADD CONSTRAINT fk_customer_request_address FOREIGN KEY (address_id) REFERENCES address(id);

--changeset mohamdi:init-sql/10
ALTER TABLE customer_request ADD COLUMN IF NOT EXISTS archived BOOLEAN DEFAULT FALSE;

--changeset mohamdi:init-sql/11

CREATE SEQUENCE IF NOT EXISTS admin_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS admin (
    id              BIGINT PRIMARY KEY DEFAULT nextval('admin_id_seq'),
    tel             VARCHAR(20),
    username        VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    first_name      VARCHAR(255),
    last_name       VARCHAR(255),
    profile_photo   VARCHAR(255),
    role            VARCHAR(50),
    department      VARCHAR(255),
    is_active       BOOLEAN DEFAULT TRUE,
    archived        BOOLEAN DEFAULT FALSE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    CONSTRAINT uq_admin_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_admin_username ON admin(username);
CREATE INDEX IF NOT EXISTS idx_admin_role ON admin(role);
CREATE INDEX IF NOT EXISTS idx_admin_is_active ON admin(is_active);

ALTER TABLE pro ADD COLUMN IF NOT EXISTS approved_by_admin_id BIGINT;

ALTER TABLE pro
    DROP CONSTRAINT IF EXISTS fk_pro_approved_by_admin;

ALTER TABLE pro
    ADD CONSTRAINT fk_pro_approved_by_admin
        FOREIGN KEY (approved_by_admin_id) REFERENCES admin(id) ON DELETE SET NULL;

ALTER TABLE pro DROP COLUMN IF EXISTS approved_by;

ALTER TABLE payment
    DROP CONSTRAINT IF EXISTS fk_payment_admin;

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id) ON DELETE SET NULL;

--changeset mohamdi:init-sql/12
ALTER TABLE pro ALTER COLUMN trade_id DROP NOT NULL;

--changeset mohamdi:init-sql/13
ALTER TABLE pro ADD COLUMN IF NOT EXISTS is_tel_verified BOOLEAN DEFAULT FALSE;

--changeset mohamdi:init-sql/14
CREATE TABLE IF NOT EXISTS pro_category (
                                            pro_id BIGINT NOT NULL,
                                            category_id BIGINT NOT NULL,
                                            PRIMARY KEY (pro_id, category_id),
    CONSTRAINT fk_pro_category_pro FOREIGN KEY (pro_id) REFERENCES pro(id) ON DELETE CASCADE,
    CONSTRAINT fk_pro_category_category FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
    );

ALTER TABLE pro DROP COLUMN IF EXISTS trade_id;

--changeset mohamdi:init-sql/15
ALTER TABLE category ADD COLUMN IF NOT EXISTS name_ar VARCHAR(255);
ALTER TABLE category ADD COLUMN IF NOT EXISTS description_ar TEXT;