CREATE TABLE tb_comodato (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_solicitante VARCHAR(150) NOT NULL,
    documento_solicitante VARCHAR(20) NOT NULL,
    equipamento_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_reserva DATE NOT NULL,
    prazo_devolucao DATE NOT NULL,
    consentimento_lgpd BOOLEAN NOT NULL
);
