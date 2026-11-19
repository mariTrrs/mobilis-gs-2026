-- Schema do banco solidario Mobilis.
-- Distancia geografica e calculada na aplicacao (Haversine), sem PostGIS.

CREATE TABLE usuario (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    email           VARCHAR(180) NOT NULL,
    senha_hash      VARCHAR(100) NOT NULL,
    cpf             VARCHAR(11),
    telefone        VARCHAR(20),
    perfil          VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_cpf UNIQUE (cpf),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('SOLICITANTE', 'OPERADOR', 'GESTOR', 'ADMIN')),
    CONSTRAINT ck_usuario_status CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADO')),
    CONSTRAINT ck_usuario_cpf CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$'),
    CONSTRAINT ck_usuario_solicitante_cpf CHECK (perfil <> 'SOLICITANTE' OR cpf IS NOT NULL)
);

CREATE TABLE instituicao (
    id                  BIGSERIAL PRIMARY KEY,
    nome                VARCHAR(150) NOT NULL,
    cnpj                VARCHAR(14)  NOT NULL,
    email               VARCHAR(180),
    telefone            VARCHAR(20)  NOT NULL,
    logradouro          VARCHAR(200) NOT NULL,
    numero              VARCHAR(20),
    bairro              VARCHAR(100) NOT NULL,
    cidade              VARCHAR(100) NOT NULL,
    uf                  CHAR(2)      NOT NULL,
    cep                 VARCHAR(8),
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    horario_retirada    VARCHAR(160) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    motivo_reprovacao   VARCHAR(500),
    criada_em           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizada_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_instituicao_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_instituicao_cnpj CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_instituicao_status CHECK (status IN ('PENDENTE', 'APROVADA', 'REPROVADA')),
    CONSTRAINT ck_instituicao_motivo CHECK (
        status <> 'REPROVADA'
        OR (motivo_reprovacao IS NOT NULL AND length(btrim(motivo_reprovacao)) > 0)
    ),
    CONSTRAINT ck_instituicao_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_instituicao_longitude CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_instituicao_latitude ON instituicao (latitude);
CREATE INDEX idx_instituicao_longitude ON instituicao (longitude);
CREATE INDEX idx_instituicao_status ON instituicao (status);

CREATE TABLE operador (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT      NOT NULL REFERENCES usuario (id),
    instituicao_id  BIGINT      NOT NULL REFERENCES instituicao (id),
    papel           VARCHAR(20) NOT NULL,
    ativo           BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_operador_usuario_instituicao UNIQUE (usuario_id, instituicao_id),
    CONSTRAINT ck_operador_papel CHECK (papel IN ('OPERADOR', 'GESTOR'))
);

CREATE INDEX idx_operador_instituicao ON operador (instituicao_id, ativo);

CREATE TABLE consentimento (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuario (id),
    finalidade  VARCHAR(80)  NOT NULL,
    versao      VARCHAR(20)  NOT NULL,
    aceito      BOOLEAN      NOT NULL,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE tipo_equipamento (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40)  NOT NULL,
    nome        VARCHAR(80)  NOT NULL,
    descricao   VARCHAR(255) NOT NULL,
    CONSTRAINT uk_tipo_equipamento_codigo UNIQUE (codigo)
);

CREATE TABLE equipamento (
    id                      BIGSERIAL PRIMARY KEY,
    codigo                  VARCHAR(40)  NOT NULL,
    instituicao_id          BIGINT       NOT NULL REFERENCES instituicao (id),
    tipo_equipamento_id     BIGINT       NOT NULL REFERENCES tipo_equipamento (id),
    descricao               VARCHAR(255) NOT NULL,
    estado_conservacao      VARCHAR(20)  NOT NULL,
    permite_emprestimo      BOOLEAN      NOT NULL,
    permite_doacao          BOOLEAN      NOT NULL,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'DISPONIVEL',
    motivo_baixa            VARCHAR(500),
    criado_em               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_equipamento_codigo UNIQUE (codigo),
    CONSTRAINT ck_equipamento_modalidade CHECK (permite_emprestimo OR permite_doacao),
    CONSTRAINT ck_equipamento_conservacao CHECK (
        estado_conservacao IN ('NOVO', 'OTIMO', 'BOM', 'REGULAR', 'RUIM')
    ),
    CONSTRAINT ck_equipamento_status CHECK (
        status IN (
            'DISPONIVEL',
            'RESERVADO',
            'EMPRESTADO',
            'DOADO',
            'DEVOLUCAO_PENDENTE',
            'EM_HIGIENIZACAO',
            'BAIXADO'
        )
    ),
    CONSTRAINT ck_equipamento_motivo_baixa CHECK (
        status <> 'BAIXADO'
        OR (motivo_baixa IS NOT NULL AND length(btrim(motivo_baixa)) > 0)
    )
);

CREATE INDEX idx_equipamento_instituicao_status ON equipamento (instituicao_id, status);

CREATE TABLE politica_instituicao (
    id                          BIGSERIAL PRIMARY KEY,
    instituicao_id              BIGINT  NOT NULL REFERENCES instituicao (id),
    tipo_equipamento_id         BIGINT  REFERENCES tipo_equipamento (id),
    equipamento_id              BIGINT  REFERENCES equipamento (id),
    permite_emprestimo          BOOLEAN NOT NULL,
    permite_doacao              BOOLEAN NOT NULL,
    prazo_padrao_dias           INTEGER NOT NULL,
    exige_comprovante_doacao    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_politica_alvo CHECK (tipo_equipamento_id IS NOT NULL OR equipamento_id IS NOT NULL),
    CONSTRAINT ck_politica_modalidade CHECK (permite_emprestimo OR permite_doacao),
    CONSTRAINT ck_politica_prazo CHECK (prazo_padrao_dias BETWEEN 1 AND 365)
);

CREATE UNIQUE INDEX uk_politica_por_tipo
    ON politica_instituicao (instituicao_id, tipo_equipamento_id)
    WHERE equipamento_id IS NULL AND tipo_equipamento_id IS NOT NULL;

CREATE UNIQUE INDEX uk_politica_por_item
    ON politica_instituicao (equipamento_id)
    WHERE equipamento_id IS NOT NULL;

CREATE TABLE foto_equipamento (
    id              BIGSERIAL PRIMARY KEY,
    equipamento_id  BIGINT       NOT NULL REFERENCES equipamento (id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    principal       BOOLEAN      NOT NULL DEFAULT FALSE,
    ordem           INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_foto_equipamento ON foto_equipamento (equipamento_id, principal);

CREATE TABLE historico_status (
    id                  BIGSERIAL PRIMARY KEY,
    equipamento_id      BIGINT      NOT NULL REFERENCES equipamento (id),
    status_anterior     VARCHAR(30),
    status_novo         VARCHAR(30) NOT NULL,
    motivo              VARCHAR(500),
    usuario_id          BIGINT REFERENCES usuario (id),
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_historico_equipamento ON historico_status (equipamento_id, criado_em);

CREATE TABLE manutencao (
    id              BIGSERIAL PRIMARY KEY,
    equipamento_id  BIGINT       NOT NULL REFERENCES equipamento (id),
    descricao       VARCHAR(500) NOT NULL,
    inicio          TIMESTAMPTZ  NOT NULL,
    fim             TIMESTAMPTZ,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ABERTA',
    CONSTRAINT ck_manutencao_status CHECK (status IN ('ABERTA', 'CONCLUIDA', 'CANCELADA'))
);

CREATE TABLE pedido (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT      NOT NULL REFERENCES usuario (id),
    equipamento_id      BIGINT      NOT NULL REFERENCES equipamento (id),
    instituicao_id      BIGINT      NOT NULL REFERENCES instituicao (id),
    modalidade          VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    motivo_recusa       VARCHAR(500),
    prazo_retirada      TIMESTAMPTZ NOT NULL,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_pedido_modalidade CHECK (modalidade IN ('EMPRESTIMO', 'DOACAO')),
    CONSTRAINT ck_pedido_status CHECK (
        status IN ('PENDENTE', 'APROVADO', 'RECUSADO', 'CANCELADO', 'CONCLUIDO', 'EXPIRADO')
    ),
    CONSTRAINT ck_pedido_motivo_recusa CHECK (
        status <> 'RECUSADO'
        OR (motivo_recusa IS NOT NULL AND length(btrim(motivo_recusa)) > 0)
    )
);

-- No maximo um pedido ativo (aguardando decisao ou retirada) por equipamento.
CREATE UNIQUE INDEX uk_pedido_um_ativo_por_equipamento
    ON pedido (equipamento_id)
    WHERE status IN ('PENDENTE', 'APROVADO');

CREATE INDEX idx_pedido_instituicao_status ON pedido (instituicao_id, status);
CREATE INDEX idx_pedido_usuario_status ON pedido (usuario_id, status);

CREATE TABLE fila_espera (
    id                      BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT           NOT NULL REFERENCES usuario (id),
    tipo_equipamento_id     BIGINT           NOT NULL REFERENCES tipo_equipamento (id),
    instituicao_id          BIGINT           REFERENCES instituicao (id),
    modalidade              VARCHAR(20)      NOT NULL,
    latitude                DOUBLE PRECISION,
    longitude               DOUBLE PRECISION,
    raio_km                 NUMERIC(6, 2),
    ativo                   BOOLEAN          NOT NULL DEFAULT TRUE,
    criado_em               TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_fila_modalidade CHECK (modalidade IN ('EMPRESTIMO', 'DOACAO'))
);

CREATE TABLE justificativa (
    id                  BIGSERIAL PRIMARY KEY,
    pedido_id           BIGINT        NOT NULL REFERENCES pedido (id),
    texto               VARCHAR(1000) NOT NULL,
    arquivo_nome        VARCHAR(255),
    arquivo_tipo        VARCHAR(80),
    arquivo_caminho     VARCHAR(500),
    criado_em           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE cessao (
    id                          BIGSERIAL PRIMARY KEY,
    pedido_id                   BIGINT      NOT NULL REFERENCES pedido (id),
    equipamento_id              BIGINT      NOT NULL REFERENCES equipamento (id),
    usuario_id                  BIGINT      NOT NULL REFERENCES usuario (id),
    instituicao_id              BIGINT      NOT NULL REFERENCES instituicao (id),
    modalidade                  VARCHAR(20) NOT NULL,
    data_retirada               TIMESTAMPTZ NOT NULL,
    data_prevista_devolucao     DATE,
    data_devolucao              TIMESTAMPTZ,
    status                      VARCHAR(20) NOT NULL,
    CONSTRAINT uk_cessao_pedido UNIQUE (pedido_id),
    CONSTRAINT ck_cessao_modalidade CHECK (modalidade IN ('EMPRESTIMO', 'DOACAO')),
    CONSTRAINT ck_cessao_status CHECK (status IN ('ATIVA', 'DEVOLVIDA', 'DOADA')),
    CONSTRAINT ck_cessao_prazo CHECK (
        (modalidade = 'DOACAO' AND data_prevista_devolucao IS NULL)
        OR (modalidade = 'EMPRESTIMO' AND data_prevista_devolucao IS NOT NULL)
    )
);

CREATE INDEX idx_cessao_equipamento ON cessao (equipamento_id, status);

CREATE TABLE proposta_modalidade (
    id                      BIGSERIAL PRIMARY KEY,
    cessao_id               BIGINT      NOT NULL REFERENCES cessao (id),
    modalidade_origem       VARCHAR(20) NOT NULL,
    modalidade_destino      VARCHAR(20) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    nova_data_final         DATE,
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_proposta_status CHECK (status IN ('PENDENTE', 'ACEITA', 'RECUSADA'))
);

CREATE TABLE termo_aceito (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuario (id),
    cessao_id   BIGINT       REFERENCES cessao (id),
    versao      VARCHAR(20)  NOT NULL,
    aceito_em   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE notificacao (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT        NOT NULL REFERENCES usuario (id),
    titulo      VARCHAR(160)  NOT NULL,
    mensagem    VARCHAR(1000) NOT NULL,
    lida        BOOLEAN       NOT NULL DEFAULT FALSE,
    criada_em   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notificacao_usuario ON notificacao (usuario_id, lida, criada_em);

CREATE TABLE preferencia_notificacao (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES usuario (id),
    email_ativo BOOLEAN     NOT NULL DEFAULT TRUE,
    criada_em   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_preferencia_usuario UNIQUE (usuario_id)
);

CREATE TABLE pedido_conversao_doacao (
    id              BIGSERIAL PRIMARY KEY,
    cessao_id       BIGINT      NOT NULL REFERENCES cessao (id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    nova_data_final DATE,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decidido_em     TIMESTAMPTZ,
    CONSTRAINT ck_conversao_status CHECK (status IN ('PENDENTE', 'ACEITA', 'RECUSADA'))
);

CREATE TABLE alerta_ociosidade (
    id              BIGSERIAL PRIMARY KEY,
    equipamento_id  BIGINT      NOT NULL REFERENCES equipamento (id),
    dias_ocioso     INTEGER     NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_alerta_dias CHECK (dias_ocioso >= 0)
);

CREATE TABLE indicacao_profissional (
    id                  BIGSERIAL PRIMARY KEY,
    pedido_id           BIGINT        NOT NULL REFERENCES pedido (id),
    nome_profissional   VARCHAR(150)  NOT NULL,
    conselho            VARCHAR(20)   NOT NULL,
    numero_registro     VARCHAR(30)   NOT NULL,
    texto               VARCHAR(1000),
    criado_em           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE convite (
    id              BIGSERIAL PRIMARY KEY,
    instituicao_id  BIGINT       NOT NULL REFERENCES instituicao (id),
    email           VARCHAR(180) NOT NULL,
    papel           VARCHAR(20)  NOT NULL,
    token           VARCHAR(80)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    expira_em       TIMESTAMPTZ  NOT NULL,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_convite_token UNIQUE (token),
    CONSTRAINT ck_convite_papel CHECK (papel IN ('OPERADOR', 'GESTOR')),
    CONSTRAINT ck_convite_status CHECK (status IN ('PENDENTE', 'ACEITO', 'EXPIRADO', 'CANCELADO'))
);

CREATE TABLE auditoria (
    id              BIGSERIAL PRIMARY KEY,
    acao            VARCHAR(60) NOT NULL,
    entidade        VARCHAR(60) NOT NULL,
    entidade_id     BIGINT,
    usuario_id      BIGINT REFERENCES usuario (id),
    detalhes        TEXT,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auditoria_entidade ON auditoria (entidade, entidade_id, criado_em);

CREATE OR REPLACE FUNCTION bloquear_alteracao_auditoria()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'A tabela auditoria é somente de inserção';
END;
$$;

CREATE TRIGGER trg_auditoria_somente_insercao
    BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW
    EXECUTE FUNCTION bloquear_alteracao_auditoria();

CREATE TABLE avaliacao (
    id          BIGSERIAL PRIMARY KEY,
    cessao_id   BIGINT        NOT NULL REFERENCES cessao (id),
    usuario_id  BIGINT        NOT NULL REFERENCES usuario (id),
    nota        INTEGER       NOT NULL,
    comentario  VARCHAR(1000),
    criado_em   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_avaliacao_cessao_usuario UNIQUE (cessao_id, usuario_id),
    CONSTRAINT ck_avaliacao_nota CHECK (nota BETWEEN 1 AND 5)
);

COMMENT ON TABLE usuario IS 'Conta autenticavel. Solicitante tem CPF unico; o perfil vai no token JWT.';
COMMENT ON TABLE instituicao IS 'ONG, igreja ou hospital que cede equipamentos. Nasce PENDENTE ate o ADMIN decidir.';
COMMENT ON TABLE operador IS 'Vinculo de operador ou gestor com uma instituicao.';
COMMENT ON TABLE tipo_equipamento IS 'Catalogo fixo dos tipos de apoio a saude.';
COMMENT ON TABLE politica_instituicao IS 'Modalidades e prazo padrao por tipo ou por item.';
COMMENT ON TABLE equipamento IS 'Item fisico identificado por codigo unico, usado tambem no QR code.';
COMMENT ON TABLE pedido IS 'Solicitacao de emprestimo ou doacao. So pode haver um pedido ativo por item.';
COMMENT ON TABLE cessao IS 'Retirada efetivada. Emprestimo tem prazo; doacao e terminal.';
COMMENT ON TABLE auditoria IS 'Trilha somente de insercao para retirada, devolucao, baixa e aprovacao.';
