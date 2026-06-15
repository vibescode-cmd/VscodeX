package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CodeFile::class, Snippet::class, ExtensionItem::class, GitRepo::class],
    version = 2,
    exportSchema = false
)
abstract class CodeDatabase : RoomDatabase() {
    abstract fun codeDao(): CodeDao

    companion object {
        @Volatile
        private var INSTANCE: CodeDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the new table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `git_repos_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `repoName` TEXT NOT NULL, 
                        `remoteUrl` TEXT NOT NULL, 
                        `branchName` TEXT NOT NULL, 
                        `encryptedUsername` TEXT NOT NULL, 
                        `encryptedToken` TEXT NOT NULL, 
                        `lastSyncTime` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Migrate and encrypt existing repo user/token
                val cursor = db.query("SELECT id, repoName, remoteUrl, branchName, username, token, lastSyncTime FROM git_repos")
                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            val id = cursor.getInt(0)
                            val repoName = cursor.getString(1)
                            val remoteUrl = cursor.getString(2)
                            val branchName = cursor.getString(3)
                            val username = cursor.getString(4)
                            val token = cursor.getString(5)
                            val lastSyncTime = cursor.getLong(6)

                            val encUsername = CredentialEncryption.encrypt(username)
                            val encToken = CredentialEncryption.encrypt(token)

                            db.execSQL(
                                "INSERT INTO git_repos_new (id, repoName, remoteUrl, branchName, encryptedUsername, encryptedToken, lastSyncTime) VALUES (?, ?, ?, ?, ?, ?, ?)",
                                arrayOf(id, repoName, remoteUrl, branchName, encUsername, encToken, lastSyncTime)
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        cursor.close()
                    }
                }

                // Drop old table and rename new table
                db.execSQL("DROP TABLE IF EXISTS git_repos")
                db.execSQL("ALTER TABLE git_repos_new RENAME TO git_repos")
            }
        }

        fun getDatabase(context: Context): CodeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodeDatabase::class.java,
                    "vscode_android_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
