-- V7: missões individuais.
-- Uma missão passa a ter um alcance: da tripulação (como até agora) ou individual,
-- cumprida por cada pessoa. Conclusões e lançamentos individuais guardam a pessoa
-- e, quando ela já tem tripulação, também a tripulação dela.
-- Assim, decidir se os pontos individuais somam no ranking da tripulação é só
-- uma configuração da consulta do ranking, sem mudar o banco.

alter table missao
    add column alcance varchar(20) not null default 'TRIPULACAO'
        check (alcance in ('TRIPULACAO', 'INDIVIDUAL'));

-- Conclusões: a pessoa (nas individuais) e a tripulação deixa de ser obrigatória,
-- porque alguém pode cumprir uma missão individual antes da montagem.
alter table conclusao alter column tripulacao_id drop not null;
alter table conclusao add column congressista_id bigint references congressista (id);
alter table conclusao add constraint ck_conclusao_dono
    check (tripulacao_id is not null or congressista_id is not null);

-- A regra "uma conclusão ativa por tripulação e missão" passa a valer só para as
-- missões da tripulação; nas individuais, vale uma por pessoa e missão.
drop index ux_conclusao_ativa;
create unique index ux_conclusao_ativa on conclusao (missao_id, tripulacao_id)
    where status in ('PENDENTE', 'APROVADA') and congressista_id is null;
create unique index ux_conclusao_individual_ativa on conclusao (missao_id, congressista_id)
    where status in ('PENDENTE', 'APROVADA') and congressista_id is not null;
create index ix_conclusao_congressista on conclusao (congressista_id);

-- Lançamentos: mesma ideia.
alter table lancamento alter column tripulacao_id drop not null;
alter table lancamento add column congressista_id bigint references congressista (id);
alter table lancamento add constraint ck_lancamento_dono
    check (tripulacao_id is not null or congressista_id is not null);
create index ix_lancamento_congressista on lancamento (congressista_id);
