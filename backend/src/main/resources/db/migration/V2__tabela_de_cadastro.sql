-- V2: tabelas de cadastro.
-- Cria as EJs (lista fechada, para o algoritmo de tripulações conseguir
-- separar pessoas da mesma empresa) e os congressistas, com as duas
-- respostas do questionário como colunas.

create table ej (
    id   bigint generated always as identity primary key,
    nome varchar(100) not null unique
);

create table congressista (
    id           bigint generated always as identity primary key,
    nome         varchar(120) not null,
    email        varchar(254) not null unique check (email = lower(email)),
    senha_hash   varchar(100) not null,
    ej_id        bigint not null references ej (id),
    tempo_mej    varchar(30) check (tempo_mej in ('MENOS_DE_6_MESES', 'DE_6_MESES_A_1_ANO', 'DE_1_A_2_ANOS', 'MAIS_DE_2_ANOS')),
    area_atuacao varchar(50) check (area_atuacao in ('PRESIDENCIA', 'PROJETOS', 'COMERCIAL', 'MARKETING', 'OPERACOES')),
    criado_em    timestamptz not null default now(),
    constraint ck_questionario_completo
        check (num_nulls(tempo_mej, area_atuacao) in (0, 2))
);


-- EJs mapeadas para o Concentra DF 2026. Uma EJ nova entra por outra
-- migração, nunca editando esta.
insert into ej (nome) values
    ('ESTAT'),
    ('Domani'),
    ('Concreta'),
    ('Orc''estra'),
    ('Nutrir'),
    ('Doisnovemeia'),
    ('Facto'),
    ('Matriz'),
    ('AdvocattA'),
    ('Strategos'),
    ('Cajuí'),
    ('Capital'),
    ('Pupila'),
    ('Publicae'),
    ('EngNet'),
    ('CONEX'),
    ('Lógica'),
    ('Econsult'),
    ('Praxis'),
    ('Flua'),
    ('Genesys'),
    ('Terapêutica'),
    ('Mecajun'),
    ('Grupo Gestão'),
    ('CJR'),
    ('CSTQ Jr.'),
    ('ENETEC'),
    ('Lamparina'),
    ('Aracá'),
    ('Zenit'),
    ('PCI'),
    ('Ábaco'),
    ('AD&M'),
    ('ATELIÊ MUDA'),
    ('Avenir'),
    ('Axia'),
    ('Confie Jr'),
    ('Cráton'),
    ('DatAí'),
    ('ECOFLOR'),
    ('EJOI'),
    ('EletronJun'),
    ('EMBRAGEA'),
    ('Endêmica'),
    ('ENTRE'),
    ('Ítaca'),
    ('META'),
    ('Phygeo'),
    ('Polaris'),
    ('PROADE'),
    ('QuarkUp'),
    ('Quimera'),
    ('Struct'),
    ('Zahir');
