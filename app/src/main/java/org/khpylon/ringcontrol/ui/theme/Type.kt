package org.khpylon.ringcontrol.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with

private val defaultTypography = Typography()
val Typography = Typography(
    labelLarge = defaultTypography.labelLarge.copy(fontSize = 14.sp),
    labelMedium = defaultTypography.labelMedium.copy(fontSize = 12.sp),
    labelSmall = defaultTypography.labelSmall.copy(fontSize = 10.sp),

    bodyLarge = defaultTypography.bodyLarge.copy(fontSize = 14.sp),
    bodyMedium = defaultTypography.bodyMedium.copy(fontSize = 12.sp),
    bodySmall = defaultTypography.bodySmall.copy(fontSize = 10.sp),

    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)