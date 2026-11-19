-- Carga local para demonstracao. Senhas em texto ficam apenas no README.
-- Hashes BCrypt (custo 10) gerados para o ambiente de desenvolvimento.

INSERT INTO usuario (nome, email, senha_hash, cpf, telefone, perfil, status) VALUES
(
    'Admin Mobilis',
    'admin@mobilis.dev',
    '$2a$10$nO86UtMaGM4dUpZ976I3Feez0W3Nok4HolGuDjHH2xCwiC.KQSnF2',
    '39053344705',
    '11900000001',
    'ADMIN',
    'ATIVO'
),
(
    'Helena Martins',
    'gestor@mobilis.dev',
    '$2a$10$aHWibLqw130cWU4ox.FE/uiSPU0C6VWNX.xWYvP0sOgPXzx4mqY46',
    '52998224725',
    '11900000002',
    'GESTOR',
    'ATIVO'
),
(
    'Ana Costa',
    'ana.solicitante@mobilis.dev',
    '$2a$10$P3wHKWJD823IoiZYgnVmLuZJUMchPcvvxodGWplSXGxSUiYU0Y92K',
    '11144477735',
    '11900000003',
    'SOLICITANTE',
    'ATIVO'
);

INSERT INTO instituicao (
    nome, cnpj, email, telefone, logradouro, numero, bairro, cidade, uf, cep,
    latitude, longitude, horario_retirada, status
) VALUES
(
    'Associação Esperança',
    '12345678000195',
    'contato@esperanca.org.br',
    '11999990001',
    'Rua das Flores',
    '100',
    'Centro',
    'São Paulo',
    'SP',
    '01001000',
    -23.550520,
    -46.633308,
    'Segunda a sexta, 9h às 17h',
    'APROVADA'
),
(
    'Igreja São Vicente',
    '98765432000198',
    'secretaria@igrejasaovicente.org.br',
    '11999990002',
    'Avenida Brasil',
    '500',
    'Mooca',
    'São Paulo',
    'SP',
    '03102000',
    -23.548900,
    -46.597800,
    'Segunda a sábado, 8h às 16h',
    'APROVADA'
),
(
    'ONG Mobilidade Livre',
    '11222333000181',
    'contato@mobilidadelivre.org.br',
    '11999990003',
    'Rua da Paz',
    '45',
    'Pinheiros',
    'São Paulo',
    'SP',
    '05422000',
    -23.566700,
    -46.691000,
    'Terça a sexta, 10h às 18h',
    'APROVADA'
);

INSERT INTO operador (usuario_id, instituicao_id, papel)
SELECT u.id, i.id, 'GESTOR'
FROM usuario u
JOIN instituicao i ON i.cnpj = '12345678000195'
WHERE u.email = 'gestor@mobilis.dev';

INSERT INTO tipo_equipamento (codigo, nome, descricao) VALUES
('CADEIRA_RODAS', 'Cadeira de rodas', 'Cadeira de rodas manual ou motorizada para deslocamento'),
('CADEIRA_BANHO', 'Cadeira de banho', 'Cadeira para higiene com apoio e rodizios'),
('ANDADOR', 'Andador', 'Andador fixo ou com rodas para marcha assistida'),
('MULETA', 'Muleta', 'Muleta axilar ou canadense'),
('BENGALA', 'Bengala', 'Bengala de apoio simples ou de quatro pontas'),
('CAMA_HOSPITALAR', 'Cama hospitalar', 'Cama com regulagem de altura e grades');

INSERT INTO politica_instituicao (
    instituicao_id, tipo_equipamento_id, permite_emprestimo, permite_doacao,
    prazo_padrao_dias, exige_comprovante_doacao
)
SELECT
    i.id,
    t.id,
    TRUE,
    TRUE,
    CASE t.codigo
        WHEN 'MULETA' THEN 20
        WHEN 'BENGALA' THEN 15
        WHEN 'CAMA_HOSPITALAR' THEN 45
        ELSE 30
    END,
    FALSE
FROM instituicao i
CROSS JOIN tipo_equipamento t;

INSERT INTO equipamento (
    codigo, instituicao_id, tipo_equipamento_id, descricao,
    estado_conservacao, permite_emprestimo, permite_doacao, status
)
SELECT v.codigo, i.id, t.id, v.descricao, v.conservacao, v.emprestimo, v.doacao, 'DISPONIVEL'
FROM (
    VALUES
        ('MOB-ESP-001', '12345678000195', 'CADEIRA_RODAS', 'Cadeira de rodas adulta, dobrável, com aro de propulsão', 'BOM', TRUE, TRUE),
        ('MOB-ESP-002', '12345678000195', 'CADEIRA_BANHO', 'Cadeira de banho com encosto e braços removíveis', 'OTIMO', TRUE, FALSE),
        ('MOB-ESP-003', '12345678000195', 'ANDADOR', 'Andador articulado com duas rodas dianteiras', 'BOM', TRUE, TRUE),
        ('MOB-ESP-004', '12345678000195', 'MULETA', 'Par de muletas axilares reguláveis', 'REGULAR', TRUE, FALSE),
        ('MOB-ESP-005', '12345678000195', 'BENGALA', 'Bengala tipo T com ponteira antiderrapante', 'OTIMO', FALSE, TRUE),
        ('MOB-ESP-006', '12345678000195', 'CAMA_HOSPITALAR', 'Cama hospitalar manual com duas manivelas e grades', 'BOM', TRUE, FALSE),
        ('MOB-ISV-001', '98765432000198', 'CADEIRA_RODAS', 'Cadeira de rodas infantil com cinto', 'BOM', TRUE, FALSE),
        ('MOB-ISV-002', '98765432000198', 'CADEIRA_BANHO', 'Cadeira de banho com assento sanitario', 'BOM', TRUE, TRUE),
        ('MOB-ISV-003', '98765432000198', 'ANDADOR', 'Andador fixo com quatro pontas de apoio', 'REGULAR', TRUE, FALSE),
        ('MOB-ISV-004', '98765432000198', 'MULETA', 'Par de muletas canadenses', 'OTIMO', TRUE, TRUE),
        ('MOB-ISV-005', '98765432000198', 'BENGALA', 'Bengala de quatro apoios', 'BOM', TRUE, FALSE),
        ('MOB-ISV-006', '98765432000198', 'CAMA_HOSPITALAR', 'Cama hospitalar com colchão impermeável', 'OTIMO', TRUE, FALSE),
        ('MOB-MLV-001', '11222333000181', 'CADEIRA_RODAS', 'Cadeira de rodas motorizada de uso eventual', 'BOM', TRUE, FALSE),
        ('MOB-MLV-002', '11222333000181', 'CADEIRA_BANHO', 'Cadeira de banho com rodas traváveis', 'BOM', FALSE, TRUE),
        ('MOB-MLV-003', '11222333000181', 'ANDADOR', 'Andador com rodas e assento', 'OTIMO', TRUE, TRUE),
        ('MOB-MLV-004', '11222333000181', 'MULETA', 'Muleta canadense avulsa, lado esquerdo', 'REGULAR', FALSE, TRUE),
        ('MOB-MLV-005', '11222333000181', 'BENGALA', 'Bengala dobrável de alumínio', 'BOM', TRUE, TRUE),
        ('MOB-MLV-006', '11222333000181', 'CAMA_HOSPITALAR', 'Cama hospitalar elétrica com controle', 'BOM', TRUE, FALSE)
) AS v(codigo, cnpj, tipo, descricao, conservacao, emprestimo, doacao)
JOIN instituicao i ON i.cnpj = v.cnpj
JOIN tipo_equipamento t ON t.codigo = v.tipo;

INSERT INTO foto_equipamento (equipamento_id, url, principal, ordem)
SELECT id, 'https://cdn.mobilis.dev/fotos/' || codigo || '.jpg', TRUE, 0
FROM equipamento;

INSERT INTO historico_status (equipamento_id, status_anterior, status_novo, motivo)
SELECT id, NULL, 'DISPONIVEL', 'Cadastro inicial do item'
FROM equipamento;
