package com.maez.qubitexplorer.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
 * - Elástica: lo que crece al tocarlo (barra de búsqueda, paneles, barras de acciones). Al encogerse no rebota.
 * - Indicador: lo que se desplaza para marcar una selección (pestañas, segmentos).
 * - Fundido: cambios de color y de contenido sin cambio de tamaño.
 */
object Motion {
    const val FAST_MS = 180
    const val NORMAL_MS = 220

    /** Cierre: arranca suave y se asienta brevemente, sin cola lenta ni parada en seco. */
    private val collapseEasing = CubicBezierEasing(0.4f, 0f, 0.7f, 1f)

    fun <T> elastic(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> indicator(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> fade(): FiniteAnimationSpec<T> = tween(FAST_MS)

    /** Rebote leve en ambos sentidos: el panel que se expande y se contrae con el progreso de una tarea. */
    fun <T> softBounce(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)

    /** Paneles y barras que aparecen debajo de algo (filtros, selección, portapapeles, menús). */
    val panelEnter: EnterTransition =
        expandVertically(elastic<IntSize>(), expandFrom = Alignment.Top) + fadeIn(fade())
    val panelExit: ExitTransition =
        shrinkVertically(tween<IntSize>(NORMAL_MS, easing = collapseEasing), shrinkTowards = Alignment.Top) + fadeOut(tween(NORMAL_MS))

    /** Progreso dentro de un panel: a diferencia del resto, también rebota levemente al contraerse. */
    val progressEnter: EnterTransition =
        expandVertically(softBounce<IntSize>(), expandFrom = Alignment.Top) + fadeIn(fade())
    val progressExit: ExitTransition =
        shrinkVertically(softBounce<IntSize>(), shrinkTowards = Alignment.Top) + fadeOut(fade())

    /** Elementos de una fila que se apartan o vuelven a su sitio (iconos de herramientas, etiquetas). */
    val inlineEnter: EnterTransition =
        expandHorizontally(elastic<IntSize>(), expandFrom = Alignment.Start) + fadeIn(fade())
    val inlineExit: ExitTransition =
        shrinkHorizontally(tween<IntSize>(NORMAL_MS, easing = collapseEasing), shrinkTowards = Alignment.End) + fadeOut(tween(NORMAL_MS))

    /**
     * Barra de selección: entra desde la izquierda tapando las herramientas, en su mismo lugar y altura
     * (la lista no se mueve), y vuelve por donde vino al deseleccionar o terminar la acción.
     */
    val selectionBarEnter: EnterTransition =
        slideInHorizontally(elastic<IntOffset>()) { w -> -w } + fadeIn(fade())
    val selectionBarExit: ExitTransition =
        slideOutHorizontally(tween<IntOffset>(NORMAL_MS)) { w -> -w } + fadeOut(fade())

    /** Herramientas que la barra de selección aparta hacia la derecha y que regresan al terminar. */
    val toolsEnter: EnterTransition =
        slideInHorizontally(elastic<IntOffset>()) { w -> w / 3 } + fadeIn(fade())
    val toolsExit: ExitTransition =
        slideOutHorizontally(tween<IntOffset>(NORMAL_MS)) { w -> w / 3 } + fadeOut(fade())

    /** Elementos pequeños que aparecen en cada fila (casillas de selección): suaves, sin rebote. */
    val softInlineEnter: EnterTransition =
        expandHorizontally(tween<IntSize>(NORMAL_MS, easing = FastOutSlowInEasing), expandFrom = Alignment.Start) +
            fadeIn(tween(NORMAL_MS))
    val softInlineExit: ExitTransition =
        shrinkHorizontally(tween<IntSize>(FAST_MS, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Start) +
            fadeOut(tween(FAST_MS))

    /** Lo mismo para elementos superpuestos que no ocupan espacio en la fila (casilla sobre una miniatura). */
    val softPopEnter: EnterTransition =
        fadeIn(tween(NORMAL_MS)) + scaleIn(tween(NORMAL_MS, easing = FastOutSlowInEasing), initialScale = 0.8f)
    val softPopExit: ExitTransition = fadeOut(tween(FAST_MS)) + scaleOut(tween(FAST_MS), targetScale = 0.8f)

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
