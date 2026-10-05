create table tenants (
    id uuid primary key,
    name varchar(255) not null,
    document varchar(32) not null unique,
    status varchar(20) not null default 'ACTIVE'
        check (status in ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

create table users (
    id uuid primary key,
    name varchar(255) not null,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    phone varchar(32),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);

create table tenant_users (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    user_id uuid not null references users(id),
    role varchar(20) not null
        check (role in ('SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'GUARDIAN')),
    status varchar(20) not null default 'ACTIVE'
        check (status in ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (tenant_id, user_id, role)
);
create index idx_tenant_users_user on tenant_users(user_id);

create table guardian_students (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    guardian_id uuid not null references users(id),
    student_id uuid not null references users(id),
    relationship varchar(64),
    created_at timestamptz not null default now(),
    unique (tenant_id, guardian_id, student_id),
    check (guardian_id <> student_id)
);
create index idx_guardian_students_student on guardian_students(student_id);

create table pools (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    name varchar(255) not null,
    total_lanes integer not null check (total_lanes > 0),
    status varchar(20) not null default 'ACTIVE'
        check (status in ('ACTIVE', 'MAINTENANCE', 'INACTIVE')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);
create index idx_pools_tenant on pools(tenant_id);

create table classes (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    pool_id uuid not null references pools(id),
    name varchar(255) not null,
    level varchar(64),
    max_capacity integer not null check (max_capacity > 0),
    status varchar(20) not null default 'ACTIVE'
        check (status in ('ACTIVE', 'INACTIVE')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);
create index idx_classes_tenant on classes(tenant_id);
create index idx_classes_pool on classes(pool_id);

create table class_schedules (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    class_id uuid not null references classes(id),
    teacher_id uuid not null references users(id),
    day_of_week smallint not null check (day_of_week between 1 and 7),
    start_time time not null,
    end_time time not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (end_time > start_time)
);
create index idx_class_schedules_tenant on class_schedules(tenant_id);
create index idx_class_schedules_class on class_schedules(class_id);
create index idx_class_schedules_teacher on class_schedules(teacher_id);

create table class_sessions (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    class_id uuid not null references classes(id),
    class_schedule_id uuid references class_schedules(id),
    teacher_id uuid not null references users(id),
    date date not null,
    start_time time not null,
    end_time time not null,
    limit_students integer not null check (limit_students > 0),
    status varchar(20) not null default 'SCHEDULED'
        check (status in ('SCHEDULED', 'COMPLETED', 'CANCELED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (class_schedule_id, date),
    check (end_time > start_time)
);
create index idx_class_sessions_tenant_date on class_sessions(tenant_id, date);
create index idx_class_sessions_class on class_sessions(class_id);
create index idx_class_sessions_teacher on class_sessions(teacher_id);

create table student_bookings (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    student_id uuid not null references users(id),
    class_session_id uuid not null references class_sessions(id),
    type varchar(20) not null default 'REGULAR'
        check (type in ('REGULAR', 'MAKEUP', 'DROP_IN')),
    status varchar(20) not null default 'CONFIRMED'
        check (status in ('CONFIRMED', 'CANCELED')),
    booked_at timestamptz not null default now(),
    canceled_at timestamptz,
    unique (student_id, class_session_id)
);
create index idx_student_bookings_session_status on student_bookings(class_session_id, status);
create index idx_student_bookings_tenant on student_bookings(tenant_id);

create table attendance (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    student_booking_id uuid not null unique references student_bookings(id),
    status varchar(20) not null
        check (status in ('PRESENT', 'ABSENT', 'EXCUSED')),
    recorded_by uuid not null references users(id),
    recorded_at timestamptz not null default now()
);
create index idx_attendance_tenant on attendance(tenant_id);

create table subscriptions (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    student_id uuid not null references users(id),
    plan_name varchar(255) not null,
    monthly_fee numeric(12, 2) not null check (monthly_fee >= 0),
    due_day smallint not null check (due_day between 1 and 31),
    start_date date not null,
    end_date date,
    status varchar(20) not null default 'ACTIVE'
        check (status in ('ACTIVE', 'PAUSED', 'CANCELED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index idx_subscriptions_tenant on subscriptions(tenant_id);
create index idx_subscriptions_student on subscriptions(student_id);

create table invoices (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    subscription_id uuid not null references subscriptions(id),
    reference_month date not null,
    amount numeric(12, 2) not null,
    due_date date not null,
    status varchar(20) not null default 'OPEN'
        check (status in ('OPEN', 'PAID', 'OVERDUE', 'CANCELED')),
    paid_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (subscription_id, reference_month)
);
create index idx_invoices_tenant_status_due on invoices(tenant_id, status, due_date);

create table transactions (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    type varchar(20) not null check (type in ('INCOME', 'EXPENSE')),
    category varchar(64) not null,
    reference_type varchar(32),
    reference_id uuid,
    amount numeric(12, 2) not null check (amount > 0),
    payment_method varchar(32),
    paid_at timestamptz not null,
    created_by uuid references users(id),
    created_at timestamptz not null default now()
);
create index idx_transactions_tenant_paid_at on transactions(tenant_id, paid_at);
create index idx_transactions_reference on transactions(reference_type, reference_id);
