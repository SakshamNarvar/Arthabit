package com.nstrange.arthabit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nstrange.arthabit.domain.model.Expense
import com.nstrange.arthabit.ui.theme.DarkOnSurface
import com.nstrange.arthabit.ui.theme.DarkSurface
import com.nstrange.arthabit.ui.theme.DarkSurfaceVariant
import com.nstrange.arthabit.ui.theme.DarkTextSecondary
import com.nstrange.arthabit.ui.theme.Primary

/**
 * Single expense row — replicating the React Native Expense component.
 * Uses dark glassmorphic styling from the theme.
 */
@Composable
fun ExpenseItem(
    expense: Expense,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Shopping cart icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Expense",
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
        }

        // Merchant + date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.merchant,
                color = DarkOnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = expense.formattedDate,
                color = DarkTextSecondary,
                fontSize = 13.sp
            )
        }

        // Amount
        Text(
            text = expense.formattedAmount,
            color = DarkOnSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

