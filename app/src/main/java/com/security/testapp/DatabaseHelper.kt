package com.security.testapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "queue.db", null, 1) {

    companion object {
        lateinit var instance: DatabaseHelper
            private set

        fun init(context: Context) {
            instance = DatabaseHelper(context.applicationContext)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE message_queue (id INTEGER PRIMARY KEY AUTOINCREMENT, message TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun insertMessage(message: String) {
        writableDatabase.execSQL("INSERT INTO message_queue (message) VALUES (?)", arrayOf(message))
    }

    val allMessages: List<String>
        get() {
            val list = mutableListOf<String>()
            val cursor = readableDatabase.rawQuery("SELECT message FROM message_queue", null)
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0))
            }
            cursor.close()
            return list
        }

    fun deleteMessage(index: Int) {
        writableDatabase.execSQL(
            "DELETE FROM message_queue WHERE id = (SELECT id FROM message_queue LIMIT 1 OFFSET ?)",
            arrayOf(index)
        )
    }
}
