-- V9: cargos Trainee e Projetista (pedido do PO em 12/10).
-- Trainee ainda não escolheu área na EJ, então não responde "área de atuação":
-- a coluna fica vazia para trainee e obrigatória para os demais cargos.

-- Troca o check da V3 (nome dado pelo PostgreSQL) por um com os cargos novos.
alter table congressista drop constraint congressista_cargo_check;
alter table congressista add constraint ck_cargo
    check (cargo in ('TRAINEE', 'ASSESSOR_CONSULTOR', 'PROJETISTA', 'GERENTE_COORDENADOR',
                     'DIRETOR', 'PRESIDENTE', 'POS_JUNIOR'));

-- A V2 exigia tempo de MEJ e área juntos (os dois ou nenhum). Agora são
-- regras separadas: tempo de MEJ sempre; área só quando o cargo não é trainee.
alter table congressista drop constraint ck_questionario_completo;
alter table congressista add constraint ck_tempo_mej_preenchido
    check (tempo_mej is not null);
alter table congressista add constraint ck_area_por_cargo
    check ((cargo = 'TRAINEE') = (area_atuacao is null));
