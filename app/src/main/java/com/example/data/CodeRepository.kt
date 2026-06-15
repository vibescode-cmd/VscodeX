package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodeRepository(private val codeDao: CodeDao) {

    val allFilesFlow: Flow<List<CodeFile>> = codeDao.getAllFilesFlow()
    val allExtensionsFlow: Flow<List<ExtensionItem>> = codeDao.getAllExtensionsFlow()
    val gitReposFlow: Flow<List<GitRepo>> = codeDao.getGitReposFlow()

    fun getSnippetsForLanguageFlow(language: String): Flow<List<Snippet>> = 
        codeDao.getSnippetsForLanguageFlow(language)

    suspend fun getFileByPath(path: String): CodeFile? = withContext(Dispatchers.IO) {
        codeDao.getFileByPath(path)
    }

    suspend fun insertFile(file: CodeFile) = withContext(Dispatchers.IO) {
        codeDao.insertFile(file)
    }

    suspend fun deleteFileOrFolder(path: String) = withContext(Dispatchers.IO) {
        codeDao.deleteFileOrFolder(path)
    }

    suspend fun insertSnippet(snippet: Snippet) = withContext(Dispatchers.IO) {
        codeDao.insertSnippet(snippet)
    }

    suspend fun deleteSnippet(snippet: Snippet) = withContext(Dispatchers.IO) {
        codeDao.deleteSnippet(snippet)
    }

    suspend fun insertExtension(extension: ExtensionItem) = withContext(Dispatchers.IO) {
        codeDao.insertExtension(extension)
    }

    suspend fun updateExtensionState(id: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        codeDao.updateExtensionState(id, isEnabled)
    }

    suspend fun insertGitRepo(repo: GitRepo) = withContext(Dispatchers.IO) {
        codeDao.insertGitRepo(repo)
    }

    suspend fun deleteGitRepo(repo: GitRepo) = withContext(Dispatchers.IO) {
        codeDao.deleteGitRepo(repo)
    }

    suspend fun seedInitialDataIfNecessary() = withContext(Dispatchers.IO) {
        // 1. Seed Files if empty
        val files = codeDao.getAllFiles()
        if (files.isEmpty()) {
            val fileList = listOf(
                CodeFile("README.md", "README.md", """# VS Code Android Edition

Selamat datang di IDE VS Code profesional untuk Android! 📱💻
Dilengkapi terminal termux-like, asisten AI Gemini, dan sinkronisasi Git.

### Fitur Utama:
1. **Multi-Language Support**: Editor kode dengan highlight sintaksis javascript, python, html, css, dan kotlin.
2. **Terminal Terintegrasi**: Simulasi lingkungan UNIX seperti Termux untuk menjalankan kode pemrograman secara efisien.
3. **Integrasi Git**: Sinkronisasi pekerjaan Anda dengan repositori cloud dan lokal.
4. **Ekstensi Terpasang**:
   - 🤖 *Gemini Copilot*: Klik tombol AI (bintang) di editor untuk auto-complete, refactor, atau minta penjelasan.
   - 🎨 *Auto Formatting*: Merapikan sintaksis penulisan kode Anda dengan satu sentuhan.
   - ⌨️ *Shortcut Bar*: Barisan pintasan karakter khusus `{`, `}`, `[`, `]`, `;`, dll di atas keyboard untuk efisiensi menulis kode di HP.

### Cara Menjalankan Kode:
Ketik perintah di Terminal bawah:
`ls` -> melihat berkas proyek.
`run project/main.js` -> menjalankan javascript.
`run project/app.py` -> menjalankan python.
`run project/Main.kt` -> menjalankan kotlin.
`help` -> menampilkan semua daftar perintah terminal.
""", "markdown", false),

                CodeFile("project", "project", "", "folder", true),

                CodeFile("project/main.js", "main.js", """// Program JavaScript Sederhana
const numbers = [1, 2, 3, 4, 5];
console.log("Menghitung perkalian matriks kuadrat:");

function calculateSumAndProduct(array) {
    let sum = 0;
    let product = 1;
    for (let i = 0; i < array.length; i++) {
        sum += array[i];
        product *= array[i];
    }
    return { sum, product };
}

const result = calculateSumAndProduct(numbers);
console.log("Jumlah elemen array: " + result.sum);
console.log("Hasil kali elemen array: " + result.product);
""", "javascript", false),

                CodeFile("project/app.py", "app.py", """# Program Python Menghitung Faktorial & Fibonacci
def hitung_faktorial(n):
    if n == 0 or n == 1:
        return 1
    else:
        return n * hitung_faktorial(n - 1)

def deret_fibonacci(limit):
    fib = [0, 1]
    while fib[-1] + fib[-2] < limit:
        fib.append(fib[-1] + fib[-2])
    return fib

angka = 6
print(f"Faktorial dari {angka} adalah: {hitung_faktorial(angka)}")
print(f"Deret Fibonacci di bawah 100: {deret_fibonacci(100)}")
""", "python", false),

                CodeFile("project/Main.kt", "Main.kt", """// Program Kotlin Klasik
fun main() {
    val languages = listOf("Java", "Kotlin", "TypeScript", "Python", "Swift")
    println("--- Menjalankan Kode Kotlin Sederhana ---")
    
    val filtered = languages.filter { it.startsWith("J") || it.startsWith("K") }
    println("Bahasa yang diawali huruf J atau K:")
    filtered.forEach { name ->
        println("- " + name + " (Panjang huruf: " + name.length + ")")
    }
}
""", "kotlin", false),

                CodeFile("project/index.html", "index.html", """<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Droid Code VS</title>
    <style>
        body {
            background-color: #1e1e1e;
            color: #ffffff;
            font-family: sans-serif;
            text-align: center;
            padding: 40px;
        }
        h1 {
            color: #007acc;
        }
        .btn {
            background-color: #007acc;
            border: none;
            color: white;
            padding: 10px 20px;
            cursor: pointer;
            border-radius: 4px;
        }
    </style>
</head>
<body>
    <h1>VS Code Android Terminal</h1>
    <p>Menulis kode html dinamis langsung di genggaman Anda.</p>
    <button class="btn" onclick="alert('Kode berjalan!')">Klik Saya</button>
</body>
</html>
""", "html", false)
            )
            fileList.forEach { codeDao.insertFile(it) }
        }

        // 2. Seed Extensions if empty
        val exts = codeDao.getAllExtensions()
        if (exts.isEmpty()) {
            val extList = listOf(
                ExtensionItem("copilot", "Gemini AI Copilot", "Generasi kode pintar bertenaga kecerdasan buatan untuk pelengkapan sintaks, deteksi eror, dan refactoring.", true, "Google AI", "1.0.0", "ai_stars"),
                ExtensionItem("prettier", "Prettier Code Formatter", "Formatting kode otomatis sekali ketuk untuk merapikan kode lekukan sintaks.", true, "Prettier", "1.2.0", "format_align_left"),
                ExtensionItem("kotlin-runner", "Kotlin runner and interpreter", "Eksekusi runtime compiler kotlin di termux-like terminal.", true, "JetBrains", "1.0.4", "terminal"),
                ExtensionItem("git-sync", "Git Cloud Synchronizer", "Fitur commit, push, pull, dan kloning repositori GitHub dengan visual state.", true, "Git Community", "2.1.0", "sync"),
                ExtensionItem("custom-themes", "Themes Pack Developer", "Koleksi visual tema premium: Monokai, VS Dracula, Solarized, Retro Matrix.", true, "VS Theme Group", "1.0.1", "palette")
            )
            extList.forEach { codeDao.insertExtension(it) }
        }

        // 3. Seed Snippets if empty
        val snips = codeDao.getAllSnippets()
        if (snips.isEmpty()) {
            val snipList = listOf(
                Snippet(0, "javascript-for", "for", "for (let i = 0; i < array.length; i++) {\n    const element = array[i];\n}", "Loop for standar javascript", "javascript"),
                Snippet(0, "javascript-func", "fun", "function hitungLuas(lebar, tinggi) {\n    return lebar * tinggi;\n}", "Fungsi standar javascript", "javascript"),
                Snippet(0, "python-def", "def", "def hitung_nilai(nilai_list):\n    total = sum(nilai_list)\n    return total / len(nilai_list)", "Fungsi standar python", "python"),
                Snippet(0, "python-if", "main", "if __name__ == '__main__':\n    print(\"Hello world!\")", "Python main guard block", "python"),
                Snippet(0, "kotlin-fun", "main", "fun main() {\n    println(\"Halo dari IDE!\")\n}", "Fungsi utama kotlin", "kotlin"),
                Snippet(0, "html-skel", "html5", "<!DOCTYPE html>\n<html>\n<head>\n    <title>Judul Dokumen</title>\n</head>\n<body>\n    \n</body>\n</html>", "Kerangka kerja HTML5 lengkap", "html")
            )
            snipList.forEach { codeDao.insertSnippet(it) }
        }
    }
}
