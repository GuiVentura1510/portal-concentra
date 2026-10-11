-- V3: cargo do congressista na EJ, perguntado no cadastro.
-- A tabela ainda está vazia em produção, então a coluna já nasce obrigatória.
alter table congressista
    add column cargo varchar(30) not null
        check (cargo in ('ASSESSOR_CONSULTOR', 'GERENTE_COORDENADOR', 'DIRETOR', 'PRESIDENTE', 'POS_JUNIOR'));
