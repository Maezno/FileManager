package com.maez.qubitexplorer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maez.qubitexplorer.R
import com.maez.qubitexplorer.services.TipJar
import com.maez.qubitexplorer.ui.components.SecondaryButton
import com.maez.qubitexplorer.ui.components.SectionCard
import com.maez.qubitexplorer.ui.theme.Aqua
import com.maez.qubitexplorer.ui.theme.TextMuted
import com.maez.qubitexplorer.ui.theme.Violet
import com.maez.qubitexplorer.ui.theme.WarningAmber

/** Sección "Apoyar el desarrollo" de Ajustes: tres propinas opcionales. */
@Composable
fun SupportSection() {
    val context = LocalContext.current
    val tipJar = remember(context) { TipJar(context) }
    DisposableEffect(tipJar) {
        tipJar.connect()
        onDispose { tipJar.disconnect() }
    }
    val prices by tipJar.prices.collectAsState()
    val event by tipJar.events.collectAsState()

    val thanks = stringResource(R.string.support_thanks)
    val unavailable = stringResource(R.string.support_unavailable)
    LaunchedEffect(event) {
        when (event) {
            TipJar.Event.Thanks -> Toast.makeText(context, thanks, Toast.LENGTH_LONG).show()
            TipJar.Event.Unavailable -> Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show()
            null -> return@LaunchedEffect
        }
        tipJar.consumeEvent()
    }

    SectionCard(
        title = stringResource(R.string.support_title),
        icon = Icons.Default.Favorite,
        iconTint = WarningAmber,
        subtitle = stringResource(R.string.support_subtitle)
    ) {
        Text(
            text = stringResource(R.string.support_body),
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tiers = listOf(
                Triple(TipJar.Tier.SMALL, Icons.Default.Coffee, Aqua),
                Triple(TipJar.Tier.MEDIUM, Icons.Default.LocalPizza, Violet),
                Triple(TipJar.Tier.LARGE, Icons.Default.Restaurant, WarningAmber)
            )
            tiers.forEach { (tier, icon, tint) ->
                SecondaryButton(
                    text = prices[tier] ?: tier.fallbackPrice,
                    icon = icon,
                    onClick = { context.findActivity()?.let { tipJar.launch(it, tier) } },
                    modifier = Modifier.weight(1f),
                    tint = tint
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
