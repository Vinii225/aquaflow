# Arquitetura e convenções

Guia rápido para quem vai mexer no código. Se algo aqui ficar desatualizado, corrija no mesmo PR.

## Camadas

```
web/         Controllers MVC: recebem a requisição, chamam um service e escolhem o template
service/     Regras de negócio e montagem das telas (transações ficam aqui)
  site/      Telas públicas: home (HomePageService) e loja (ProductCatalogService)
repository/  Spring Data JPA: consultas ao banco
domain/      Entidades JPA e enums (com os textos exibidos, ex.: Weekday, ProductCategory)
config/      Spring Security e configurações tipadas (AquaflowProperties)
security/    Login e papéis
```

Regra de dependência: `web → service → repository → domain`. Um controller não deve montar listas, agrupar dados ou formatar textos: isso vai para um service, que devolve um `record` com o formato da tela (ex.: `HomePageService.HomePage`).

### Controller fino

```java
@GetMapping("/loja")
public String index(Model model) {
    model.addAttribute("catalog", productCatalogService.load());
    return "products";
}
```

### Service de tela

- `@Transactional(readOnly = true)` em métodos que só leem.
- Devolve `record`s imutáveis com o que o template precisa. O template não deve ter regra de negócio.
- Lógica pura (formatar horário, juntar nomes) fica em métodos `static` com visibilidade de pacote, para testar sem banco (veja `HomePageServiceTest`).

## Onde ficam as coisas

| Preciso de... | Use |
| --- | --- |
| Nome dos dias da semana | `Weekday` (1 = segunda, igual ao banco) |
| Usuários ativos de um papel | `tenantUserRepository.findActiveUsersByRole(tenantId, role)` |
| Escola atual | `TenantContext.getTenantId()` |
| Nome, endereço e telefone da escola | `AquaflowProperties.school()` (nos templates: `${schoolName}`, `${schoolAddress}`, `${schoolPhone}`, `${schoolPhoneHref}`) |
| `<head>` de uma página | `<head th:replace="~{fragments/head :: head('Título', 'Descrição ou null')}"></head>` |
| Menu, rodapé e alertas | `fragments/navbar`, `fragments/footer`, `fragments/alerts` |
| Item ativo no menu | `${currentPath}` (preenchido em `GlobalModelAttributes`) |

## Banco de dados

- Toda mudança de schema é uma migration nova em `src/main/resources/db/migration`, no padrão `V<n>__descricao.sql`. **Nunca edite uma migration que já foi para a `main`**: o Flyway confere o checksum e a aplicação não sobe.
- `ddl-auto=validate`: se a entidade não bater com o banco, a aplicação não inicia. Isso é intencional.
- Texto com acento no `application.properties` precisa de escape unicode (`ç`), porque o arquivo é lido em ISO-8859-1.

## Front-end

- Bootstrap 5 + `static/css/aquaflow.css`. Use as variáveis de `:root` (`--color-primary`, `--color-accent`, ...) em vez de cores soltas.
- Fontes: Fredoka nos títulos das páginas públicas (`.home-page`), Nunito no texto.
- Hover sempre dentro de `@media (hover: hover)`; animação de movimento dentro de `@media (prefers-reduced-motion: no-preference)`.
- Imagens novas: `static/img/`, com `width`, `height` e `alt`. Se vierem de terceiros, registre o crédito em [CREDITOS.md](CREDITOS.md).

## Testes

- `PublicPagesSmokeTest` sobe a aplicação com Postgres real (Testcontainers) e roda as migrations. Toda página pública nova ganha um teste ali.
- Testes unitários ficam no mesmo pacote da classe testada.
