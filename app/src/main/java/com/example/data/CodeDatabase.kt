package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CodeFile::class, Snippet::class, ExtensionItem::class, GitRepo::class],
    version = 1,
    exportSchema = false
)
abstract class CodeDatabase : RoomDatabase() {
    abstract fun codeDao(): CodeDao

    companion object {
        @Volatile
        private var INSTANCE: CodeDatabase? = null

        fun getDatabase(context: Context): CodeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodeDatabase::class.java,
                    "vscode_android_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
