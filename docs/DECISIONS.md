# DECISIONS

Registro de decisões arquiteturais. Formato: contexto → decisão → consequência. Decisões não são apagadas; se mudarem, marcar como **Substituída por D-XXX**.

---

### D-001 — Módulo único
**Contexto:** projeto pessoal, um desenvolvedor.
**Decisão:** um único módulo Gradle `:app`, com separação por pacotes.
**Consequência:** build simples. Se crescer, pacotes já separados facilitam extrair módulos.

### D-002 — MVVM + Clean Architecture leve
**Decisão:** camadas `presentation`, `domain`, `data`. `domain` em Kotlin puro. Sem módulos separados por camada.
**Consequência:** testabilidade sem cerimônia excessiva.

### D-003 — Room como fonte única da verdade
**Contexto:** requisito offline-first.
**Decisão:** UI observa Flows do Room; rede apenas atualiza o banco.
**Consequência:** offline funciona por padrão; toda tela de detalhe passa pelo banco.

### D-004 — kotlinx.serialization
**Decisão:** usar kotlinx.serialization para JSON (converter Retrofit oficial) e rotas type-safe do Navigation Compose. Gson descartado.
**Consequência:** uma biblioteca de serialização só; compatível com Kotlin sem reflexão.

### D-005 — ViewModel junto da tela
**Contexto:** prompt mestre sugere pasta `viewmodel/` global.
**Decisão:** organizar `presentation/screens/<feature>/` com Screen, ViewModel e UiState juntos.
**Consequência:** navegação por feature mais fácil.

### D-006 — Use cases apenas com regra de negócio
**Decisão:** criar use case quando houver lógica (progresso, status, marcações em lote). Leitura simples: ViewModel → interface de repositório.
**Consequência:** evita use cases "passa-recado".

### D-007 — Dados pessoais sem FK para o cache
**Decisão:** tabelas pessoais referenciam IDs TMDB sem foreign key; cache `tmdb_*` pode ser apagado/atualizado livremente.
**Consequência:** dado pessoal nunca some por atualização de cache. Integridade garantida no repositório; ao adicionar à biblioteca, garantir que o cache do item exista.

### D-008 — Busca não é persistida
**Decisão:** resultados de busca vêm direto da rede (debounce 400 ms). Só itens abertos/adicionados vão para o cache.
**Consequência:** banco não enche de dados irrelevantes; busca exige internet (aceito pelo prompt mestre).

### D-009 — Favorite e Rating como colunas
**Contexto:** prompt mestre sugere entidades Favorite e Rating.
**Decisão:** `is_favorite` e `rating` em `user_movie`/`user_tv_show`.
**Consequência:** menos JOINs; relação é 1:1. Favoritar sem status definido — resolver na fase de biblioteca (provável: favoritar adiciona à biblioteca).

### D-010 — minSdk 26
**Decisão:** `minSdk 26` (Android 8.0), `compileSdk/targetSdk` último estável.
**Consequência:** `java.time` nativo, sem desugaring; cobre qualquer celular atual.

### D-011 — Nome: Pipoca
**Contexto:** o projeto começou numa pasta `tv_time`, nome de marca de terceiros.
**Decisão:** app se chama **Pipoca**. `applicationId`/pacote base: `com.thalyspenha.pipoca`. Repositório: `git@github.com:thalyspenha/pipoca.git`, pasta `~/Projetos/Pessoal/pipoca`.
**Consequência:** `applicationId` não deve mudar depois da primeira instalação.

### D-012 — Autenticação TMDB por Bearer token
**Decisão:** usar API Read Access Token (v4) no header `Authorization`, via interceptor, lido de `local.properties` → `BuildConfig.TMDB_API_TOKEN`.
**Consequência:** chave fora das URLs/logs de query. Obs.: chave em `BuildConfig` fica no APK — aceitável para app pessoal não publicado.
