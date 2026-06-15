package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeDao {
    // --- File queries ---
    @Query("SELECT * FROM code_files ORDER BY path ASC")
    fun getAllFilesFlow(): Flow<List<CodeFile>>

    @Query("SELECT * FROM code_files")
    suspend fun getAllFiles(): List<CodeFile>

    @Query("SELECT * FROM code_files WHERE path = :path")
    suspend fun getFileByPath(path: String): CodeFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: CodeFile)

    @Delete
    suspend fun deleteFile(file: CodeFile)

    @Query("DELETE FROM code_files WHERE path = :path OR path LIKE :path || '/%'")
    suspend fun deleteFileOrFolder(path: String)

    // --- Snippets queries ---
    @Query("SELECT * FROM snippets WHERE language = :language OR language = 'all'")
    fun getSnippetsForLanguageFlow(language: String): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets")
    suspend fun getAllSnippets(): List<Snippet>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: Snippet)

    @Delete
    suspend fun deleteSnippet(snippet: Snippet)

    // --- Extensions queries ---
    @Query("SELECT * FROM extensions")
    fun getAllExtensionsFlow(): Flow<List<ExtensionItem>>

    @Query("SELECT * FROM extensions")
    suspend fun getAllExtensions(): List<ExtensionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: ExtensionItem)

    @Query("UPDATE extensions SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateExtensionState(id: String, isEnabled: Boolean)

    // --- Git queries ---
    @Query("SELECT * FROM git_repos")
    fun getGitReposFlow(): Flow<List<GitRepo>>

    @Query("SELECT * FROM git_repos")
    suspend fun getGitRepos(): List<GitRepo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGitRepo(repo: GitRepo)

    @Delete
    suspend fun deleteGitRepo(repo: GitRepo)
}
