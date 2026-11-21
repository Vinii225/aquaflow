# Roteiro de teste: Produto V3 (Review da Sprint #4, 25/11)

Ambiente: https://aquaflow-9h8a.onrender.com. O primeiro acesso pode levar até 1 minuto.
Tempo estimado: 10 minutos. Os logins estão no [README](../README.md#login-de-demonstração).

**Antes da Review:**

- Confirme no Render que `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET` e `APP_BASE_URL` estão definidas. Sem elas, os passos 3 e 4 usam o pagamento simulado, que aprova na hora.
- Tenha em mãos o comprador de teste do Mercado Pago e um cartão de teste (nome do titular `APRO` aprova, `CONT` deixa pendente, `OTHE` recusa).

| # | Passo | Resultado esperado |
| --- | --- | --- |
| 1 | Entrar como **aluno da escola A** (`aluno@aquaflow.com` / `aluno123`) | Rodapé e loja mostram "AquaFlow Escola de Natação" |
| 2 | Em **Loja**, buscar `oculos`; depois filtrar categoria **Equipamentos de treino** com preço máx. `80`; copiar o link e abrir em outra aba | A busca acha "Óculos de natação". O filtro mostra Prancha e Pull buoy. O link abre com os mesmos filtros preenchidos |
| 3 | Buscar `xyz` | Mensagem "Nenhum produto encontrado" e botão para limpar os filtros |
| 4 | Adicionar os óculos ao carrinho, finalizar a compra e pagar no Mercado Pago com titular `APRO` | Volta ao site com "Pedido confirmado" e situação "Pago". O pedido aparece em **Meus pedidos** |
| 5 | Em **Planos**, contratar um plano e pagar no Mercado Pago com titular `APRO` (o aluno de demonstração já tem plano: use uma conta nova criada em `/register`) | "Bem-vindo ao plano". A primeira mensalidade aparece como "Paga" em **Minhas faturas** |
| 6 | Opcional: repetir o passo 4 com titular `CONT` e depois com `OTHE` | `CONT`: "Pagamento em análise". `OTHE`: "Pagamento recusado", com os itens de volta ao carrinho |
| 7 | Entrar como **admin da escola A** (`admin@aquaflow.com` / `admin123`) e abrir **Painel**, **Faturas** e **Caixa** | O pedido e a mensalidade dos passos 4 e 5 aparecem, com a entrada no caixa |
| 8 | Sair e entrar como **admin da escola B** (`admin@golfinhos.com` / `admin123`) e abrir **Painel**, **Piscinas**, **Faturas** e **Caixa** | Só "Golfinhos Natação": "Piscina Coberta Golfinhos" e a fatura do "Aluno Golfinhos". **Nenhum** dado da escola A (piscinas, faturas, pedidos ou caixa dos passos 4 e 5) |
| 9 | Entrar como **aluno da escola B** (`aluno@golfinhos.com` / `aluno123`) e abrir a **Loja** | Só os produtos da Golfinhos (ex.: "Palmar de treino") |
| 10 | Sair e abrir a home como visitante | Escola padrão (AquaFlow), com endereço e telefone vindos do banco |

Prints para anexar no card S4-07: passos 2, 4, 5 e 8. [preencher após a execução]
