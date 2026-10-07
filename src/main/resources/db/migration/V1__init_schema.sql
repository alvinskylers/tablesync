/*Create Users Table*/
CREATE TABLE users(
    id              UUID                NOT NULL,
    email           VARCHAR(100)        NOT NULL,
    phone           VARCHAR(20) ,
    password_hash   VARCHAR(255)        NOT NULL,
    role            VARCHAR(20)         NOT NULL    DEFAULT 'CUSTOMER',
    created_at      TIMESTAMP           NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_email UNIQUE (email)
);

/*Create Tables Table*/
CREATE TABLE tables(
    id                  UUID            NOT NULL,
    table_number        INTEGER         NOT NULL,
    seat_count          INTEGER         NOT NULL,
    created_at          TIMESTAMP       NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    CONSTRAINT  pk_tables PRIMARY KEY (id)
);

/*Create Reservations Table*/
CREATE TABLE reservations(
    id                  UUID            NOT NULL,
    customer_id         UUID            NOT NULL,
    table_id            UUID            NOT NULL,
    reservation_start   TIMESTAMP       NOT NULL,
    reservation_end     TIMESTAMP       NOT NULL,
    status              VARCHAR(20)     NOT NULL     DEFAULT 'PENDING' ,
    created_at          TIMESTAMP       NOT NULL     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reservations PRIMARY KEY (id),
    CONSTRAINT fk_reserved_by FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_reserved_table FOREIGN KEY (table_id) REFERENCES tables(id)
);