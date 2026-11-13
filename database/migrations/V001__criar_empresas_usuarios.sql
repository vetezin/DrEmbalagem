-- V001 — Login, empresas e usuários (Task 1)
-- Cria as tabelas empresa, usuario e usuario_permissao conforme
-- docs/superpowers/specs/2026-11-12-login-empresas-usuarios-design.md.
-- Transacional: aplicar por psql com -X -v ON_ERROR_STOP=1.

BEGIN;

CREATE TABLE empresa (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    razao_social  VARCHAR(200) NOT NULL,
    nome_fantasia VARCHAR(200),
    cnpj          VARCHAR(14)  NOT NULL,
    cep           VARCHAR(8),
    logradouro    VARCHAR(200),
    numero        VARCHAR(20),
    complemento   VARCHAR(120),
    bairro        VARCHAR(120),
    cidade        VARCHAR(120),
    uf            VARCHAR(2),
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_empresa_cnpj UNIQUE (cnpj)
);

CREATE TABLE usuario (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_id              BIGINT,
    nome                    VARCHAR(120) NOT NULL,
    email                   VARCHAR(254) NOT NULL,
    senha_hash              VARCHAR(255) NOT NULL,
    nivel                   VARCHAR(20)  NOT NULL,
    troca_senha_obrigatoria BOOLEAN      NOT NULL,
    ativo                   BOOLEAN      NOT NULL,
    criado_em               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_usuario_empresa FOREIGN KEY (empresa_id)
        REFERENCES empresa (id) ON DELETE RESTRICT,
    CONSTRAINT ck_usuario_nivel
        CHECK (nivel IN ('SUPERADMIN', 'ADMIN', 'CONSULTOR')),
    CONSTRAINT ck_usuario_nivel_empresa
        CHECK ((nivel = 'SUPERADMIN' AND empresa_id IS NULL)
            OR (nivel <> 'SUPERADMIN' AND empresa_id IS NOT NULL))
);

CREATE UNIQUE INDEX uq_usuario_email_lower ON usuario (lower (email));
CREATE INDEX ix_usuario_empresa ON usuario (empresa_id);

CREATE TABLE usuario_permissao (
    usuario_id BIGINT      NOT NULL,
    permissao  VARCHAR(80) NOT NULL,
    PRIMARY KEY (usuario_id, permissao),
    CONSTRAINT fk_usuario_permissao_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (id) ON DELETE CASCADE
);

COMMIT;
