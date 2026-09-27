package com.thalyspenha.pipoca.presentation.screens.collection.form

import androidx.lifecycle.SavedStateHandle
import com.thalyspenha.pipoca.data.repository.MutableClock
import com.thalyspenha.pipoca.domain.model.CollectionItemDraft
import com.thalyspenha.pipoca.domain.model.CollectionMediaType
import com.thalyspenha.pipoca.domain.model.DataResult
import com.thalyspenha.pipoca.domain.model.MediaFormat
import com.thalyspenha.pipoca.domain.model.MovieDetails
import com.thalyspenha.pipoca.domain.repository.MovieRepository
import com.thalyspenha.pipoca.domain.usecase.collection.AddCollectionItemUseCase
import com.thalyspenha.pipoca.domain.usecase.collection.FakeCollectionRepository
import com.thalyspenha.pipoca.domain.usecase.collection.RemoveCollectionItemUseCase
import com.thalyspenha.pipoca.domain.usecase.collection.UpdateCollectionItemUseCase
import com.thalyspenha.pipoca.domain.usecase.episodes.FakeTvShowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionItemFormViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeCollectionRepository()
    private val clock = MutableClock() // 2026-09-26

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(itemId: Long = 0) = CollectionItemFormViewModel(
        savedStateHandle = SavedStateHandle(mapOf("tmdbId" to 603L, "mediaType" to "MOVIE", "itemId" to itemId)),
        repository = repository,
        movieRepository = TitledMovieRepository,
        tvShowRepository = FakeTvShowRepository(),
        add = AddCollectionItemUseCase(repository, clock),
        update = UpdateCollectionItemUseCase(repository, clock),
        remove = RemoveCollectionItemUseCase(repository),
        clock = clock,
    )

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun `novo item comeca com padroes e titulo do cache`() = test {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isEditing)
        assertEquals("Matrix", state.title)
        assertEquals(MediaFormat.BLURAY, state.format)
        assertEquals("1", state.quantity)
    }

    @Test
    fun `adicionar grava e termina`() = test {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onFormatChange(MediaFormat.UHD_4K_BLURAY)
        vm.onEditionChange(" Steelbook ")
        vm.onRegionChange("")
        vm.onQuantityChange("2")
        vm.onAcquiredAtChange(LocalDate.of(2024, 5, 1))
        vm.onSave()
        advanceUntilIdle()

        assertTrue(vm.state.value.isDone)
        val item = repository.items.value.values.single()
        assertEquals(MediaFormat.UHD_4K_BLURAY, item.format)
        assertEquals("Steelbook", item.edition)
        assertNull(item.region)
        assertEquals(2, item.quantity)
        assertEquals(LocalDate.of(2024, 5, 1), item.acquiredAt)
        assertEquals(CollectionMediaType.MOVIE, item.mediaType)
    }

    @Test
    fun `quantidade invalida mostra erro e nao grava`() = test {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onQuantityChange("0")
        vm.onSave()
        advanceUntilIdle()

        assertTrue(vm.state.value.showErrors)
        assertEquals("Informe de 1 a 99", vm.state.value.quantityError)
        assertFalse(vm.state.value.isDone)
        assertTrue(repository.items.value.isEmpty())
    }

    @Test
    fun `quantidade aceita so digitos, ate 2`() = test {
        val vm = viewModel()
        vm.onQuantityChange("1a23")

        assertEquals("12", vm.state.value.quantity)
    }

    @Test
    fun `data futura mostra erro e nao grava`() = test {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onAcquiredAtChange(LocalDate.of(2026, 9, 27))
        vm.onSave()
        advanceUntilIdle()

        assertEquals("A data não pode ser no futuro", vm.state.value.dateError(vm.today))
        assertTrue(repository.items.value.isEmpty())
    }

    @Test
    fun `editar carrega o item e salva alteracoes`() = test {
        val id = AddCollectionItemUseCase(repository, clock)(
            CollectionItemDraft(603, CollectionMediaType.MOVIE, MediaFormat.DVD, edition = "Simples", quantity = 3),
        )
        val vm = viewModel(itemId = id)
        advanceUntilIdle()

        assertTrue(vm.state.value.isEditing)
        assertEquals(MediaFormat.DVD, vm.state.value.format)
        assertEquals("Simples", vm.state.value.edition)
        assertEquals("3", vm.state.value.quantity)

        vm.onFormatChange(MediaFormat.BLURAY)
        vm.onSave()
        advanceUntilIdle()

        assertEquals(MediaFormat.BLURAY, repository.getItem(id)?.format)
        assertEquals(1, repository.items.value.size)
    }

    @Test
    fun `remover apaga o item e termina`() = test {
        val id = AddCollectionItemUseCase(repository, clock)(
            CollectionItemDraft(603, CollectionMediaType.MOVIE, MediaFormat.DVD),
        )
        val vm = viewModel(itemId = id)
        advanceUntilIdle()

        vm.onDelete()
        advanceUntilIdle()

        assertTrue(vm.state.value.isDone)
        assertNull(repository.getItem(id))
    }

    @Test
    fun `item que nao existe mais fecha o formulario`() = test {
        val vm = viewModel(itemId = 42)
        advanceUntilIdle()

        assertTrue(vm.state.value.isDone)
    }
}

private object TitledMovieRepository : MovieRepository {
    override fun observeMovieDetails(id: Long): Flow<MovieDetails?> = flowOf(
        MovieDetails(
            id = id, title = "Matrix", originalTitle = "The Matrix", overview = null, posterPath = null,
            backdropPath = null, releaseDate = null, runtimeMinutes = null, voteAverage = null,
            genres = emptyList(), directors = emptyList(), cast = emptyList(),
        ),
    )

    override suspend fun refreshMovieDetails(id: Long, force: Boolean): DataResult<Unit> = DataResult.Success(Unit)
}
