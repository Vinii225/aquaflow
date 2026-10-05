# AquaFlow

Sistema de gestão para escola de natação: piscinas, turmas, grade de horários, geração de aulas, reservas de alunos, chamada de presença e financeiro (assinaturas, faturas, pagamento manual).

## Stack

Java 21 · Spring Boot 3 · Thymeleaf · Spring Security · PostgreSQL · Flyway · Docker

## Como rodar

Pré-requisito: Docker.

```bash
docker compose up -d --build
```

A aplicação sobe em `http://localhost:8080`. O schema do banco e os dados de demonstração são criados automaticamente na primeira subida (via Flyway).

Para derrubar tudo:

```bash
docker compose down
```

## Login de demonstração

| Papel | E-mail | Senha |
| --- | --- | --- |
| Administrador | admin@aquaflow.com | admin123 |
| Professor | ricardo@aquaflow.com | professor123 |
| Aluno | aluno@aquaflow.com | aluno123 |

Também é possível criar uma conta de aluno em `/register`.

## Fluxo rápido para testar

1. Entrar como admin → **Aulas** → "Gerar próximas semanas" (cria as aulas a partir da grade).
2. Entrar como aluno → **Aulas disponíveis** → reservar uma vaga.
3. Entrar como professor → **Minhas aulas** → "Fazer chamada".
4. Como admin → **Faturas** → gerar faturas do mês e registrar um pagamento.

## Estrutura

```
src/main/java/br/edu/aquaflow/
  domain/        entidades JPA
  repository/     Spring Data JPA
  security/       autenticação e papéis
  service/        regras de negócio (agenda, reserva, chamada, financeiro)
  web/            controllers (público, admin, professor, aluno)
src/main/resources/
  db/migration/   Flyway (schema + dados de demonstração)
  templates/      Thymeleaf
  static/         CSS e imagens
```

A modelagem de banco usada como base está em `Docs/mvp`.

## Fora do escopo desta fase

Loja de produtos, pagamento online, tela de responsáveis (guardian) e multi-tenant real (o MVP assume uma única escola).
