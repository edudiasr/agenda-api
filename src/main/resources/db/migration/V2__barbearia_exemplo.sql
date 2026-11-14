-- Dados de exemplo: uma barbearia com dois profissionais.

INSERT INTO servico (nome, duracao_minutos, preco_centavos) VALUES
    ('Corte', 30, 4500),
    ('Barba', 30, 3500),
    ('Corte e barba', 60, 7000),
    ('Sobrancelha', 15, 1500);

INSERT INTO profissional (nome) VALUES ('Rafael'), ('Bruno');

-- Rafael: terça a sábado, 9h às 18h
INSERT INTO expediente (profissional_id, dia_semana, inicio, fim)
SELECT 1, d, '09:00', '18:00' FROM generate_series(2, 6) AS d;

-- Bruno: segunda a sexta, 10h às 19h
INSERT INTO expediente (profissional_id, dia_semana, inicio, fim)
SELECT 2, d, '10:00', '19:00' FROM generate_series(1, 5) AS d;
