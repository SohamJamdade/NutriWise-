package com.example.nutriwise.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object QuickCommerceLauncher {

    private fun urlEncode(query: String): String {
        return URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
    }

    fun openBlinkit(context: Context, query: String) {
        val encodedQuery = urlEncode(query)
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("blinkit://search?q=$encodedQuery")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://blinkit.com/s/?q=$encodedQuery")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        launchSafely(context, appIntent, webIntent, "Blinkit")
    }

    fun openZepto(context: Context, query: String) {
        val encodedQuery = urlEncode(query)
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("zepto://search?query=$encodedQuery")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.zeptonow.com/search?q=$encodedQuery")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        launchSafely(context, appIntent, webIntent, "Zepto")
    }

    fun openInstamart(context: Context, query: String) {
        val encodedQuery = urlEncode(query)
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("swiggy://instamart/search?query=$encodedQuery")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.swiggy.com/instamart/search?custom_back=true&query=$encodedQuery")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        launchSafely(context, appIntent, webIntent, "Swiggy Instamart")
    }

    private fun launchSafely(context: Context, appIntent: Intent, webIntent: Intent, appName: String) {
        try {
            context.startActivity(appIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open $appName search.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}