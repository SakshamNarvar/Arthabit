package com.nstrange.arthabit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nstrange.arthabit.ui.theme.CardBackground
import com.nstrange.arthabit.ui.theme.CardBorder
import com.nstrange.arthabit.ui.theme.CardShadow

/**
 * Neo-brutalist card with shadow offset — replicating the React Native CustomBox component.
 */
@Composable
fun CustomBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(10.dp)

    Box(modifier = modifier.padding(bottom = 5.dp, end = 5.dp)) {
        // Shadow box
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 5.dp, y = 5.dp)
                .clip(shape)
                .background(CardShadow)
        )
        // Main box
        Box(
            modifier = Modifier
                .clip(shape)
                .background(CardBackground)
                .border(2.dp, CardBorder, shape)
                .padding(24.dp),
            content = content
        )
    }
}

