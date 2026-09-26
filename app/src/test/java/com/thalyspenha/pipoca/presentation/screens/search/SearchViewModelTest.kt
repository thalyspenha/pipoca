package com.thalyspenha.pipoca.presentation.screens.search

import com.thalyspenha.pipoca.domain.model.DataError
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MovieSummary
import com.thalyspenha.pipoca.domain.model.SearchPage
import com.thalyspenha.pipoca.domain.model.TvShowSummary
import com.thalyspenha.pipoca.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeSearchRepository()
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SearchViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    /** Espera o debounce passar e a pesquisa terminar. */
    private fun TestScope.settle() = advanceUntilIdle()

    @Test
    fun `estado inicial e Idle sem chamar a rede`() = test {
        settle()

        assertEquals(SearchContent.Idle, viewModel.uiState.value.content)
        assertTrue(repository.movieQueries.isEmpty())
    }

    @Test
    fun `digitacao rapida gera uma unica requisicao com o texto final`() = test {
        viewModel.onQueryChange("m")
        advanceTimeBy(100)
        viewModel.onQueryChange("ma")
        advanceTimeBy(100)
        viewModel.onQueryChange("matrix")
        settle()

        assertEquals(listOf("matrix"), repository.movieQueries)
        val results = viewModel.uiState.value.content as SearchContent.Results
        assertEquals(SearchResultItem(603, SearchType.MOVIES, "Matrix", 1999, "/p.jpg"), results.items.single())
    }

    @Test
    fun `campo mostra o texto na hora, antes do debounce`() = test {
        viewModel.onQueryChange("mat")

        assertEquals("mat", viewModel.uiState.value.query)
    }

    @Test
    fun `nao pesquisa antes de terminar o debounce`() = test {
        viewModel.onQueryChange("matrix")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS - 1)
        runCurrent()

        assertTrue(repository.movieQueries.isEmpty())
    }

    @Test
    fun `texto vazio ou curto nao chama a rede e volta para Idle`() = test {
        viewModel.onQueryChange("matrix")
        settle()
        viewModel.onQueryChange(" a ")
        settle()

        assertEquals(listOf("matrix"), repository.movieQueries)
        assertEquals(SearchContent.Idle, viewModel.uiState.value.content)
    }

    @Test
    fun `mesma pesquisa nao e repetida`() = test {
        viewModel.onQueryChange("matrix")
        settle()
        viewModel.onQueryChange("matrix ")
        settle()
        viewModel.onQueryChange("matri")
        advanceTimeBy(100)
        viewModel.onQueryChange("matrix")
        settle()

        assertEquals(listOf("matrix"), repository.movieQueries)
    }

    @Test
    fun `trocar para series pesquisa na hora com o mesmo texto`() = test {
        viewModel.onQueryChange("breaking bad")
        settle()

        viewModel.onTypeChange(SearchType.TV_SHOWS)
        runCurrent()

        assertEquals(listOf("breaking bad"), repository.tvQueries)
        val results = viewModel.uiState.value.content as SearchContent.Results
        assertEquals(SearchType.TV_SHOWS, results.items.single().type)
        assertEquals(SearchType.TV_SHOWS, viewModel.uiState.value.type)
    }

    @Test
    fun `pesquisa nova cancela a anterior em andamento`() = test {
        repository.delayMs = 1_000
        viewModel.onQueryChange("matrix")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 100)
        assertEquals(SearchContent.Loading, viewModel.uiState.value.content)

        viewModel.onQueryChange("reloaded")
        settle()

        assertEquals(listOf("matrix", "reloaded"), repository.movieQueries)
        assertEquals(1, repository.completed)
        val results = viewModel.uiState.value.content as SearchContent.Results
        assertEquals("reloaded", results.items.single().title)
    }

    @Test
    fun `sem resultados mostra Empty`() = test {
        repository.returnEmpty = true
        viewModel.onQueryChange("xyzxyz")
        settle()

        assertEquals(SearchContent.Empty("xyzxyz"), viewModel.uiState.value.content)
    }

    @Test
    fun `erro mostra Error e retry pesquisa de novo`() = test {
        repository.error = DataError.Network
        viewModel.onQueryChange("matrix")
        settle()
        assertEquals(SearchContent.Error(DataError.Network), viewModel.uiState.value.content)

        repository.error = null
        viewModel.retry()
        settle()

        assertEquals(listOf("matrix", "matrix"), repository.movieQueries)
        assertTrue(viewModel.uiState.value.content is SearchContent.Results)
    }
}

private class FakeSearchRepository : SearchRepository {
    val movieQueries = mutableListOf<String>()
    val tvQueries = mutableListOf<String>()
    var completed = 0
    var delayMs = 0L
    var returnEmpty = false
    var error: DataError? = null

    override suspend fun searchMovies(query: String, page: Int): DataResult<SearchPage<MovieSummary>> {
        movieQueries += query
        return respond(listOf(movie(query)))
    }

    override suspend fun searchTvShows(query: String, page: Int): DataResult<SearchPage<TvShowSummary>> {
        tvQueries += query
        return respond(listOf(show(query)))
    }

    private suspend fun <T> respond(items: List<T>): DataResult<SearchPage<T>> {
        delay(delayMs)
        completed++
        error?.let { return DataResult.Failure(it) }
        val list = if (returnEmpty) emptyList() else items
        return DataResult.Success(SearchPage(page = 1, totalPages = 1, totalResults = list.size, items = list))
    }

    private fun movie(query: String) = MovieSummary(
        id = if (query == "matrix") 603 else 604,
        title = if (query == "matrix") "Matrix" else query,
        originalTitle = "The Matrix",
        overview = null,
        posterPath = "/p.jpg",
        backdropPath = null,
        releaseDate = LocalDate.of(1999, 3, 31),
        voteAverage = 8.2,
    )

    private fun show(query: String) = TvShowSummary(
        id = 1396,
        name = query,
        originalName = query,
        overview = null,
        posterPath = null,
        backdropPath = null,
        firstAirDate = LocalDate.of(2008, 1, 20),
        voteAverage = 9.0,
    )
}
