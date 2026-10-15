-- Limpeza depois do teste de carga (carga/teste-de-carga.js).
--
-- Apaga tudo o que o teste criou e DESFAZ A MONTAGEM, para a montagem real
-- (sábado, 12h) funcionar. Só rode se a montagem que existe no banco foi a do teste.
--
-- Como rodar (a URL externa está no Render: banco > Connect > External):
--   psql "URL_EXTERNA_DO_BANCO" -v ON_ERROR_STOP=1 -f carga/limpeza.sql
--
-- Tudo roda numa transação: se algo falhar, nada muda.

begin;

-- Contas e missões do teste.
create temporary table carga_pessoas on commit drop as
    select id from congressista where email like '%@carga.teste';
create temporary table carga_missoes on commit drop as
    select id from missao where titulo like '[CARGA]%';

\echo 'Antes da limpeza:'
select (select count(*) from carga_pessoas) as contas_de_carga,
       (select count(*) from carga_missoes) as missoes_de_carga,
       (select count(*) from tripulacao) as tripulacoes,
       (select count(*) from conclusao) as conclusoes,
       (select count(*) from lancamento) as lancamentos,
       (select count(*) from foto_envio) as fotos_de_envio;

-- 1. Pontos e envios das missões de carga e das contas de carga (as fotos dos
--    envios saem junto, por "on delete cascade").
delete from lancamento
 where missao_id in (select id from carga_missoes)
    or congressista_id in (select id from carga_pessoas)
    or conclusao_id in (select id from conclusao
                         where missao_id in (select id from carga_missoes)
                            or congressista_id in (select id from carga_pessoas)
                            or enviada_por in (select id from carga_pessoas));
delete from conclusao
 where missao_id in (select id from carga_missoes)
    or congressista_id in (select id from carga_pessoas)
    or enviada_por in (select id from carga_pessoas);

-- 2. Desfaz a montagem do teste. Pontos de missões da tripulação que sobrarem
--    (só existem se alguém real jogou durante o teste) saem junto; nos individuais,
--    só se apaga a tripulação de consulta.
update lancamento set tripulacao_id = null where congressista_id is not null;
update conclusao set tripulacao_id = null where congressista_id is not null;
delete from lancamento where tripulacao_id is not null;
delete from conclusao where tripulacao_id is not null;
update tripulacao set capitao_id = null;
update congressista set tripulacao_id = null;
delete from tripulacao;

-- 3. Missões e contas do teste (a foto de perfil sai junto).
delete from missao where id in (select id from carga_missoes);
delete from congressista where id in (select id from carga_pessoas);

-- 4. Vagas das outras missões: recontadas a partir dos envios que valem.
update missao m
   set vagas_usadas = (select count(*) from conclusao c
                        where c.missao_id = m.id and c.status in ('PENDENTE', 'APROVADA'));

\echo 'Depois da limpeza (contas, missões e tripulações devem estar em 0):'
select (select count(*) from congressista where email like '%@carga.teste') as contas_de_carga,
       (select count(*) from missao where titulo like '[CARGA]%') as missoes_de_carga,
       (select count(*) from tripulacao) as tripulacoes,
       (select count(*) from congressista) as congressistas_que_ficam,
       (select count(*) from conclusao) as conclusoes,
       (select count(*) from foto_envio) as fotos_de_envio;

commit;
