# Plano de Gerenciamento das Aquisições: Sprint #4 (S4-03)

**Projeto:** AquaFlow · **Sprint:** #4 (04/11 a 25/11/2026) · **Versão:** rascunho para revisão · **Dono do card:** Emanoel Barreto · **Revisor:** Vinícius Ares (replanejamento de 18/11)

Base: PMBOK (Aquisições) + Scrum. Fontes: Sprint Planning #4 e reunião de controle de 18/11 no Trello, onde o fornecedor foi escolhido.

## 1. O que vem de terceiros

| Item | Fornecedor | Uso | Custo |
| --- | --- | --- | --- |
| Hospedagem e banco | Render (Web Service Docker + PostgreSQL) | Homologação | Gratuito (750 h/mês) |
| Código e CI | GitHub e GitHub Actions | Repositório, PRs, testes, releases | Gratuito |
| Gestão | Trello | Backlog, sprints, atas | Gratuito |
| Fontes e ícones | Google Fonts, Bootstrap Icons | Interface | Licenças livres |
| Fotos da loja | Wikimedia Commons | Catálogo | Creative Commons (ver `docs/CREDITOS.md`) |
| Meio de pagamento | Mercado Pago (modo de teste) | Contratação de plano e loja | Sem custo no sandbox |

## 2. Fazer x comprar

O time faz o produto: planos, loja e multi-escolas. Infraestrutura e pagamento vêm prontos de terceiros, sem custo. O pagamento fica com um gateway porque o AquaFlow não deve receber nem guardar dados de cartão.

## 3. Critérios de seleção do meio de pagamento

Critérios eliminatórios:

- ambiente de teste gratuito;
- Pix, cartão e boleto no Brasil;
- integração com Java/Spring;
- documentação em português.

## 4. Comparação e resultado

| Critério | Mercado Pago | Asaas | Stripe | PagSeguro |
| --- | --- | --- | --- | --- |
| Ambiente de teste | Contas de teste (vendedor e comprador) | Sandbox com chave própria | Modo de teste | Sandbox |
| Pix, cartão e boleto | Sim, compra de teste com os três | Sim | Pix só em pagamento único (conta BR) | Sim |
| Java/Spring | SDK Java oficial | API REST | SDK Java oficial | API REST |
| **Resultado** | **Escolhido** | **Plano B** | Descartado | Reserva |

- **Mercado Pago escolhido** (daily de 18/11), integrado com o SDK Java oficial (`com.mercadopago:sdk-java`) e o Checkout Pro.
- **Asaas é o plano B:** troca se as contas de teste do Mercado Pago não funcionarem até 20/11 (decisão da daily de 18/11).
- **Stripe descartado:** no Brasil, o Pix só existe em pagamento único.
- **PagSeguro fica como reserva.**

## 5. Controle

- **Chaves e segredos** só em variáveis de ambiente (Render) e GitHub Secrets: `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET`. O revisor confere no PR que nenhuma chave entrou no repositório.
- **Sem chave, o pagamento simulado continua funcionando:** CI e homologação não quebram.
- **Limites gratuitos:** o SM confere em cada reunião de controle as 750 h/mês do Render e os minutos do GitHub Actions. Consumo na Sprint #4: [preencher]
- **Licenças e créditos** em `docs/CREDITOS.md`. O Mercado Pago e o SDK Java (Apache 2.0) estão registrados na seção "Serviços de terceiros".
- Troca de fornecedor só com aprovação do PO.

## 6. Validação

- Pagamento de teste aprovado, pendente e recusado atualiza a fatura e o pedido (S4-06).
- Teste automatizado com o pagamento simulado no CI (`PaymentFlowTest`).
- Na Review, o PO faz uma contratação e uma compra de teste em homologação.

## 7. Encerramento e avaliação (Review #4, 25/11)

| Pergunta | Resposta |
| --- | --- |
| O fornecedor atendeu? | [preencher] |
| Houve custo, limite ou indisponibilidade? | [preencher] |
| Manter ou trocar? | [preencher] |

## 8. Aprovação

- Revisado por: [preencher] · Data: [preencher]
- Aprovado pelo PO: [preencher] · Data: [preencher]
