package com.nabprize.play.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nabprize.play.R

/**
 * NP-Coin brand logo — use this wherever an NP-Coin icon/badge is needed.
 */
@Composable
fun NpCoinImage(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Image(
        painter = painterResource(R.drawable.img_np_coin),
        contentDescription = "NP-Coin",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
