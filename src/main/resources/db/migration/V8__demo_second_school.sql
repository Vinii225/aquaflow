-- Multi-escolas, fase 1 (PB-09): segunda escola de demonstração, com admin, professor
-- e aluno próprios. Serve para a Review e para os testes de isolamento por tenant_id.
-- Logins no README. Ids fixos para o seed ser previsível em qualquer banco.

insert into tenants (id, name, document, status, slug, address, phone, default_school) values
    ('11111111-1111-1111-1111-111111111112', 'Golfinhos Natação', '11.111.111/0001-11', 'ACTIVE',
     'golfinhos', 'Rua das Ondas, 456 - Bairro Novo', '(83) 98888-0000', false);

insert into users (id, name, email, password_hash, phone) values
    ('22222222-2222-2222-2222-2222222222b1', 'Administrador Golfinhos', 'admin@golfinhos.com', crypt('admin123', gen_salt('bf')), '(83) 98888-0001'),
    ('22222222-2222-2222-2222-2222222222b2', 'Marina Costa', 'professor@golfinhos.com', crypt('professor123', gen_salt('bf')), '(83) 98888-0002'),
    ('22222222-2222-2222-2222-2222222222b3', 'Aluno Golfinhos', 'aluno@golfinhos.com', crypt('aluno123', gen_salt('bf')), '(83) 98888-0003');

insert into tenant_users (id, tenant_id, user_id, role, status) values
    ('33333333-3333-3333-3333-3333333333b1', '11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-2222222222b1', 'SCHOOL_ADMIN', 'ACTIVE'),
    ('33333333-3333-3333-3333-3333333333b2', '11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-2222222222b2', 'TEACHER', 'ACTIVE'),
    ('33333333-3333-3333-3333-3333333333b3', '11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-2222222222b3', 'STUDENT', 'ACTIVE');

insert into pools (id, tenant_id, name, total_lanes, status) values
    ('44444444-4444-4444-4444-4444444444b1', '11111111-1111-1111-1111-111111111112', 'Piscina Coberta Golfinhos', 5, 'ACTIVE');

insert into classes (id, tenant_id, pool_id, name, level, max_capacity, status) values
    ('55555555-5555-5555-5555-5555555555b1', '11111111-1111-1111-1111-111111111112', '44444444-4444-4444-4444-4444444444b1',
     'Natação Adulto - Intermediário', 'intermediário', 12, 'ACTIVE');

insert into class_schedules (id, tenant_id, class_id, teacher_id, day_of_week, start_time, end_time) values
    ('66666666-6666-6666-6666-6666666666b1', '11111111-1111-1111-1111-111111111112', '55555555-5555-5555-5555-5555555555b1',
     '22222222-2222-2222-2222-2222222222b2', 1, '18:00', '19:00'),
    ('66666666-6666-6666-6666-6666666666b2', '11111111-1111-1111-1111-111111111112', '55555555-5555-5555-5555-5555555555b1',
     '22222222-2222-2222-2222-2222222222b2', 3, '18:00', '19:00');

insert into plans (id, tenant_id, name, description, monthly_fee, classes_per_week, highlighted, display_order, product_discount_percent) values
    ('77777777-7777-7777-7777-7777777777b1', '11111111-1111-1111-1111-111111111112', 'Golfinho Básico',
     'Duas aulas por semana para manter a rotina na água.', 120.00, 2, false, 1, null),
    ('77777777-7777-7777-7777-7777777777b2', '11111111-1111-1111-1111-111111111112', 'Golfinho Livre',
     'Treine quando quiser, com acesso a todas as turmas.', 220.00, 5, true, 2, 10);

insert into plan_features (plan_id, feature_order, description) values
    ('77777777-7777-7777-7777-7777777777b1', 0, '2 aulas por semana (60 min)'),
    ('77777777-7777-7777-7777-7777777777b1', 1, '1 reposição de aula por mês'),
    ('77777777-7777-7777-7777-7777777777b2', 0, 'Até 5 aulas por semana (60 min)'),
    ('77777777-7777-7777-7777-7777777777b2', 1, 'Reposições ilimitadas, conforme vaga'),
    ('77777777-7777-7777-7777-7777777777b2', 2, '10% de desconto em produtos da loja');

insert into products (id, tenant_id, name, description, category, price, image_path, image_credit, display_order) values
    ('88888888-8888-8888-8888-8888888888b1', '11111111-1111-1111-1111-111111111112', 'Óculos espelhado Golfinhos',
     'Lente espelhada para treino em piscina aberta.', 'ACCESSORY', 89.90,
     '/img/produtos/oculos.jpg', 'Jpogi, domínio público', 1),
    ('88888888-8888-8888-8888-8888888888b2', '11111111-1111-1111-1111-111111111112', 'Touca de lycra Golfinhos',
     'Mais confortável que a de silicone. Tamanho único.', 'ACCESSORY', 24.90,
     '/img/produtos/touca.jpg', 'Floriano, CC BY 3.0', 2),
    ('88888888-8888-8888-8888-8888888888b3', '11111111-1111-1111-1111-111111111112', 'Palmar de treino',
     'Aumenta a resistência na braçada. Par, tamanho M.', 'TRAINING', 54.90,
     null, null, 3);

insert into subscriptions (id, tenant_id, student_id, plan_name, monthly_fee, due_day, start_date, status) values
    ('99999999-9999-9999-9999-9999999999b1', '11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-2222222222b3',
     'Golfinho Básico', 120.00, 10, current_date, 'ACTIVE');

insert into invoices (id, tenant_id, subscription_id, reference_month, amount, due_date, status) values
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaab1', '11111111-1111-1111-1111-111111111112', '99999999-9999-9999-9999-9999999999b1',
     date_trunc('month', current_date)::date, 120.00, date_trunc('month', current_date)::date + 9, 'OPEN');
