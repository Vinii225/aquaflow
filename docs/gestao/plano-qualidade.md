# Plano de Gerenciamento da Qualidade: Sprint #4 (S4-01)

**Projeto:** AquaFlow · **Sprint:** #4 (04/11 a 25/11/2026) · **Versão:** rascunho para revisão · **Dono do card:** Luis Eduardo · **Revisor:** Ramotep Lopes (replanejamento de 18/11)

Base: PMBOK (Qualidade) + Scrum. Fontes: Sprint Planning #4 (DoD) e resumo da pré-review de 21/10 no Trello.

## 1. Padrões (o que é "pronto")

**Definition of Done da Planning #4:**

- Critérios de aceite do item atendidos e validados pelo PO
- PR revisado por outro integrante (revisor em rodízio, PB-08)
- CI verde: build e testes automáticos (Postgres real) passando
- Nenhuma tela ou consulta nova mostra dados de outra escola (`tenant_id`)
- Funciona no celular e no desktop
- No ar em homologação, sem erro
- Card movido para Concluído no mesmo dia, com link do PR ou do documento

**Padrões técnicos do repositório** (`CONTRIBUTING.md` e `docs/ARQUITETURA.md`):

- Uma branch e um PR pequeno por card. Commits em Conventional Commits.
- Migrations novas a partir da última versão; migration que já foi para a `main` nunca é editada.
- Chaves e segredos só em variáveis de ambiente e GitHub Secrets.

## 2. Garantia da qualidade (prevenção)

- **Revisão por pares:** todo PR é revisado por outro integrante antes do merge. Revisor em rodízio, definido no replanejamento de 18/11.
- **CI obrigatório:** o workflow `ci.yml` roda `./mvnw verify` em todo PR, com os testes subindo a aplicação contra PostgreSQL real (Testcontainers) e todas as migrations. Também faz o build da imagem Docker usada no Render.
- **Smoke test pós-deploy:** `deploy.yml` confere `/`, `/planos` e `/login` em homologação depois de cada deploy da `main`.

## 3. Critério obrigatório: isolamento por escola (`tenant_id`)

Desde a Sprint #4 a plataforma tem mais de uma escola. Um filtro por `tenant_id` esquecido mostra dados de uma escola para outra, e esse é o principal risco do S4-05.

- O teste `MultiTenantIsolationTest` precisa passar no CI: com 2 escolas no banco, a escola A não vê piscinas, faturas, pedidos nem mensagens da escola B, e vice-versa.
- Toda tela nova ganha um caso nesse teste.
- O revisor do PR confere se as consultas novas filtram pela escola atual.

## 4. Controle da qualidade (métricas)

| Métrica | Como medir | Meta | Resultado Sprint #4 |
| --- | --- | --- | --- |
| Testes automáticos passando | Resultado do CI na `main` e nos PRs | 100% | [preencher] |
| Erros em homologação | Falhas do smoke test e erros vistos no Render durante a sprint | 0 | [preencher] |
| Itens rejeitados na Review | Itens que o PO não aceitou na Review #4 | 0 | [preencher] |

Números de referência de sprints anteriores: [preencher]

## 5. Melhoria

Desvios das metas vão para a Retrospectiva, com ação e dono registrados no Trello. Mudança neste plano só com aprovação do PO.

## 6. Aprovação

- Revisado por: [preencher] · Data: [preencher]
- Aprovado pelo PO: [preencher] · Data: [preencher]
