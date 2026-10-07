package com.gtc.app_finance.data.dao

import android.content.ContentValues
import android.database.Cursor
import com.gtc.app_finance.data.database.TursoDatabaseHelper
import com.gtc.app_finance.data.entity.TransactionEntity
import com.gtc.app_finance.domain.model.MonthlyAggregate

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

    fun getByDateRange(startDate: String, endDate: String): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            "transactions",
            null,
            "date >= ? AND date <= ?",
            arrayOf(startDate, endDate),
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

    fun getByMonth(yearMonth: String): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            "transactions",
            null,
            "date LIKE ?",
            arrayOf("$yearMonth%"),
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

    fun getMonthlyAggregates(): List<MonthlyAggregate> {
        val list = mutableListOf<MonthlyAggregate>()
        val db = dbHelper.readableDatabase
        val sql = """
            SELECT 
                substr(date, 1, 7) as ym,
                COALESCE(SUM(CASE WHEN LOWER(type) = 'income' THEN amount ELSE 0 END), 0.0) as income,
                COALESCE(SUM(CASE WHEN LOWER(type) = 'expense' THEN amount ELSE 0 END), 0.0) as expense
            FROM transactions
            GROUP BY substr(date, 1, 7)
            ORDER BY ym DESC;
        """.trimIndent()

        val cursor = db.rawQuery(sql, null)
        cursor.use { c ->
            val ymIdx = c.getColumnIndexOrThrow("ym")
            val incomeIdx = c.getColumnIndexOrThrow("income")
            val expenseIdx = c.getColumnIndexOrThrow("expense")

            while (c.moveToNext()) {
                val ym = c.getString(ymIdx)
                if (!ym.isNullOrBlank()) {
                    val inc = c.getDouble(incomeIdx)
                    val exp = c.getDouble(expenseIdx)
                    list.add(
                        MonthlyAggregate(
                            yearMonth = ym,
                            totalIncome = inc,
                            totalExpense = exp,
                            netBalance = inc - exp
                        )
                    )
                }
            }
        }
        return list
    }

    fun deleteById(id: String): Boolean {
        val db = dbHelper.writableDatabase
        val rows = db.delete("transactions", "id = ?", arrayOf(id))
        return rows > 0
    }

    fun deleteAll(): Int {
        val db = dbHelper.writableDatabase
        return db.delete("transactions", null, null)
    }

    fun getAllIds(): Set<String> {
        val ids = mutableSetOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.query("transactions", arrayOf("id"), null, null, null, null, null)
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
        val cursor = db.rawQuery("SELECT COUNT(*) FROM transactions", null)
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
