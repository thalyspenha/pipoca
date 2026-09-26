# IMPLEMENTAR FASE 4 — BIBLIOTECA PESSOAL

Agora implementar a persistência local do usuário.

Antes de começar:

* leia DATABASE.md;
* leia ARCHITECTURE.md;
* analise todas as entidades existentes;
* preserve dados existentes.

Criar ou finalizar entidades Room para:

Movie
TvShow
UserMovie
UserTvShow
Favorite
WatchHistory
Rating

Separar claramente:

DADOS DO TMDB

de

DADOS DO USUÁRIO.

Implementar operações:

* adicionar filme;
* remover filme;
* adicionar série;
* remover série;
* marcar filme como assistido;
* marcar filme como quero assistir;
* marcar série como quero assistir;
* marcar série como assistindo;
* marcar série como concluída;
* favoritar;
* remover favorito;
* adicionar nota pessoal;
* remover nota.

Criar Repository e Use Cases.

A Home deve começar a utilizar dados reais do banco.

Criar migrations corretamente.

Não apagar banco automaticamente durante desenvolvimento.

Criar testes para os principais casos de uso.

Executar build e testes.

Atualizar DATABASE.md e ROADMAP.md.
