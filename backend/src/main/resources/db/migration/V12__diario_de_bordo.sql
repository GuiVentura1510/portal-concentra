-- Fotos que os integrantes anexam ao Diário de Bordo da tripulação (aba Tripulação),
-- direto do celular. Não valem pontos. Quem anexou pode apagar a sua; o staff pode
-- apagar qualquer uma. Ficam no banco porque o disco do Render não é permanente.
create table foto_diario (
    id            bigint generated always as identity primary key,
    -- O diário é da tripulação: desfazer a montagem apaga as fotos junto.
    tripulacao_id bigint not null references tripulacao (id) on delete cascade,
    -- Remover um cadastro apaga as fotos que a pessoa anexou.
    autor_id      bigint not null references congressista (id) on delete cascade,
    -- Código sorteado no envio, usado no endereço da imagem.
    chave         varchar(36) not null unique,
    -- Um JPEG já reduzido pelo site.
    conteudo      bytea not null,
    -- Escrita por quem anexou (opcional). Curta para caber no rodapé da polaroide.
    legenda       varchar(30),
    criada_em     timestamptz not null default now()
);

create index ix_foto_diario_tripulacao on foto_diario (tripulacao_id);
