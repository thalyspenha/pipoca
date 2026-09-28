# Pipoca — notas de versão

## Não lançado (depois da 1.0.0)

Ainda na versão 1.0.0 (sem novo APK release); instalado no S25 como debug.

- Aba "Mais" sempre reabre no menu (D-059).
- Correções: "hoje" no fuso do aparelho, histórico sem duplicar por toque duplo, toque duplo no status e no "Assisti", plural "1 episódio" (D-060).
- UX: estrelas da nota maiores e "Remover nota", confirmação ao desmarcar temporada, busca sem resultado no topo, gêneros como rótulos, toque na Coleção abre detalhes, novos ícones de Biblioteca/Coleção/Estatísticas (D-061).
- Histórico agrupa episódios marcados juntos (D-062); sinopse antes das temporadas (D-063); abas Filmes/Séries iguais em todas as telas (D-064); título dos detalhes só na barra depois de rolar (D-065).
- "Quero assistir" também para séries, no lugar de "Quero ver" (D-066).

## 1.0.0 (versionCode 2) — 2026-09-28

Primeira versão utilizável, para uso pessoal (fora da Google Play).

### Funcionalidades
- **Início:** continuar assistindo, séries em andamento, quero assistir, assistidos recentemente, favoritos e adicionados recentemente à coleção.
- **Biblioteca:** filmes e séries com filtros por status (com contagem), Grid/Lista, ordenação, pesquisa local e ações rápidas no toque longo.
- **Busca** no TMDB (filmes e séries), com debounce.
- **Detalhes** de filme e série: status, favorito, nota 1–10, elenco, gêneros; séries com Pausada/Abandonada.
- **Temporadas e episódios:** marcar episódio ou temporada, progresso ("37 / 62"), próximo episódio, conclusão automática.
- **Minha Coleção:** mídia física/digital (4K, Blu-ray, DVD, Digital, Outro), edição, região, quantidade, data; filtros e ordenação.
- **Favoritos, Histórico e Estatísticas** (tempo assistido, gêneros, notas, coleção por formato).
- **Configurações:** tema Sistema/Claro/Escuro, limpar cache, informações do banco, versão, Sobre com atribuição ao TMDB.
- **Offline:** biblioteca, coleção, favoritos, histórico, progresso e detalhes já abertos funcionam sem internet; aviso só quando a operação precisa de rede.

### APKs
Gerados com `./gradlew assembleDebug assembleRelease`:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk` (sem R8, assinado com a chave de debug da máquina, D-058)

Instalar (mantém os dados se a assinatura for a mesma do app instalado):
```sh
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Configuração necessária
- `TMDB_API_TOKEN` (Read Access Token v4) no `local.properties` antes do build; sem ele o app abre, mas Busca e detalhes novos mostram erro de chave. Ver `docs/SETUP.md`.

### Limitações conhecidas
- **O token TMDB vai dentro do APK** (`BuildConfig`, D-012): não compartilhe o APK.
- **Assinatura por máquina:** Mac e Linux têm `debug.keystore` diferentes; instalar de uma máquina por cima do app instalado pela outra falha (e desinstalar apaga os dados). Solução: copiar o mesmo `debug.keystore` para as duas (SETUP.md).
- Sem backup/exportação: os dados ficam só no aparelho (desinstalar apaga tudo).
- Tempo assistido e gêneros dependem do cache TMDB; títulos sem duração contam à parte (DATABASE.md).
- Ordenação por título segue a ordem binária do SQLite para acentos ("Árvore" depois de "Z").
- Sobre mostra a atribuição ao TMDB em texto, sem o logo oficial.

### Próximos passos opcionais
- Exportar/importar backup (JSON) para trocar de aparelho.
- Logo oficial do TMDB na tela Sobre.
- R8 no release (exige regras e teste no aparelho).
- Lembrete de novos episódios das séries em andamento.
