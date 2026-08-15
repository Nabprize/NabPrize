package com.nabprize.play.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nabprize.play.R

val Poppins: FontFamily = FontFamily(
    // lighter weights → regular file
    Font(R.font.poppins_regular, FontWeight.Thin),
    Font(R.font.poppins_regular, FontWeight.ExtraLight),
    Font(R.font.poppins_regular, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_regular, FontWeight.Medium),
    // semi-bold and heavier → semibold file
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_semibold, FontWeight.Bold),
    Font(R.font.poppins_semibold, FontWeight.ExtraBold),
    Font(R.font.poppins_semibold, FontWeight.Black)
)
