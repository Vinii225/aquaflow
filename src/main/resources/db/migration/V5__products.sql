-- Desconto do aluno nos produtos da loja, por plano (antes só existia como texto nos benefícios)
alter table plans add column product_discount_percent smallint
    check (product_discount_percent between 0 and 100);
update plans set product_discount_percent = 10 where id = '77777777-7777-7777-7777-777777777702';
update plans set product_discount_percent = 15 where id = '77777777-7777-7777-7777-777777777703';

create table products (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    name varchar(255) not null,
    description varchar(500),
    category varchar(20) not null
        check (category in ('ACCESSORY', 'TRAINING', 'SWIMWEAR')),
    price numeric(12, 2) not null check (price >= 0),
    image_path varchar(255),
    image_credit varchar(255),
    display_order smallint not null default 0,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_products_tenant on products(tenant_id);

-- Fotos: Wikimedia Commons (créditos em image_credit e em docs/CREDITOS.md)
insert into products (id, tenant_id, name, description, category, price, image_path, image_credit, display_order) values
    ('88888888-8888-8888-8888-888888888801', '11111111-1111-1111-1111-111111111111', 'Óculos de natação',
     'Lentes antiembaçantes e vedação em silicone. Ajuste da tira na parte de trás.', 'ACCESSORY', 49.90,
     '/img/produtos/oculos.jpg', 'Jpogi, domínio público', 1),
    ('88888888-8888-8888-8888-888888888802', '11111111-1111-1111-1111-111111111111', 'Touca de silicone',
     'Protege o cabelo do cloro e reduz o atrito na água. Tamanho único.', 'ACCESSORY', 34.90,
     '/img/produtos/touca.jpg', 'Floriano, CC BY 3.0', 2),
    ('88888888-8888-8888-8888-888888888803', '11111111-1111-1111-1111-111111111111', 'Toalha de microfibra',
     'Seca rápido e ocupa pouco espaço na mochila. 70 × 140 cm.', 'ACCESSORY', 59.90,
     '/img/produtos/toalha.jpg', 'HanSangYoon, CC BY-SA 4.0', 3),
    ('88888888-8888-8888-8888-888888888804', '11111111-1111-1111-1111-111111111111', 'Prancha de EVA',
     'Usada nos exercícios de pernada, da adaptação ao treino.', 'TRAINING', 79.90,
     '/img/produtos/prancha.jpg', 'Sam5827, CC BY-SA 3.0', 4),
    ('88888888-8888-8888-8888-888888888805', '11111111-1111-1111-1111-111111111111', 'Pull buoy',
     'Flutuador entre as pernas para isolar a braçada.', 'TRAINING', 69.90,
     '/img/produtos/pull-buoy.jpg', 'Edo de Roo, CC BY-SA 3.0', 5),
    ('88888888-8888-8888-8888-888888888806', '11111111-1111-1111-1111-111111111111', 'Nadadeiras de treino',
     'Lâmina curta para ganhar propulsão e trabalhar a pernada. Do 34 ao 45.', 'TRAINING', 159.90,
     '/img/produtos/nadadeiras.jpg', 'Alexander Z., CC BY-SA 3.0', 6);
