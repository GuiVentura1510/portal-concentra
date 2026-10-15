-- V11: mais de uma foto por envio de missão (pedido do Gui em 14/10).
-- O staff escolhe, em cada missão, quantas fotos o envio aceita, de 1 a 5.
-- Missões já criadas ficam com 1, que era o comportamento até agora.
alter table missao
    add column max_fotos integer not null default 1
        check (max_fotos between 1 and 5);

-- A foto deixa de ser identificada pela conclusão (uma por envio) e passa a ser
-- identificada pela própria chave, que já era única. "ordem" guarda a posição da
-- foto no envio (0, 1, 2...); as fotos de antes ficam na posição 0.
alter table foto_envio drop constraint foto_envio_pkey;
alter table foto_envio alter column conclusao_id set not null;
alter table foto_envio add column ordem integer not null default 0;
alter table foto_envio drop constraint foto_envio_chave_key;
alter table foto_envio add constraint foto_envio_pkey primary key (chave);
create unique index ux_foto_envio_ordem on foto_envio (conclusao_id, ordem);

-- Chaves de todas as fotos do envio, separadas por vírgula (até 5 × 36 caracteres),
-- para as listas montarem os endereços sem ler as imagens. "foto_chave" continua
-- com a primeira, para o site antigo funcionar durante a publicação.
alter table conclusao
    add column fotos_chaves varchar(200);
update conclusao set fotos_chaves = foto_chave where foto_chave is not null;
