package com.usmandiscountstore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usmandiscountstore.app.ui.theme.BrandGreen
import com.usmandiscountstore.app.ui.theme.BrandOrange

/**
 * UDS + Shopping Cart Logo
 * Usage: AppLogo(size = 100.dp)
 */
@Composable
fun AppLogo(size: Dp = 100.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 4),
        color = BrandGreen
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "UDS",
                fontSize = (size.value / 4).sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Store",
                tint = BrandOrange,
                modifier = Modifier.size(size / 3)
            )
        }
    }
}

/**
 * Compact logo for TopBar (small)
 */
@Composable
fun AppLogoSmall(size: Dp = 40.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 4),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "UDS",
                fontSize = (size.value / 4.5).sp,
                fontWeight = FontWeight.Black,
                color = BrandGreen,
                letterSpacing = 0.5.sp
            )
        }
    }
}
