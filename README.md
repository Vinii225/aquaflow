# AquaFlow — MVP

Sistema de gestão para escola de natação (projeto acadêmico). Esta é a primeira versão funcional: gestão de piscinas, turmas, grade semanal, geração de aulas, reservas de alunos, chamada de presença e financeiro (assinaturas, faturas e pagamento manual).

## Stack

- Java 21, Spring Boot 3.3, Spring MVC + Thymeleaf
- Spring Data JPA, Spring Security 6 (login por formulário, papéis por escola)
- PostgreSQL + Flyway
- Bootstrap 5
- Maven

## Como rodar localmente

Pré-requisitos: Java 21 e Docker.

```bash
docker compose up -d

./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. O Flyway cria o schema e popula dados de demonstração automaticamente na primeira subida.

> Use Java 21 para compilar — o Lombok ainda não suporta versões de JDK mais novas que a 21 nesta máquina; se o `java -version` padrão do seu ambiente for diferente, aponte o `JAVA_HOME` para uma instalação 21 antes de rodar o Maven.

## Login de demonstração

| Papel | E-mail | Senha |
| --- | --- | --- |
| Administrador | admin@aquaflow.com | admin123 |
| Professor | ricardo@aquaflow.com | professor123 |
| Professor | ana@aquaflow.com | professor123 |
| Aluno | aluno@aquaflow.com | aluno123 |

Qualquer pessoa também pode criar uma conta de aluno em `/register`.

## Fluxo sugerido para testar

1. Entrar como admin e conferir piscinas/turmas/grade já cadastradas (dados fictícios).
2. Em **Aulas**, clicar em "Gerar próximas semanas" para criar as aulas concretas a partir da grade.
3. Entrar como aluno e reservar uma vaga em **Aulas disponíveis**.
4. Entrar como professor e fazer a chamada da aula em **Minhas aulas**.
5. Como admin, em **Faturas**, gerar as faturas do mês e registrar um pagamento.

## O que está implementado

- Login/cadastro com papéis (administrador, professor, aluno) — responsável (guardian) existe no modelo de dados mas ainda sem tela própria.
- CRUD de piscinas, turmas e grade semanal.
- Geração de aulas a partir da grade (idempotente).
- Reserva de vaga com controle de capacidade e cancelamento.
- Chamada de presença por aula.
- Assinaturas, geração de faturas do mês e registro manual de pagamento (gera lançamento no livro-caixa).
- Página institucional pública com horários, professores e piscinas.

## O que ficou fora do MVP

- Tela de responsáveis (guardian) vendo os filhos vinculados.
- Reposição de aula (`makeup`) automatizada.
- Loja de produtos e pagamento online (Nuvem Pago/Pix) — está fora do escopo desta fase, conforme o pitch original do time.
- Multi-tenant real: o MVP assume uma única escola (uma linha em `tenants`), criada pela migration de seed.

## Estrutura

```
src/main/java/br/edu/aquaflow/
  domain/          entidades JPA (13 tabelas do modelo de BD)
  repository/      Spring Data JPA
  security/        UserDetails e autenticação
  service/         regras de negócio (agenda, reserva, chamada, financeiro)
  web/             controllers MVC (publico, admin, professor, aluno)
src/main/resources/
  db/migration/    Flyway (schema + dados de demonstração)
  templates/       Thymeleaf
  static/css/      paleta e estilos
```

A modelagem de banco usada como base está em `Docs/mvp` (fornecida pela equipe de BD).
