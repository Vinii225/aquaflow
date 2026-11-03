create table plans (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    name varchar(255) not null,
    description varchar(500),
    monthly_fee numeric(12, 2) not null check (monthly_fee >= 0),
    classes_per_week smallint not null check (classes_per_week between 1 and 7),
    highlighted boolean not null default false,
    display_order smallint not null default 0,
    active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_plans_tenant on plans(tenant_id);

create table plan_features (
    plan_id uuid not null references plans(id) on delete cascade,
    feature_order integer not null,
    description varchar(255) not null,
    primary key (plan_id, feature_order)
);

insert into plans (id, tenant_id, name, description, monthly_fee, classes_per_week, highlighted, display_order) values
    ('77777777-7777-7777-7777-777777777701', '11111111-1111-1111-1111-111111111111', 'Essencial',
     'Para quem quer começar no seu ritmo, com uma aula por semana.', 99.90, 1, false, 1),
    ('77777777-7777-7777-7777-777777777702', '11111111-1111-1111-1111-111111111111', 'Regular',
     'Equilíbrio ideal entre frequência e evolução técnica.', 150.00, 2, true, 2),
    ('77777777-7777-7777-7777-777777777703', '11111111-1111-1111-1111-111111111111', 'Intensivo',
     'Para quem quer evoluir rápido ou treinar com mais frequência.', 199.90, 3, false, 3);

insert into plan_features (plan_id, feature_order, description) values
    ('77777777-7777-7777-7777-777777777701', 0, '1 aula por semana (60 min)'),
    ('77777777-7777-7777-7777-777777777701', 1, 'Turma separada por idade e nível'),
    ('77777777-7777-7777-7777-777777777701', 2, 'Acompanhamento de professor qualificado'),
    ('77777777-7777-7777-7777-777777777701', 3, '1 reposição de aula por mês'),

    ('77777777-7777-7777-7777-777777777702', 0, '2 aulas por semana (60 min)'),
    ('77777777-7777-7777-7777-777777777702', 1, 'Turma separada por idade e nível'),
    ('77777777-7777-7777-7777-777777777702', 2, '2 reposições de aula por mês'),
    ('77777777-7777-7777-7777-777777777702', 3, 'Avaliação de desempenho trimestral'),
    ('77777777-7777-7777-7777-777777777702', 4, '10% de desconto em produtos e kits de natação'),

    ('77777777-7777-7777-7777-777777777703', 0, '3 aulas por semana (60 min)'),
    ('77777777-7777-7777-7777-777777777703', 1, 'Turma separada por idade e nível'),
    ('77777777-7777-7777-7777-777777777703', 2, 'Reposições ilimitadas, conforme vaga'),
    ('77777777-7777-7777-7777-777777777703', 3, 'Avaliação de desempenho mensal'),
    ('77777777-7777-7777-7777-777777777703', 4, 'Kit iniciante (touca + óculos) incluso na matrícula'),
    ('77777777-7777-7777-7777-777777777703', 5, '15% de desconto em produtos e kits de natação');
