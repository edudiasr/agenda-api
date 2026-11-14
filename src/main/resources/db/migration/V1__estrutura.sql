-- btree_gist permite usar "=" num índice GiST junto com o operador de sobreposição de intervalos (&&)
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE servico (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(80) NOT NULL UNIQUE,
    duracao_minutos INT         NOT NULL CHECK (duracao_minutos BETWEEN 15 AND 240 AND duracao_minutos % 15 = 0),
    preco_centavos  INT         NOT NULL CHECK (preco_centavos >= 0),
    ativo           BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE profissional (
    id    BIGSERIAL PRIMARY KEY,
    nome  VARCHAR(80) NOT NULL,
    ativo BOOLEAN     NOT NULL DEFAULT TRUE
);

-- horário de trabalho de cada profissional por dia da semana (1 = segunda ... 7 = domingo)
CREATE TABLE expediente (
    id              BIGSERIAL PRIMARY KEY,
    profissional_id BIGINT   NOT NULL REFERENCES profissional (id),
    dia_semana      SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    inicio          TIME     NOT NULL,
    fim             TIME     NOT NULL,
    CHECK (inicio < fim),
    UNIQUE (profissional_id, dia_semana)
);

CREATE TABLE agendamento (
    id               BIGSERIAL PRIMARY KEY,
    codigo           VARCHAR(8)  NOT NULL UNIQUE,
    profissional_id  BIGINT      NOT NULL REFERENCES profissional (id),
    servico_id       BIGINT      NOT NULL REFERENCES servico (id),
    cliente_nome     VARCHAR(80) NOT NULL,
    cliente_telefone VARCHAR(20) NOT NULL,
    inicio           TIMESTAMPTZ NOT NULL,
    fim              TIMESTAMPTZ NOT NULL,
    status           VARCHAR(12) NOT NULL DEFAULT 'AGENDADO'
                     CHECK (status IN ('AGENDADO', 'CANCELADO', 'CONCLUIDO', 'FALTOU')),
    criado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (inicio < fim),
    -- O banco garante que o mesmo profissional não tem dois horários que se sobrepõem.
    -- Cancelados não contam, então o horário volta a ficar livre.
    CONSTRAINT agendamento_sem_sobreposicao
        EXCLUDE USING gist (profissional_id WITH =, tstzrange(inicio, fim) WITH &&)
        WHERE (status <> 'CANCELADO')
);
CREATE INDEX agendamento_profissional_inicio ON agendamento (profissional_id, inicio);

CREATE TABLE usuario (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(120) NOT NULL UNIQUE,
    nome       VARCHAR(80)  NOT NULL,
    senha_hash VARCHAR(100) NOT NULL
);
