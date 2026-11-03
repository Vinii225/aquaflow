# Como contribuir

## Fluxo

1. Pegue um card no [Trello](https://trello.com/b/EetgQnZH/aquaflow-projeto-gps) e mova para "Em andamento".
2. Crie uma branch a partir da `main`: `feat/s3-03-catalogo`, `fix/acentos-rodape`, `docs/readme`.
3. Rode `./mvnw verify` antes de abrir o PR (precisa de Docker rodando).
4. Abra o PR para a `main` citando o card. O CI precisa passar e alguém do time revisa.
5. Depois do merge, o Render publica sozinho em homologação. Confira o link e mova o card para "Concluído".

**Não faça push direto na `main`**: ela é o que está no ar.

## Commits

Mensagens em português no padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br/):

```
feat: adiciona pagina da loja (PB-03)
fix: corrige acentos no nome da escola
docs: atualiza README
test: cobre a pagina de planos
ci: adiciona smoke test pos-deploy
refactor: move montagem da home para um service
```

## Código

Leia [docs/ARQUITETURA.md](docs/ARQUITETURA.md) antes de criar uma tela, uma migration ou um service.
