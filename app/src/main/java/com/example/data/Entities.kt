package com.example.data

import androidx.room.*

@Entity(tableName = "code_files")
data class CodeFile(
    @PrimaryKey val path: String, // unique file path, e.g. "project/main.js", "README.md"
    val name: String,
    val content: String,
    val language: String,
    val isFolder: Boolean,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "snippets")
data class Snippet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val prefix: String,
    val body: String,
    val description: String,
    val language: String
)

@Entity(tableName = "extensions")
data class ExtensionItem(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val publisher: String,
    val version: String,
    val iconName: String
)

@Entity(tableName = "git_repos")
data class GitRepo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val repoName: String,
    val remoteUrl: String,
    val branchName: String = "main",
    val username: String = "",
    val token: String = "",
    val lastSyncTime: Long = 0L
)
