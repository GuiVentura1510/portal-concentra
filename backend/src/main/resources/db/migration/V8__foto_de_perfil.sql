-- V8: foto de perfil.
-- A foto chega do site já reduzida (JPEG pequeno, de 256 px) e fica no próprio
-- banco, porque o disco do Render não é permanente. Fica em tabela separada para
-- as consultas de congressista não carregarem os bytes da imagem.
create table foto_perfil (
    -- Uma foto por pessoa. Apagar o cadastro apaga a foto junto.
    congressista_id bigint primary key references congressista (id) on delete cascade,
    -- Código sorteado a cada envio. É ele que vai no endereço da imagem: assim
    -- ninguém acha fotos trocando o número do cadastro, e o navegador pode guardar
    -- a imagem para sempre (foto nova = endereço novo).
    chave           varchar(36) not null unique,
    conteudo        bytea not null,
    enviada_em      timestamptz not null default now()
);

-- Cópia da chave no cadastro: as listas (tripulação, ranking, painel do staff)
-- montam o endereço da foto sem consultar a tabela das imagens.
alter table congressista
    add column foto_chave varchar(36);
