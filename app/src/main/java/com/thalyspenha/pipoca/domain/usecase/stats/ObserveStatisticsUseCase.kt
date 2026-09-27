package com.thalyspenha.pipoca.domain.usecase.stats

import com.thalyspenha.pipoca.domain.repository.StatsRepository
import com.thalyspenha.pipoca.domain.stats.Statistics
import com.thalyspenha.pipoca.domain.stats.StatsCalculator
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

/** Estatísticas com mês/ano do calendário atual no fuso do aparelho (D-047). */
class ObserveStatisticsUseCase @Inject constructor(
    private val repository: StatsRepository,
    private val clock: Clock,
    private val zone: ZoneId,
) {
    operator fun invoke(): Flow<Statistics> {
        val (monthStart, yearStart) = StatsCalculator.periodStarts(clock.instant(), zone)
        return repository.observeStatistics(monthStart, yearStart)
    }
}
