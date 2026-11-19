CREATE TABLE tb_instituicao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cnpj VARCHAR(18) NOT NULL UNIQUE,
    endereco VARCHAR(200) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL
);

CREATE TABLE tb_equipamento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_tag VARCHAR(50) NOT NULL UNIQUE,
    tipo VARCHAR(80) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    condicao VARCHAR(80) NOT NULL,
    instituicao_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_equipamento_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicao (id)
);
