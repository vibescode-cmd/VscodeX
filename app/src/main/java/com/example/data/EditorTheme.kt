package com.example.data

import androidx.compose.ui.graphics.Color

data class EditorTheme(
    val name: String,
    val background: Color,
    val sidebarBg: Color,
    val editorBg: Color,
    val lineNumbersColor: Color,
    val textColor: Color,
    val keywordColor: Color,
    val stringColor: Color,
    val commentColor: Color,
    val numberColor: Color,
    val accentColor: Color,
    val isDark: Boolean
) {
    companion object {
        val ProfessionalPolish = EditorTheme(
            name = "Professional Polish",
            background = Color(0xFF0D1117),
            sidebarBg = Color(0xFF161B22),
            editorBg = Color(0xFF0D1117),
            lineNumbersColor = Color(0xFF484F58),
            textColor = Color(0xFFC9D1D9),
            keywordColor = Color(0xFFFF7B72),
            stringColor = Color(0xFFA5D6FF),
            commentColor = Color(0xFF8B949E),
            numberColor = Color(0xFF79C0FF),
            accentColor = Color(0xFFF78166),
            isDark = true
        )

        val VSCodeDark = EditorTheme(
            name = "VS Code Dark",
            background = Color(0xFF1E1E1E),
            sidebarBg = Color(0xFF252526),
            editorBg = Color(0xFF1E1E1E),
            lineNumbersColor = Color(0xFF858585),
            textColor = Color(0xFFD4D4D4),
            keywordColor = Color(0xFF569CD6),
            stringColor = Color(0xFFCE9178),
            commentColor = Color(0xFF6A9955),
            numberColor = Color(0xFFB5CEA8),
            accentColor = Color(0xFF007ACC),
            isDark = true
        )

        val Dracula = EditorTheme(
            name = "Dracula",
            background = Color(0xFF282A36),
            sidebarBg = Color(0xFF21222C),
            editorBg = Color(0xFF282A36),
            lineNumbersColor = Color(0xFF6272A4),
            textColor = Color(0xFFF8F8F2),
            keywordColor = Color(0xFFFF79C6),
            stringColor = Color(0xFFF1FA8C),
            commentColor = Color(0xFF6272A4),
            numberColor = Color(0xFFBD93F9),
            accentColor = Color(0xFFBD93F9),
            isDark = true
        )

        val Monokai = EditorTheme(
            name = "Monokai",
            background = Color(0xFF272822),
            sidebarBg = Color(0xFF1E1F1C),
            editorBg = Color(0xFF272822),
            lineNumbersColor = Color(0xFF75715E),
            textColor = Color(0xFFF8F8F2),
            keywordColor = Color(0xFFF92672),
            stringColor = Color(0xFFE6DB74),
            commentColor = Color(0xFF75715E),
            numberColor = Color(0xFFAE81FF),
            accentColor = Color(0xFFA6E22E),
            isDark = true
        )

        val SolarizedLight = EditorTheme(
            name = "Solarized Light",
            background = Color(0xFFFDF6E3),
            sidebarBg = Color(0xFFEEE8D5),
            editorBg = Color(0xFFFDF6E3),
            lineNumbersColor = Color(0xFF93A1A1),
            textColor = Color(0xFF657B83),
            keywordColor = Color(0xFF859900),
            stringColor = Color(0xFF2AA198),
            commentColor = Color(0xFF93A1A1),
            numberColor = Color(0xFFCB4B16),
            accentColor = Color(0xFF268BD2),
            isDark = false
        )

        val RetroMatrix = EditorTheme(
            name = "Retro Matrix",
            background = Color(0xFF000000),
            sidebarBg = Color(0xFF050505),
            editorBg = Color(0xFF000000),
            lineNumbersColor = Color(0xFF00FF00),
            textColor = Color(0xFF33FF33),
            keywordColor = Color(0xFF00FF00),
            stringColor = Color(0xFF88FF88),
            commentColor = Color(0xFF005500),
            numberColor = Color(0xFF55FF55),
            accentColor = Color(0xFF33FF33),
            isDark = true
        )

        val ALL_THEMES = listOf(ProfessionalPolish, VSCodeDark, Dracula, Monokai, SolarizedLight, RetroMatrix)
    }
}
