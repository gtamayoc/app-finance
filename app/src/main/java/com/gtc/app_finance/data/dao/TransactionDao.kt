package com.gtc.app_finance.data.dao

import android.content.ContentValues
import android.database.Cursor
import com.gtc.app_finance.data.database.TursoDatabaseHelper
import com.gtc.app_finance.data.entity.TransactionEntity

class TransactionDao(private val dbHelper: TursoDatabaseHelper) {

    fun insert(transaction: TransactionEntity): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", transaction.id)
            put("title", transaction.title)
            put("amount", transaction.amount)
            put("type", transaction.type)
            put("category", transaction.category)
            put("date", transaction.date)
        }
        val result = db.insertWithOnConflict("transactions", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        return result != -1L
    }

    fun getAll(): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            "transactions",
            null,
            null,
            null,
            null,
            null,
            "date DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val titleIdx = c.getColumnIndexOrThrow("title")
            val amountIdx = c.getColumnIndexOrThrow("amount")
            val typeIdx = c.getColumnIndexOrThrow("type")
            val categoryIdx = c.getColumnIndexOrThrow("category")
            val dateIdx = c.getColumnIndexOrThrow("date")

            while (c.moveToNext()) {
                list.add(
                    TransactionEntity(
                        id = c.getString(idIdx),
                        title = c.getString(titleIdx),
                        amount = c.getDouble(amountIdx),
                        type = c.getString(typeIdx),
                        category = c.getString(categoryIdx),
                        date = c.getString(dateIdx)
                    )
                )
            }
        }
        return list
    }

    fun deleteById(id: String): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete("transactions", "id = ?", arrayOf(id))
        return rows > 0
    }

    fun getCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM transactions", null)
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
