package com.minimal.home

/**
 * Your customization lives here. Edit, commit to GitHub, download the new APK.
 */
object Config {
    // Apps shown first, in this order (package names). Everything else follows alphabetically.
    val pinned = listOf(
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.apps.livetv",
        "org.videolan.vlc"
    )

    // Apps that never show up (package names). Long-press a tile to hide apps on the TV itself.
    val hidden = setOf<String>()

    // Clock format
    const val use24h = false

    // Colors (ARGB hex)
    val accent = 0xFF8AB4FF.toInt()      // focus outline
    val bgTop = 0xFF10151F.toInt()       // wallpaper gradient top
    val bgBottom = 0xFF000000.toInt()    // wallpaper gradient bottom
    val cardColor = 0xFF1B2130.toInt()   // tile background
}
