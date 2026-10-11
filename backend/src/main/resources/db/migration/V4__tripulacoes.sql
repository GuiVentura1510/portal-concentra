-- V4: tripulações. Cada congressista entra em uma tripulação de 5 a 6 pessoas,
-- montada pelo portal em 15/10. Quem se cadastra depois é encaixado na hora.
create table tripulacao (
    id             bigint generated always as identity primary key,
    nome           varchar(60) not null unique,
    -- Onde a tripulação se encontra no evento. O staff preenche depois.
    ponto_encontro varchar(200),
    -- Escolhido pela própria tripulação no evento e registrado pelo staff.
    capitao_id     bigint,
    criada_em      timestamptz not null default now()
);

alter table congressista
    add column tripulacao_id bigint references tripulacao (id);

alter table tripulacao
    add constraint fk_tripulacao_capitao foreign key (capitao_id) references congressista (id);

create index ix_congressista_tripulacao on congressista (tripulacao_id);
