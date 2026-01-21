package com.bookd.app.basic.extension

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
        role = role
    )
}

fun Modifier.noRippleSelectable(
    selected: Boolean,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
) = composed {
    selectable(
        selected = selected,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        role = role,
        onClick = onClick
    )
}