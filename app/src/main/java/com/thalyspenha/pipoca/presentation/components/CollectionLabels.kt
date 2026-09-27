package com.thalyspenha.pipoca.presentation.components

import com.thalyspenha.pipoca.domain.model.CollectionFilter
import com.thalyspenha.pipoca.domain.model.CollectionSort
import com.thalyspenha.pipoca.domain.model.MediaFormat

/** Nomes exibidos da coleção (`fase7.md`). */
val MediaFormat.label: String
    get() = when (this) {
        MediaFormat.UHD_4K_BLURAY -> "4K UHD Blu-ray"
        MediaFormat.BLURAY -> "Blu-ray"
        MediaFormat.DVD -> "DVD"
        MediaFormat.DIGITAL -> "Digital"
        MediaFormat.OTHER -> "Outro"
    }

val CollectionFilter.label: String
    get() = when (this) {
        CollectionFilter.ALL -> "Todos"
        CollectionFilter.UHD_4K -> "4K"
        CollectionFilter.BLURAY -> "Blu-ray"
        CollectionFilter.DVD -> "DVD"
        CollectionFilter.DIGITAL -> "Digital"
    }

val CollectionSort.label: String
    get() = when (this) {
        CollectionSort.TITLE -> "Título"
        CollectionSort.RECENTLY_ADDED -> "Adicionados recentemente"
        CollectionSort.ACQUISITION_DATE -> "Data de aquisição"
    }
