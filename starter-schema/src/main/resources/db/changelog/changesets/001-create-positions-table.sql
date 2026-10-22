--liquibase formatted sql

--changeset starter:001-create-positions-table
CREATE TABLE positions (
    id UUID NOT NULL,
    isin VARCHAR(12) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    price NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT positions_pkey PRIMARY KEY (id)
);
