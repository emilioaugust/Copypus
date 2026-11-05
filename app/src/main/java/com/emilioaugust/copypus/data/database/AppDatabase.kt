package com.emilioaugust.copypus.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.emilioaugust.copypus.data.entity.ClipboardItem
import com.emilioaugust.copypus.data.dao.ClipboardDao
import com.emilioaugust.copypus.data.dao.SnippetDao
import com.emilioaugust.copypus.data.entity.SnippetEntity

@Database(entities = [ClipboardItem::class, SnippetEntity::class], version = 10)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao
    abstract fun snippetDao(): SnippetDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS clipboard_items_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                text TEXT,
                imagePath TEXT,
                type TEXT NOT NULL,
                isFavorite INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO clipboard_items_new (id, text, imagePath, type, isFavorite, timestamp)
            SELECT 
                id, 
                text, 
                NULL, 
                'TEXT', 
                isFavorite, 
                timestamp 
            FROM clipboard_items
        """.trimIndent())

                db.execSQL("DROP TABLE clipboard_items")
                db.execSQL("ALTER TABLE clipboard_items_new RENAME TO clipboard_items")

                db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_clipboard_items_text 
            ON clipboard_items(text)
        """.trimIndent())
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            CREATE TABLE IF NOT EXISTS clipboard_items_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                text TEXT,
                imagePath TEXT,
                type TEXT NOT NULL,
                isFavorite INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())

                db.execSQL("""
            INSERT INTO clipboard_items_new (id, text, imagePath, type, isFavorite, timestamp)
            SELECT id, text, imagePath, type, isFavorite, timestamp FROM clipboard_items
        """.trimIndent())

                db.execSQL("DROP TABLE clipboard_items")
                db.execSQL("ALTER TABLE clipboard_items_new RENAME TO clipboard_items")

                db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS index_clipboard_items_text 
            ON clipboard_items(text)
        """.trimIndent())
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            ALTER TABLE clipboard_items
            ADD COLUMN imageHash TEXT
        """.trimIndent())
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS snippets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        key TEXT NOT NULL,
                        value TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_snippets_key
                    ON snippets(key)
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "clipboard_database"
                    )
                        .addMigrations(
                            MIGRATION_6_7, MIGRATION_7_8,
                            MIGRATION_8_9, MIGRATION_9_10
                        )
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}