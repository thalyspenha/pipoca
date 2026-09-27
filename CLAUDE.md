# Pipoca

App Android pessoal (Kotlin + Compose) para filmes, séries e coleção física, com dados do TMDB. Pacote `com.thalyspenha.pipoca`. Aparelho principal: Galaxy S25, Android 16.

Este arquivo é a memória portátil do projeto: vale em qualquer máquina (Mac, Linux). Tudo que precisa sobreviver entre sessões/máquinas fica aqui ou em `docs/`, nunca só na memória local do Claude.

## Antes de qualquer mudança

Leia, nesta ordem: `docs/PROJECT.md`, `docs/ARCHITECTURE.md`, `docs/DATABASE.md`, `docs/ROADMAP.md`, `docs/DECISIONS.md`. Depois examine o código existente. `ROADMAP.md` diz em que fase o projeto está e qual o próximo passo.

`prompt_mestre.md` é a especificação original. `faseN.md` são as instruções de cada fase.

Máquina nova ou build falhando por ambiente: ver `docs/SETUP.md`.

## Regras

- Desenvolvimento por fases. Só implemente uma fase quando o usuário disser "IMPLEMENTAR FASE X", e apenas ela.
- Ir por partes. Fase 1 em 4 partes (D-014), Fase 2 em 3 partes (D-019), Fase 3 em 3 partes (D-023), Fase 4 em 3 partes (D-027), Fase 5 em 3 partes (D-031), Fase 6 em 3 partes (D-035): uma por vez, parando ao fim de cada. Antes de começar uma fase nova, perguntar se o usuário quer dividi-la em partes; se sim, registrar a divisão no ROADMAP e fazer só a parte pedida ("PARTE N").
- Não leia `faseN.md` sem permissão do usuário (lidos até agora: `fase1.md`, `fase2.md`, `fase3.md`, `fase4.md`, `fase5.md`, `fase6.md`).
- Nunca commitar sem pedido. Ao fim de cada parte/fase, sugerir mensagem de commit e parar.
- Quando o usuário pedir commit: antes, conferir se docs (`ROADMAP.md`, `DECISIONS.md`, docs afetados) e este arquivo estão atualizados; depois commit + push para `main`.
- Decisão arquitetural relevante vai para `docs/DECISIONS.md`; sempre atualizar `docs/ROADMAP.md`.
- Nunca commitar a chave TMDB (`local.properties`); conferir o que está staged antes de commitar.
- Ao fim de cada parte/fase: `./gradlew build` (inclui lint e testes unitários), corrigir erros e avisos. Com aparelho conectado, rodar também os instrumentados conforme `docs/SETUP.md` (nunca `connectedDebugAndroidTest`: desinstala o app e apaga os dados).
- Dependência nova só com registro em `DECISIONS.md`; versões em `gradle/libs.versions.toml`.

## Git

Remote `git@github.com:thalyspenha/pipoca.git`, branch `main`. Usar a identidade pessoal (Thalys Penha, `thalyspenha@outlook.com.br`) e a chave SSH pessoal, nunca a identidade global (trabalho). Essa configuração é local do repo e **não vem no clone**: em máquina nova, configurar conforme `docs/SETUP.md` e conferir com `git config user.email` antes do primeiro commit.
