# PROJECT — Pipoca

## Visão geral

Aplicativo Android pessoal (hobby) para gerenciar filmes e séries: pesquisar via TMDB, montar biblioteca, acompanhar episódios, registrar notas e datas, manter uma coleção física (4K UHD, Blu-ray, DVD...) e ver estatísticas.

Inspirado conceitualmente em TV Time, Letterboxd e gerenciadores de coleção, **sem copiar identidade visual, código ou marca** de nenhum deles.

Não será publicado na Google Play inicialmente. Uso por um único usuário, sem conta/login.

> Nome: **Pipoca** · pacote `com.thalyspenha.pipoca` (D-011).

## Objetivos funcionais

- Pesquisar filmes e séries (TMDB).
- Ver capas e detalhes (sinopse, gêneros, elenco, diretor, duração, nota TMDB).
- Biblioteca pessoal com status próprios:
  - Filmes: `WANT_TO_WATCH`, `WATCHED`
  - Séries: `WANT_TO_WATCH`, `WATCHING`, `COMPLETED`, `PAUSED`, `DROPPED`
- Marcar episódios individualmente (com data) e calcular progresso automaticamente.
- Favoritos, nota pessoal, data em que assistiu, observações.
- Coleção física: tipo de mídia, edição, região, quantidade, observação, data de aquisição.
- Histórico de visualizações.
- Estatísticas calculadas a partir dos dados locais.
- Uso offline para tudo que já foi baixado.

## Dispositivo alvo

- Aparelho principal: **Samsung Galaxy S25, Android 16 (API 36), One UI**. Testes manuais priorizam ele.
- Implicações: edge-to-edge obrigatório, predictive back ativo, conferir layouts com fonte/zoom grandes da One UI.
- Continua suportando `minSdk 26` (D-010).

## Princípios

1. **Estado pessoal é local e soberano.** Nunca depende da API para saber se algo foi assistido.
2. **Offline-first.** Room é a fonte da verdade para a UI; a rede só alimenta o cache.
3. **Simplicidade.** Nada de dependência por conveniência; abstrações só quando pagam o custo.
4. **Incremental.** Desenvolvido em fases (ver ROADMAP.md). Cada fase compila e passa nos testes.
5. **Documentado.** Decisões relevantes vão para DECISIONS.md.

## Fora de escopo (por ora)

- Contas, login, sincronização em nuvem.
- Recursos sociais (amigos, feed, comentários).
- Publicação na Play Store.
- Tablets/TV como alvo primário (deve funcionar, mas otimização é para celular).

## Documentos

| Arquivo | Conteúdo |
|---|---|
| PROJECT.md | Visão, objetivos, princípios (este arquivo) |
| ARCHITECTURE.md | Camadas, pacotes, stack, fluxo de dados, cache |
| DATABASE.md | Modelo de dados Room |
| TMDB.md | Integração com a API, endpoints, API key |
| ROADMAP.md | Fases e estado atual |
| DECISIONS.md | Registro de decisões (ADR simplificado) |

## Regra de trabalho

Antes de modificar código: ler PROJECT, ARCHITECTURE, DATABASE, ROADMAP (e DECISIONS), examinar o código existente e entender o estado atual. Nunca assumir estado inicial, nunca recriar arquivos sem necessidade, preservar o que funciona. Commits somente quando solicitado.
