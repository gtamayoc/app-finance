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

    /**
     * Reads all credits while computing remainingAmount dynamically from existing payments.
     * This guarantees that if a payment is deleted or orphan, the credit balance always
     * reflects the real mathematical truth (total_amount - sum(payments)).
     */
    fun getAll(): List<CreditEntity> {
        val list = mutableListOf<CreditEntity>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT 
                c.id,
                c.title,
                c.total_amount,
                MAX(0.0, c.total_amount - COALESCE(SUM(p.amount), 0.0)) AS computed_remaining,
                c.due_date
            FROM credits c
            LEFT JOIN payments p ON p.credit_id = c.id
            GROUP BY c.id, c.title, c.total_amount, c.due_date
            ORDER BY c.due_date ASC;
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val titleIdx = c.getColumnIndexOrThrow("title")
            val totalIdx = c.getColumnIndexOrThrow("total_amount")
            val remainingIdx = c.getColumnIndexOrThrow("computed_remaining")
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

    /**
     * Synchronizes the stored remaining_amount column in the credits table with
     * the actual sum of payments, healing any drift caused by manual edits or legacy states.
     */
    fun syncStoredRemainingAmounts() {
        try {
            val db = dbHelper.writableDatabase
            db.execSQL("""
                UPDATE credits
                SET remaining_amount = MAX(0.0, total_amount - (
                    SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE credit_id = credits.id
                ));
            """.trimIndent())
        } catch (e: Exception) {
            android.util.Log.w("CreditDao", "Error updating stored remaining_amount: ${e.message}")
        }
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
