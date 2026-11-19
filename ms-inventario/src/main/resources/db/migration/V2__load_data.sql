INSERT INTO tb_instituicao (nome, cnpj, endereco, bairro, cidade, telefone, latitude, longitude) VALUES
('Associacao Esperanca', '12.345.678/0001-90', 'Rua das Flores, 100', 'Centro', 'Sao Paulo', '11999990001', -23.550520, -46.633308),
('Igreja Sao Vicente', '98.765.432/0001-10', 'Av. Brasil, 500', 'Mooca', 'Sao Paulo', '11999990002', -23.548900, -46.597800),
('ONG Mobilidade Livre', '11.222.333/0001-44', 'Rua da Paz, 45', 'Pinheiros', 'Sao Paulo', '11999990003', -23.561400, -46.699000);

INSERT INTO tb_equipamento (codigo_tag, tipo, descricao, condicao, instituicao_id, status) VALUES
('EQ-001', 'Cadeira de Rodas', 'Cadeira de rodas adulta padrao', 'BOA', 1, 'DISPONIVEL'),
('EQ-002', 'Muleta', 'Par de muletas axilares', 'BOA', 1, 'DISPONIVEL'),
('EQ-003', 'Andador', 'Andador com rodas', 'REGULAR', 2, 'DISPONIVEL'),
('EQ-004', 'Cama Hospitalar', 'Cama hospitalar manual', 'BOA', 2, 'EM_MANUTENCAO'),
('EQ-005', 'Cadeira de Banho', 'Cadeira de banho com bracos', 'BOA', 3, 'DISPONIVEL');
