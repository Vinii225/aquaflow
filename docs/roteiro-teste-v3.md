# Roteiro de teste: Produto V3 (Review da Sprint #4, 25/11)

Ambiente: https://aquaflow-9h8a.onrender.com. O primeiro acesso pode levar até 1 minuto.
Tempo estimado: 10 minutos. Os logins estão no [README](../README.md#login-de-demonstração).

**Pagamento:** a homologação roda sem `MERCADOPAGO_ACCESS_TOKEN`, então os passos 4 e 5 usam o pagamento simulado, que aprova na hora, sem sair do site. O Mercado Pago em sandbox está coberto pelos testes automatizados (`PaymentFlowTest`) e só entra no ar quando o token for definido no Render.

| # | Passo | Resultado esperado |
| --- | --- | --- |
| 1 | Entrar como **aluno da escola A** (`aluno@aquaflow.com` / `aluno123`) | Rodapé e loja mostram "AquaFlow Escola de Natação" |
| 2 | Em **Loja**, buscar `oculos`; depois filtrar categoria **Equipamentos de treino** com preço máx. `80`; copiar o link e abrir em outra aba | A busca acha "Óculos de natação". O filtro mostra Prancha e Pull buoy. O link abre com os mesmos filtros preenchidos |
| 3 | Buscar `xyz` | Mensagem "Nenhum produto encontrado" e botão para limpar os filtros |
| 4 | Adicionar os óculos ao carrinho e finalizar a compra | "Pedido confirmado" com situação "Pago". O pedido aparece em **Meus pedidos** |
| 5 | Em **Planos**, contratar um plano (o aluno de demonstração já tem plano: use uma conta nova criada em `/register`) | "Bem-vindo ao plano". A primeira mensalidade aparece como "Paga" em **Minhas faturas** |
| 6 | Só com o token do Mercado Pago definido: repetir o passo 4 pagando com titular `CONT` e depois `OTHE` | `CONT`: "Pagamento em análise". `OTHE`: "Pagamento recusado", com os itens de volta ao carrinho. Sem o token, pular |
| 7 | Entrar como **admin da escola A** (`admin@aquaflow.com` / `admin123`) e abrir **Painel**, **Faturas** e **Caixa** | O pedido e a mensalidade dos passos 4 e 5 aparecem, com a entrada no caixa |
| 8 | Sair e entrar como **admin da escola B** (`admin@golfinhos.com` / `admin123`) e abrir **Painel**, **Piscinas**, **Faturas** e **Caixa** | Só "Golfinhos Natação": "Piscina Coberta Golfinhos" e a fatura do "Aluno Golfinhos". **Nenhum** dado da escola A (piscinas, faturas, pedidos ou caixa dos passos 4 e 5) |
| 9 | Entrar como **aluno da escola B** (`aluno@golfinhos.com` / `aluno123`) e abrir a **Loja** | Só os produtos da Golfinhos (ex.: "Palmar de treino") |
| 10 | Sair e abrir a home como visitante | Escola padrão (AquaFlow), com endereço e telefone vindos do banco |

Prints para anexar no card S4-07: passos 2, 4, 5 e 8. [preencher após a execução]
