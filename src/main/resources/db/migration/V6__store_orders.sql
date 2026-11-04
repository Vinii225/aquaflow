-- Pedidos da loja (PB-04). Nome e preço do produto são copiados para o item:
-- o pedido continua igual mesmo se o produto mudar de preço depois.
create table store_orders (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    student_id uuid not null references users(id),
    status varchar(20) not null check (status in ('PAID', 'DELIVERED', 'CANCELED')),
    subtotal numeric(12, 2) not null check (subtotal >= 0),
    discount_percent smallint not null default 0 check (discount_percent between 0 and 100),
    discount_amount numeric(12, 2) not null default 0 check (discount_amount >= 0),
    total numeric(12, 2) not null check (total >= 0),
    payment_method varchar(32) not null,
    paid_at timestamptz,
    created_at timestamptz not null default now()
);
create index idx_store_orders_tenant_created on store_orders(tenant_id, created_at desc);
create index idx_store_orders_student on store_orders(student_id);

create table store_order_items (
    id uuid primary key,
    order_id uuid not null references store_orders(id) on delete cascade,
    product_id uuid references products(id),
    product_name varchar(255) not null,
    unit_price numeric(12, 2) not null check (unit_price >= 0),
    quantity smallint not null check (quantity between 1 and 99),
    line_total numeric(12, 2) not null check (line_total >= 0),
    item_order integer not null default 0
);
create index idx_store_order_items_order on store_order_items(order_id);
