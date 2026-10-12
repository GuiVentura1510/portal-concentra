-- V10: foto como comprovação das missões (pedido do PO em 12/10).
-- O staff escolhe, em cada missão, o que o envio pede:
--   TEXTO          só texto (como até agora);
--   TEXTO_OU_FOTO  texto, foto ou os dois (pelo menos um);
--   FOTO           foto obrigatória (texto opcional).
alter table missao
    add column comprovacao varchar(20) not null default 'TEXTO'
        check (comprovacao in ('TEXTO', 'TEXTO_OU_FOTO', 'FOTO'));

-- Uma foto por envio. Fica no banco, como a foto de perfil (V8), porque o disco
-- do Render não é permanente; em tabela separada, para as listas de conclusões
-- não carregarem os bytes. Apagar a conclusão apaga a foto.
create table foto_envio (
    conclusao_id bigint primary key references conclusao (id) on delete cascade,
    -- Código sorteado no envio, usado no endereço da imagem.
    chave        varchar(36) not null unique,
    conteudo     bytea not null
);

-- Cópia da chave na conclusão, para montar o endereço sem ler a imagem.
alter table conclusao
    add column foto_chave varchar(36);
