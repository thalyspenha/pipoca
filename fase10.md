# IMPLEMENTAR FASE 10 — POLIMENTO E RELEASE PESSOAL

Agora trate o projeto como uma versão utilizável.

Não reescreva partes funcionais sem necessidade.

Primeiro faça uma auditoria completa.

Verifique:

* arquitetura;
* banco;
* navegação;
* API;
* cache;
* estados;
* tratamento de erros;
* segurança;
* UI;
* acessibilidade;
* performance.

## OFFLINE

Garantir que:

* biblioteca funciona offline;
* coleção funciona offline;
* favoritos funcionam offline;
* histórico funciona offline;
* progresso funciona offline;
* detalhes previamente carregados permanecem disponíveis.

Quando não houver internet:

mostrar uma mensagem amigável apenas quando uma operação realmente exigir conexão.

## PERFORMANCE

Verificar:

* carregamento de imagens;
* listas LazyColumn/LazyGrid;
* recomposição desnecessária;
* chamadas repetidas ao TMDB;
* queries do Room;
* cache.

## UI

Polir:

* espaçamentos;
* tipografia;
* cards;
* posters;
* loading;
* erros;
* empty states;
* Dark Mode;
* Light Mode;
* navegação.

Não introduzir animações excessivas.

## TESTES

Adicionar/corrigir testes para:

* Repository;
* Use Cases;
* ViewModels;
* cálculo de progresso;
* estatísticas;
* banco.

## TMDB

Criar tela:

Configurações → Sobre

Informar que o aplicativo utiliza a API do TMDB e adicionar a atribuição exigida pelo TMDB.

## CONFIGURAÇÕES

Criar:

* Sobre;
* Tema;
* Limpar cache;
* informações do banco;
* versão do aplicativo.

Não criar sistema de login.

Não criar backend.

Não criar sincronização em nuvem.

Não criar anúncios.

## RELEASE

Gerar APK debug funcional.

Se o ambiente permitir, gerar também APK release.

Não publicar na Google Play.

Antes de finalizar:

1. executar ./gradlew clean;
2. executar build;
3. executar testes;
4. corrigir todos os erros possíveis;
5. verificar warnings importantes;
6. atualizar toda documentação;
7. atualizar ROADMAP.md;
8. criar RELEASE_NOTES.md.

No final informe:

* funcionalidades implementadas;
* funcionalidades pendentes;
* localização do APK;
* comando para instalar via ADB;
* configuração necessária;
* limitações conhecidas;
* próximos passos opcionais.

# FIM DA FASE 10
