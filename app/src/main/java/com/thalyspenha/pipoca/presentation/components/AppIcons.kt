package com.thalyspenha.pipoca.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ícones Material que não estão em `material-icons-core` (evita a dependência `extended`).
 * Caminhos copiados dos ícones Material oficiais (24dp).
 */
object AppIcons {
    val Pause: ImageVector by lazy { icon("Pause", "M6,19h4V5H6v14zM14,5v14h4V5h-4z") }

    val GridView: ImageVector by lazy {
        icon(
            "GridView",
            "M3,3v8h8V3H3zM9,9H5V5h4V9zM3,13v8h8v-8H3zM9,19H5v-4h4V19zM13,3v8h8V3H13zM19,9h-4V5h4V9z" +
                "M13,13v8h8v-8H13zM19,19h-4v-4h4V19z",
        )
    }

    /** Aba Biblioteca: não confunde com o botão "Ver em lista" (D-061). */
    val VideoLibrary: ImageVector by lazy {
        icon(
            "VideoLibrary",
            "M4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6zM20,2H8C6.9,2 6,2.9 6,4v12c0,1.1 0.9,2 2,2h12" +
                "c1.1,0 2,-0.9 2,-2V4C22,2.9 21.1,2 20,2zM12,14.5v-9l6,4.5L12,14.5z",
        )
    }

    /** Aba Coleção (disco): a estrela ficava para a nota pessoal (D-061). */
    val Album: ImageVector by lazy {
        icon(
            "Album",
            "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,16.5" +
                "c-2.49,0 -4.5,-2.01 -4.5,-4.5S9.51,7.5 12,7.5s4.5,2.01 4.5,4.5 -2.01,4.5 -4.5,4.5z" +
                "M12,11c-0.55,0 -1,0.45 -1,1s0.45,1 1,1 1,-0.45 1,-1 -0.45,-1 -1,-1z",
        )
    }

    /** Entrada Estatísticas no "Mais" (o ⓘ parecia "Sobre"). */
    val BarChart: ImageVector by lazy { icon("BarChart", "M4,9h4v11H4zM16,13h4v7h-4zM10,4h4v16h-4z") }

    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes(path), fill = SolidColor(Color.Black))
            .build()
}
