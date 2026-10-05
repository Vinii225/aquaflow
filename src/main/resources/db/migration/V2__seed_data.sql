create extension if not exists pgcrypto;

insert into tenants (id, name, document, status) values
    ('11111111-1111-1111-1111-111111111111', 'AquaFlow Escola de Natação', '00.000.000/0001-00', 'ACTIVE');

insert into users (id, name, email, password_hash, phone) values
    ('22222222-2222-2222-2222-222222222201', 'Administrador AquaFlow', 'admin@aquaflow.com', crypt('admin123', gen_salt('bf')), '(83) 99999-0001'),
    ('22222222-2222-2222-2222-222222222202', 'Ricardo Lima', 'ricardo@aquaflow.com', crypt('professor123', gen_salt('bf')), '(83) 99999-0002'),
    ('22222222-2222-2222-2222-222222222203', 'Ana Souza', 'ana@aquaflow.com', crypt('professor123', gen_salt('bf')), '(83) 99999-0003'),
    ('22222222-2222-2222-2222-222222222204', 'Aluno Demonstração', 'aluno@aquaflow.com', crypt('aluno123', gen_salt('bf')), '(83) 99999-0004');

insert into tenant_users (id, tenant_id, user_id, role, status) values
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222201', 'SCHOOL_ADMIN', 'ACTIVE'),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222202', 'TEACHER', 'ACTIVE'),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222203', 'TEACHER', 'ACTIVE'),
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222204', 'STUDENT', 'ACTIVE');

insert into pools (id, tenant_id, name, total_lanes, status) values
    ('44444444-4444-4444-4444-444444444401', '11111111-1111-1111-1111-111111111111', 'Piscina Semiolímpica', 6, 'ACTIVE'),
    ('44444444-4444-4444-4444-444444444402', '11111111-1111-1111-1111-111111111111', 'Piscina Infantil', 4, 'ACTIVE');

insert into classes (id, tenant_id, pool_id, name, level, max_capacity, status) values
    ('55555555-5555-5555-5555-555555555501', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444401', 'Natação Adulto - Iniciante', 'iniciante', 10, 'ACTIVE'),
    ('55555555-5555-5555-5555-555555555502', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444402', 'Natação Infantil - Adaptação', 'adaptação', 8, 'ACTIVE');

insert into class_schedules (id, tenant_id, class_id, teacher_id, day_of_week, start_time, end_time) values
    ('66666666-6666-6666-6666-666666666601', '11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555555501', '22222222-2222-2222-2222-222222222202', 2, '07:00', '08:00'),
    ('66666666-6666-6666-6666-666666666602', '11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555555501', '22222222-2222-2222-2222-222222222202', 4, '07:00', '08:00'),
    ('66666666-6666-6666-6666-666666666603', '11111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555555502', '22222222-2222-2222-2222-222222222203', 3, '09:00', '10:00');

insert into subscriptions (id, tenant_id, student_id, plan_name, monthly_fee, due_day, start_date, status) values
    (gen_random_uuid(), '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222204', 'Mensal 2x/semana', 150.00, 10, current_date, 'ACTIVE');
