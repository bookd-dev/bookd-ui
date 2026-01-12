package com.bookd.app.basic.extension

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

fun Modifier.noRippleClickable(
    interactionSource: MutableInteractionSource? = MutableInteractionSource(),
    indication: Indication? = null,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = clickable(
    interactionSource = interactionSource,
    indication = indication,
    enabled = enabled,
    onClick = onClick,
    role = role
)

fun Modifier.noRippleSelectable(
    selected: Boolean,
    interactionSource: MutableInteractionSource? = MutableInteractionSource(),
    indication: Indication? = null,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = selectable(
    selected = selected,
    interactionSource = interactionSource,
    indication = indication,
    enabled = enabled,
    role = role,
    onClick = onClick
)