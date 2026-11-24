package com.maez.qubitexplorer.services

import java.util.Locale

private fun isSpanish() = Locale.getDefault().language == "es"

/** Valor lógico de una celda, en el idioma del usuario (como lo muestra Excel). */
internal fun sheetBoolean(value: Boolean): String =
    if (isSpanish()) (if (value) "VERDADERO" else "FALSO") else (if (value) "TRUE" else "FALSE")

internal fun defaultSheetName(): String = if (isSpanish()) "Hoja" else "Sheet"
