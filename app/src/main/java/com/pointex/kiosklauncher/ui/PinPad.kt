package com.pointex.kiosklauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Maximum PIN length supported by [PinDots] and [PinKeypad]. */
const val PIN_MAX_LENGTH = 6

/** Row of dots showing how many digits of [pinLength] (max [PIN_MAX_LENGTH]) have been entered. */
@Composable
fun PinDots(pinLength: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(PIN_MAX_LENGTH) { index ->
            val filled = index < pinLength
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

/** A 3x4 numeric keypad (1-9, backspace, 0, confirm) for PIN entry. */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (digit in row) {
                    PinKey(
                        modifier = Modifier.weight(1f),
                        onClick = { onDigit(digit) },
                    ) {
                        Text(text = digit.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PinKey(
                modifier = Modifier.weight(1f),
                onClick = onBackspace,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Effacer",
                )
            }
            PinKey(
                modifier = Modifier.weight(1f),
                onClick = { onDigit('0') },
            ) {
                Text(text = "0", style = MaterialTheme.typography.headlineSmall)
            }
            PinKey(
                modifier = Modifier.weight(1f),
                onClick = onConfirm,
                enabled = confirmEnabled,
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PinKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1.4f)
            .clip(MaterialTheme.shapes.large)
            .background(containerColor)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

/** Centered helper/error text shown above the PIN dots. */
@Composable
fun PinPadTitle(text: String, isError: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}
