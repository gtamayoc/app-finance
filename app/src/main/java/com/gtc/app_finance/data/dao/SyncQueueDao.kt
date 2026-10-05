package com.gtc.app_finance.data.dao

import android.content.ContentValues
import android.database.Cursor
import com.gtc.app_finance.data.database.TursoDatabaseHelper
import com.gtc.app_finance.data.entity.SyncQueueEntity

class SyncQueueDao(private val dbHelper: TursoDatabaseHelper) {

    fun insert(item: SyncQueueEntity): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("entity_type", item.entityType)
            put("entity_id", item.entityId)
            put("operation", item.operation)
            put("sql_command", item.sqlCommand)
            put("created_at", item.createdAt)
        }
        return db.insert("sync_queue", null, values)
    }

    fun getAll(): List<SyncQueueEntity> {
        val list = mutableListOf<SyncQueueEntity>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            "sync_queue",
            null,
            null,
            null,
            null,
            null,
            "id ASC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow("id")
            val typeIdx = c.getColumnIndexOrThrow("entity_type")
            val entityIdIdx = c.getColumnIndexOrThrow("entity_id")
            val opIdx = c.getColumnIndexOrThrow("operation")
            val sqlIdx = c.getColumnIndexOrThrow("sql_command")
            val createdIdx = c.getColumnIndexOrThrow("created_at")

            while (c.moveToNext()) {
                list.add(
                    SyncQueueEntity(
                        id = c.getLong(idIdx),
                        entityType = c.getString(typeIdx),
                        entityId = c.getString(entityIdIdx),
                        operation = c.getString(opIdx),
                        sqlCommand = c.getString(sqlIdx),
                        createdAt = c.getLong(createdIdx)
                    )
                )
            }
        }
        return list
    }

    fun deleteById(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete("sync_queue", "id = ?", arrayOf(id.toString())) > 0
    }

    fun getPendingEntityIds(entityType: String): Set<String> {
        val ids = mutableSetOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "sync_queue",
            arrayOf("entity_id"),
            "entity_type = ? AND operation != 'DELETE'",
            arrayOf(entityType),
            null,
            null,
            null
        )
        cursor.use { c ->
            val idx = c.getColumnIndexOrThrow("entity_id")
            while (c.moveToNext()) {
                ids.add(c.getString(idx))
            }
        }
        return ids
    }

    fun clear(): Int {
        val db = dbHelper.writableDatabase
        return db.delete("sync_queue", null, null)
    }

    fun getCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM sync_queue", null)
        return cursor.use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
