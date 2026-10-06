package com.gtc.app_finance.data.dao

import android.content.ContentValues
import android.database.Cursor
import com.gtc.app_finance.data.database.TursoDatabaseHelper
import com.gtc.app_finance.data.entity.PaymentEntity

class PaymentDao(private val dbHelper: TursoDatabaseHelper) {

    fun insert(payment: PaymentEntity): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", payment.id)
            put("credit_id", payment.creditId)
            put("amount", payment.amount)
            put("date", payment.date)
        }
        val result = db.insertWithOnConflict("payments", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        return result != -1L
    }

    fun getPaymentsForCredit(creditId: String): List<PaymentEntity> {
        val list = mutableListOf<PaymentEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            "payments",
            null,
            "credit_id = ?",
            arrayOf(creditId),
            null,
            null,
            "date DESC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val creditIdIdx = c.getColumnIndexOrThrow("credit_id")
            val amountIdx = c.getColumnIndexOrThrow("amount")
            val dateIdx = c.getColumnIndexOrThrow("date")

            while (c.moveToNext()) {
                list.add(
                    PaymentEntity(
                        id = c.getString(idIdx),
                        creditId = c.getString(creditIdIdx),
                        amount = c.getDouble(amountIdx),
                        date = c.getString(dateIdx)
                    )
                )
            }
        }
        return list
    }

    fun getAll(): List<PaymentEntity> {
        val list = mutableListOf<PaymentEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query("payments", null, null, null, null, null, "date DESC")
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val creditIdIdx = c.getColumnIndexOrThrow("credit_id")
            val amountIdx = c.getColumnIndexOrThrow("amount")
            val dateIdx = c.getColumnIndexOrThrow("date")

            while (c.moveToNext()) {
                list.add(
                    PaymentEntity(
                        id = c.getString(idIdx),
                        creditId = c.getString(creditIdIdx),
                        amount = c.getDouble(amountIdx),
                        date = c.getString(dateIdx)
                    )
                )
            }
        }
        return list
    }

    fun deleteById(id: String): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete("payments", "id = ?", arrayOf(id))
        return rows > 0
    }

    fun deleteByCreditId(creditId: String): Int {
        val db = dbHelper.writableDatabase
        return db.delete("payments", "credit_id = ?", arrayOf(creditId))
    }

    fun deleteAll(): Int {
        val db = dbHelper.writableDatabase
        return db.delete("payments", null, null)
    }

    fun getAllIds(): Set<String> {
        val ids = mutableSetOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("payments", arrayOf("id"), null, null, null, null, null)
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
        val cursor = db.rawQuery("SELECT COUNT(*) FROM payments", null)
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
