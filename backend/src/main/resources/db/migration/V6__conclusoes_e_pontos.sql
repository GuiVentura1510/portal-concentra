-- V6: conclusão das missões e pontuação.
-- Regras do README: vagas reservadas com operação atômica, uma conclusão válida
-- por tripulação e missão, e a pontuação como extrato de lançamentos.

-- Quantas vagas da missão já foram ocupadas. Só cresce por um "update ... where"
-- condicional, que é atômico mesmo com vários staffs aprovando ao mesmo tempo.
alter table missao
    add column vagas_usadas integer not null default 0 check (vagas_usadas >= 0);

create table conclusao (
    id            bigint generated always as identity primary key,
    missao_id     bigint not null references missao (id),
    tripulacao_id bigint not null references tripulacao (id),
    -- PENDENTE: o capitão enviou e o staff ainda não avaliou.
    status        varchar(20) not null check (status in ('PENDENTE', 'APROVADA', 'RECUSADA')),
    -- Texto enviado pelo capitão. Vazio quando o staff marca direto.
    resposta      varchar(4000),
    enviada_por   bigint references congressista (id),
    enviada_em    timestamptz not null default now(),
    avaliada_por  bigint references congressista (id),
    avaliada_em   timestamptz,
    motivo_recusa varchar(500)
);

-- No máximo uma conclusão pendente ou aprovada por tripulação e missão.
-- Uma recusada não conta, então a tripulação pode reenviar.
create unique index ux_conclusao_ativa on conclusao (missao_id, tripulacao_id)
    where status in ('PENDENTE', 'APROVADA');

create index ix_conclusao_status on conclusao (status);

-- Extrato de pontos. O total de uma tripulação é sempre a soma dos lançamentos.
create table lancamento (
    id            bigint generated always as identity primary key,
    tripulacao_id bigint not null references tripulacao (id),
    missao_id     bigint references missao (id),
    -- Cada conclusão aprovada gera no máximo um lançamento.
    conclusao_id  bigint unique references conclusao (id),
    pontos        integer not null,
    descricao     varchar(200) not null,
    criado_por    bigint references congressista (id),
    criado_em     timestamptz not null default now()
);

create index ix_lancamento_tripulacao on lancamento (tripulacao_id);
