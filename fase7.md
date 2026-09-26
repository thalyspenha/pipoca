# IMPLEMENTAR FASE 7 — MINHA COLEÇÃO

Implementar gerenciamento da coleção física/digital.

Um título poderá possuir informações de coleção independentes das informações do TMDB.

Criar:

CollectionItem

Campos sugeridos:

* id;
* tmdbId;
* mediaType;
* format;
* edition;
* region;
* quantity;
* acquisitionDate;
* notes.

Formatos:

* 4K UHD Blu-ray
* Blu-ray
* DVD
* Digital
* Outro

Permitir adicionar um filme/série à coleção.

Permitir editar os dados da coleção.

Permitir remover.

Criar tela:

"Minha Coleção"

Mostrar:

* poster;
* título;
* formato;
* edição.

Criar filtros:

Todos
4K
Blu-ray
DVD
Digital

Criar ordenação:

* título;
* adicionado recentemente;
* data de aquisição.

Não misturar "possuo fisicamente" com "assistido".

Um filme pode ser:

4K Blu-ray + Quero assistir

ou:

4K Blu-ray + Assistido.

Implementar corretamente essa independência.

Testar CRUD completo.

Build obrigatório.

Atualizar documentação.
