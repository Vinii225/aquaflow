# v0.3.0 · Multi-escolas, busca na loja e pagamento em sandbox

Produto V3, entregue na Sprint #4. A plataforma passa a atender várias escolas com os dados separados. A loja ganha busca e filtros. A contratação de plano e a compra na loja podem ser pagas pelo Mercado Pago em modo de teste.

## Novidades

- **Multi-escolas, fase 1 (PB-09):**
  - nome, endereço, telefone e slug de cada escola vêm do banco;
  - cada usuário logado vê só os dados da escola em que tem vínculo, e o visitante vê a escola padrão;
  - dados de demonstração com duas escolas (AquaFlow e Golfinhos Natação), cada uma com admin, professor e aluno.
- **Busca e filtro na loja (PB-03):**
  - busca por nome sem diferenciar maiúsculas nem acentos ("oculos" acha "Óculos");
  - filtros por categoria e por faixa de preço, combináveis;
  - os filtros ficam na URL, então o link pode ser compartilhado (ex.: `/loja?q=oculos&categoria=acessorios&max=60`);
  - quando nada é encontrado, a loja avisa e oferece limpar os filtros.
- **Pagamento em sandbox (PB-02 e PB-04):**
  - contratação de plano e checkout da loja pelo Mercado Pago Checkout Pro (Pix, cartão e boleto, em modo de teste);
  - aprovado: fatura ou pedido pago, com entrada no caixa;
  - pendente: fatura em aberto ou pedido aguardando pagamento;
  - recusado: contratação cancelada (o aluno pode tentar de novo) ou pedido recusado, com os itens de volta ao carrinho.

## Melhorias

- **Isolamento por escola:** todas as consultas filtram pela escola atual, e um id de outra escola responde 404.
- Os formulários do admin recusam piscina, turma, professor ou aluno de outra escola.
- O status do pagamento é atualizado pelo retorno do aluno e pelo webhook, sempre consultando a API do Mercado Pago. O webhook confere a assinatura e é idempotente.
- As telas de confirmação do plano e do pedido mostram pagamento em análise e pagamento recusado.

## Técnico

- Novas migrations:
  - V7: dados da escola em `tenants`;
  - V8: segunda escola de demonstração;
  - V9: `gateway_payments` e novos status de pedido.
- Abstração `GatewayPagamento` com `MercadoPagoGateway` (SDK oficial `com.mercadopago:sdk-java` 3.7.0) e `PagamentoSimuladoGateway`.
- Novas variáveis de ambiente: `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET` e `APP_BASE_URL`.
- Novos testes contra Postgres real:
  - `MultiTenantIsolationTest`: isolamento entre as duas escolas;
  - `StoreSearchTest`: busca, filtros e combinação;
  - `PaymentFlowTest`: os três status nos dois fluxos, retorno, webhook e idempotência.
- Testes de unidade: `ProductFilterTest`, `TenantContextTest`, `MercadoPagoGatewayTest` e `WebhookVerifierTest`.

> Sem `MERCADOPAGO_ACCESS_TOKEN`, o pagamento continua **simulado** e é aprovado na hora, como na v0.2.0. É assim no CI.

## Fica para a Sprint #5

- PB-09 Multi-escolas, fase 2: tela de cadastro de escolas pela Equipe AquaFlow e escolha da escola no login, para quem tem vínculo com mais de uma.
- Site público por escola em `/e/{slug}` (home, planos, loja e contato). Era opcional na Sprint #4 e volta ao Product Backlog.
- PB-05 Área do aluno.

**Homologação:** https://aquaflow-9h8a.onrender.com
**Mudanças completas:** https://github.com/Vinii225/aquaflow/compare/v0.2.0...v0.3.0
