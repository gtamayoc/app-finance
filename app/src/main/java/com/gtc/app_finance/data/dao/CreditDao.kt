package com.gtc.app_finance.data.dao

import android.content.ContentValues
import android.database.Cursor
import com.gtc.app_finance.data.database.TursoDatabaseHelper
import com.gtc.app_finance.data.entity.CreditEntity

class CreditDao(private val dbHelper: TursoDatabaseHelper) {

    fun insert(credit: CreditEntity): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", credit.id)
            put("title", credit.title)
            put("total_amount", credit.totalAmount)
            put("remaining_amount", credit.remainingAmount)
            put("due_date", credit.dueDate)
        }
        val result = db.insertWithOnConflict("credits", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        return result != -1L
    }

    fun getAll(): List<CreditEntity> {
        val list = mutableListOf<CreditEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            "credits",
            null,
            null,
            null,
            null,
            null,
            "due_date ASC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val titleIdx = c.getColumnIndexOrThrow("title")
            val totalIdx = c.getColumnIndexOrThrow("total_amount")
            val remainingIdx = c.getColumnIndexOrThrow("remaining_amount")
            val dueDateIdx = c.getColumnIndexOrThrow("due_date")

            while (c.moveToNext()) {
                list.add(
                    CreditEntity(
                        id = c.getString(idIdx),
                        title = c.getString(titleIdx),
                        totalAmount = c.getDouble(totalIdx),
                        remainingAmount = c.getDouble(remainingIdx),
                        dueDate = c.getString(dueDateIdx)
                    )
                )
            }
        }
        return list
    }

    fun updateRemainingAmount(creditId: String, newRemaining: Double): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("remaining_amount", newRemaining)
        }
        val rows = db.update("credits", values, "id = ?", arrayOf(creditId))
        return rows > 0
    }

    fun deleteById(id: String): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete("credits", "id = ?", arrayOf(id))
        return rows > 0
    }

    fun deleteAll(): Int {
        val db = dbHelper.writableDatabase
        return db.delete("credits", null, null)
    }

    fun getAllIds(): Set<String> {
        val ids = mutableSetOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("credits", arrayOf("id"), null, null, null, null, null)
        cursor.use { c ->
            val idx = c.getColumnIndexOrThrow("id")
            while (c.moveToNext()) {
                ids.add(c.getString(idx))
            }
        }
        return ids
    }

    fun getCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM credits", null)
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
