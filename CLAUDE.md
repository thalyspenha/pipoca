# Pipoca

App Android pessoal (Kotlin + Compose) para filmes, séries e coleção física, com dados do TMDB. Pacote `com.thalyspenha.pipoca`.

## Antes de qualquer mudança

Leia, nesta ordem: `docs/PROJECT.md`, `docs/ARCHITECTURE.md`, `docs/DATABASE.md`, `docs/ROADMAP.md`, `docs/DECISIONS.md`. Depois examine o código existente. `ROADMAP.md` diz em que fase o projeto está.

`prompt_mestre.md` é a especificação original. `faseN.md` são as instruções de cada fase.

## Regras

- Desenvolvimento por fases. Só implemente uma fase quando o usuário disser "IMPLEMENTAR FASE X", e apenas ela.
- Fase 1 é feita em 4 partes (ROADMAP.md, D-014): só a parte pedida ("PARTE N"), depois parar e sugerir commit. Nunca fazer a fase inteira de uma vez.
- Não leia `faseN.md` sem permissão do usuário (lidos até agora: `fase1.md`).
- Nunca commitar sem pedido. Sugerir mensagem de commit no fim da fase.
- Decisão arquitetural relevante vai para `docs/DECISIONS.md`; sempre atualizar `docs/ROADMAP.md`.
- Nunca commitar a chave TMDB (`local.properties`).
- Ao fim de cada fase: `./gradlew build`, testes relevantes, corrigir erros.

## Git

Remote `git@github.com:thalyspenha/pipoca.git`. O repo já está configurado com a identidade pessoal e com `core.sshCommand` usando `~/.ssh/id_rsa_personal`. Não usar a identidade global (trabalho).
