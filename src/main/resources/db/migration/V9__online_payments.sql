-- Pagamento online (S4-06): Mercado Pago em sandbox ou pagamento simulado.
-- Cada tentativa de pagamento de uma fatura (PB-02) ou de um pedido da loja (PB-04) vira
-- uma linha aqui. O id da linha vai como external_reference para o gateway e liga o
-- pagamento de volta à fatura/pedido e à escola certa.

-- Pedido agora pode esperar o pagamento ou ter o pagamento recusado
alter table store_orders drop constraint if exists store_orders_status_check;
alter table store_orders add constraint store_orders_status_check
    check (status in ('PENDING', 'PAID', 'DELIVERED', 'REJECTED', 'CANCELED'));

create table gateway_payments (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    reference_type varchar(20) not null check (reference_type in ('INVOICE', 'STORE_ORDER')),
    reference_id uuid not null,
    gateway varchar(20) not null check (gateway in ('SIMULATED', 'MERCADO_PAGO')),
    gateway_preference_id varchar(100),
    gateway_payment_id varchar(100),
    status varchar(20) not null default 'PENDING' check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    amount numeric(12, 2) not null check (amount >= 0),
    payment_method varchar(32) not null,
    payer_id uuid not null references users(id),
    status_updated_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_gateway_payments_tenant_reference on gateway_payments(tenant_id, reference_type, reference_id);
create index idx_gateway_payments_gateway_payment on gateway_payments(gateway_payment_id);
