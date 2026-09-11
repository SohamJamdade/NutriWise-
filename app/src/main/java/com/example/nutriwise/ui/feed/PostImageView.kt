package com.example.nutriwise.ui.feed

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun PostImageView(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Check if the string is an embedded Base64 image
    val isBase64 = imageUrl.startsWith("data:image") || imageUrl.length > 500

    if (isBase64) {
        // Decode Base64 string directly into a Compose ImageBitmap
        val bitmap = remember(imageUrl) {
            try {
                val cleanBase64 = if (imageUrl.contains(",")) {
                    imageUrl.substringAfter(",")
                } else {
                    imageUrl
                }
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            EmptyImageFallback(modifier)
        }
    } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
        // Standard web link loaded via Coil
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        EmptyImageFallback(modifier)
    }
}

@Composable
private fun EmptyImageFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF1E1E24)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Fastfood,
            contentDescription = null,
            tint = Color.DarkGray,
            modifier = Modifier.size(48.dp)
        )
    }
}