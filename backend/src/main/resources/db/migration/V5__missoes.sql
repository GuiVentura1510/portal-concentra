-- V5: missões. Criadas e editadas pelo staff no painel, não no código:
-- título, pontos, vagas, janela de horário e forma de conclusão.
create table missao (
    id         bigint generated always as identity primary key,
    titulo     varchar(120) not null,
    descricao  varchar(4000) not null default '',
    pontos     integer not null check (pontos >= 0),
    -- Quantas tripulações podem pontuar. Vazio = sem limite.
    vagas      integer check (vagas > 0),
    -- Janela em que a missão aceita conclusão. Vazio = sem limite naquele lado.
    inicio     timestamptz,
    fim        timestamptz,
    -- ENVIO_CAPITAO: o capitão envia uma resposta e o staff aprova.
    -- STAFF_MARCA: o staff marca como cumprida direto (ex.: ao carimbar o passaporte).
    conclusao  varchar(20) not null check (conclusao in ('ENVIO_CAPITAO', 'STAFF_MARCA')),
    -- Só missões visíveis aparecem para os congressistas.
    visivel    boolean not null default false,
    criada_em  timestamptz not null default now(),
    constraint ck_missao_janela check (inicio is null or fim is null or fim > inicio)
);
