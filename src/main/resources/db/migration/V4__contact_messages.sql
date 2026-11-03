alter table users add column platform_admin boolean not null default false;

create table contact_messages (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    plan_id uuid references plans(id),
    sender_id uuid references users(id),
    name varchar(255) not null,
    email varchar(255) not null,
    phone varchar(32),
    message text not null,
    status varchar(20) not null default 'NEW'
        check (status in ('NEW', 'FORWARDED', 'ARCHIVED')),
    forwarded_to varchar(64),
    forward_note varchar(1000),
    forwarded_by uuid references users(id),
    forwarded_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_contact_messages_status_created on contact_messages(status, created_at desc);
create index idx_contact_messages_tenant on contact_messages(tenant_id);

insert into users (id, name, email, password_hash, phone, platform_admin) values
    ('22222222-2222-2222-2222-222222222299', 'Equipe AquaFlow', 'plataforma@aquaflow.com', crypt('plataforma123', gen_salt('bf')), '(83) 99999-0099', true);
