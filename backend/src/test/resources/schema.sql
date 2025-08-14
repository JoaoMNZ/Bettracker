-- ========= ENUMERATIONS =========
CREATE TYPE bettor_type AS ENUM ('FREE', 'PREMIUM');
CREATE TYPE transaction_type AS ENUM ('DEPOSIT', 'WITHDRAWAL');
CREATE TYPE bet_status AS ENUM ('PENDING', 'WON', 'LOST', 'PUSH', 'HALF_WON', 'HALF_LOST', 'CASHOUT', 'VOID');
CREATE TYPE stake_type AS ENUM ('VALUE', 'UNIT');

-- ========= TABLES =========
CREATE TABLE bettor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    type bettor_type NOT NULL DEFAULT 'FREE',
    unit_value DECIMAL(8,2),
    created_at TIMESTAMP NOT NULL DEFAULT current_timestamp
);

CREATE TABLE bookmaker (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    bettor_id BIGINT NOT NULL,
    CONSTRAINT fk_bookmaker_bettor FOREIGN KEY(bettor_id) REFERENCES bettor(id) ON DELETE CASCADE
);

CREATE TABLE tipster (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    bettor_id BIGINT NOT NULL,
    CONSTRAINT fk_tipster_bettor FOREIGN KEY(bettor_id) REFERENCES bettor(id) ON DELETE CASCADE
);

CREATE TABLE sport (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    bettor_id BIGINT NOT NULL,
    CONSTRAINT fk_sport_bettor FOREIGN KEY(bettor_id) REFERENCES bettor(id) ON DELETE CASCADE
);

CREATE TABLE competition (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    bettor_id BIGINT NOT NULL,
    sport_id BIGINT NOT NULL,
    CONSTRAINT fk_competition_bettor FOREIGN KEY(bettor_id) REFERENCES bettor(id) ON DELETE CASCADE,
    CONSTRAINT fk_competition_sport FOREIGN KEY(sport_id) REFERENCES sport(id) ON DELETE CASCADE
);

CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    amount DECIMAL(8,2) NOT NULL,
    type transaction_type NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT current_timestamp,
    bettor_id BIGINT NOT NULL,
    bookmaker_id BIGINT NOT NULL,
    CONSTRAINT fk_transaction_bettor FOREIGN KEY(bettor_id) REFERENCES bettor(id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_bookmaker FOREIGN KEY(bookmaker_id) REFERENCES bookmaker(id) ON DELETE CASCADE
);

CREATE TABLE bet (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    selection VARCHAR(255) NOT NULL,
    stake DECIMAL(8,2) NOT NULL,
    stake_type stake_type NOT NULL DEFAULT 'VALUE',
    odds DECIMAL(8,2) NOT NULL,
    status bet_status NOT NULL DEFAULT 'PENDING',
    event_date TIMESTAMP NOT NULL DEFAULT current_timestamp,
    bettor_id BIGINT NOT NULL,
    bookmaker_id BIGINT,
    tipster_id BIGINT,
    sport_id BIGINT,
    competition_id BIGINT,
    CONSTRAINT fk_bet_bettor FOREIGN KEY(bettor_id) REFERENCES bettor(id) ON DELETE CASCADE,
    CONSTRAINT fk_bet_bookmaker FOREIGN KEY(bookmaker_id) REFERENCES bookmaker(id) ON DELETE SET NULL,
    CONSTRAINT fk_bet_tipster FOREIGN KEY(tipster_id) REFERENCES tipster(id) ON DELETE SET NULL,
    CONSTRAINT fk_bet_sport FOREIGN KEY(sport_id) REFERENCES sport(id) ON DELETE SET NULL,
    CONSTRAINT fk_bet_competition FOREIGN KEY(competition_id) REFERENCES competition(id) ON DELETE SET NULL
);

-- ========= INDEXES =========
CREATE INDEX idx_bookmaker_bettor_id ON bookmaker(bettor_id);
CREATE INDEX idx_tipster_bettor_id ON tipster(bettor_id);
CREATE INDEX idx_sport_bettor_id ON sport(bettor_id);
CREATE INDEX idx_competition_bettor_id ON competition(bettor_id);
CREATE INDEX idx_competition_sport_id ON competition(sport_id);
CREATE INDEX idx_transaction_bettor_id ON transaction(bettor_id);
CREATE INDEX idx_transaction_bookmaker_id ON transaction(bookmaker_id);
CREATE INDEX idx_bet_bettor_id ON bet(bettor_id);
CREATE INDEX idx_bet_bookmaker_id ON bet(bookmaker_id);
CREATE INDEX idx_bet_tipster_id ON bet(tipster_id);
CREATE INDEX idx_bet_sport_id ON bet(sport_id);
CREATE INDEX idx_bet_competition_id ON bet(competition_id);