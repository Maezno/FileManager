package com.maez.filemanager.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Lenguaje de movimiento único de la app. Cada familia de elementos usa siempre la misma animación:
 * - Elástica: lo que crece o se encoge al tocarlo (barra de búsqueda, paneles, barras de acciones).
 * - Indicador: lo que se desplaza para marcar una selección (pestañas, segmentos).
 * - Fundido: cambios de color y de contenido sin cambio de tamaño.
 */
object Motion {
    const val FAST_MS = 180
    const val NORMAL_MS = 220

    fun <T> elastic(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> indicator(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> fade(): FiniteAnimationSpec<T> = tween(FAST_MS)

    /** Paneles y barras que aparecen debajo de algo (filtros, selección, portapapeles, menús). */
    val panelEnter: EnterTransition =
        expandVertically(elastic<IntSize>(), expandFrom = Alignment.Top) + fadeIn(fade())
    val panelExit: ExitTransition =
        shrinkVertically(elastic<IntSize>(), shrinkTowards = Alignment.Top) + fadeOut(fade())

    /** Elementos de una fila que se apartan o vuelven a su sitio (iconos de herramientas, etiquetas). */
    val inlineEnter: EnterTransition =
        expandHorizontally(elastic<IntSize>(), expandFrom = Alignment.Start) + fadeIn(fade())
    val inlineExit: ExitTransition =
        shrinkHorizontally(elastic<IntSize>(), shrinkTowards = Alignment.End) + fadeOut(fade())

    /** Un botón que aparece en su sitio con un "pop". */
    val popEnter: EnterTransition = scaleIn(elastic()) + fadeIn(fade())
    val popExit: ExitTransition = scaleOut(fade()) + fadeOut(fade())

    /** Panel que nace desde un botón; [origin] es la posición del botón dentro del panel. */
    fun panelFrom(origin: TransformOrigin): Pair<EnterTransition, ExitTransition> =
        (panelEnter + scaleIn(elastic(), initialScale = 0.85f, transformOrigin = origin)) to
            (panelExit + scaleOut(fade(), targetScale = 0.85f, transformOrigin = origin))

    /** Diálogos y vistas a pantalla completa. */
    val dialogEnter: EnterTransition = fadeIn(tween(NORMAL_MS)) + scaleIn(elastic(), initialScale = 0.92f)
    val dialogExit: ExitTransition = fadeOut(fade()) + scaleOut(fade(), targetScale = 0.92f)

    /** Cambio entre pestañas principales; [forward] indica si se avanza hacia la derecha. */
    fun tabEnter(forward: Boolean): EnterTransition =
        slideInHorizontally(tween<IntOffset>(NORMAL_MS)) { w -> ((if (forward) 1 else -1) * w * 0.15f).toInt() } +
            fadeIn(tween(NORMAL_MS))

    fun tabExit(forward: Boolean): ExitTransition =
        slideOutHorizontally(fade<IntOffset>()) { w -> ((if (forward) -1 else 1) * w * 0.15f).toInt() } +
            fadeOut(fade())
}
