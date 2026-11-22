-- Multi-escolas, fase 1 (PB-09): nome, endereço, telefone e slug da escola saem do
-- application.properties e passam a ficar no banco. A escola padrão é a usada para
-- visitantes anônimos (site público) e para quem não tem vínculo com nenhuma escola.
alter table tenants add column slug varchar(64);
alter table tenants add column address varchar(255);
alter table tenants add column phone varchar(32);
alter table tenants add column default_school boolean not null default false;

-- Escola de demonstração (V2): mesmos valores que estavam em aquaflow.school.*
update tenants
   set slug = 'aquaflow',
       address = 'Av. das Piscinas, 123 - Centro',
       phone = '(83) 99999-0000',
       default_school = true
 where id = '11111111-1111-1111-1111-111111111111';

-- Banco sem a escola de demonstração: cada escola ganha um slug derivado do id
-- e a mais antiga vira a padrão.
update tenants set slug = 'escola-' || left(id::text, 8) where slug is null;
update tenants set default_school = true
 where id = (select id from tenants order by created_at, id limit 1)
   and not exists (select 1 from tenants where default_school);

alter table tenants alter column slug set not null;
create unique index ux_tenants_slug on tenants(slug);
-- No máximo uma escola padrão
create unique index ux_tenants_default_school on tenants(default_school) where default_school;
