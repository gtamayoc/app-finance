package com.gtc.app_finance.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TursoDatabaseHelper(
    context: Context,
    dbName: String = "finance_app.db",
    val dbVersion: Int = 4
) : SQLiteOpenHelper(
    context,
    dbName,
    null,
    dbVersion
) {

    companion object {
        const val CREATE_TABLE_TRANSACTIONS = """
            CREATE TABLE IF NOT EXISTS transactions (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                category TEXT NOT NULL,
                date TEXT NOT NULL
            );
        """

        const val CREATE_TABLE_CREDITS = """
            CREATE TABLE IF NOT EXISTS credits (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                total_amount REAL NOT NULL,
                remaining_amount REAL NOT NULL,
                due_date TEXT NOT NULL
            );
        """

        const val CREATE_TABLE_PAYMENTS = """
            CREATE TABLE IF NOT EXISTS payments (
                id TEXT PRIMARY KEY,
                credit_id TEXT NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL,
                FOREIGN KEY(credit_id) REFERENCES credits(id) ON DELETE CASCADE
            );
        """

        const val CREATE_TABLE_SYNC_QUEUE = """
            CREATE TABLE IF NOT EXISTS sync_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                entity_type TEXT NOT NULL,
                entity_id TEXT NOT NULL,
                operation TEXT NOT NULL,
                sql_command TEXT NOT NULL,
                created_at INTEGER NOT NULL
            );
        """

        // Full-cycle idempotent triggers for credits remaining_amount calculation
        const val CREATE_TRG_PAYMENT_INSERT_SQL = """
            CREATE TRIGGER IF NOT EXISTS trg_payment_after_insert
            AFTER INSERT ON payments
            FOR EACH ROW
            BEGIN
                UPDATE credits
                SET remaining_amount = MAX(0.0, total_amount - (
                    SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE credit_id = NEW.credit_id
                ))
                WHERE id = NEW.credit_id;
            END;
        """

        const val CREATE_TRG_PAYMENT_DELETE_SQL = """
            CREATE TRIGGER IF NOT EXISTS trg_payment_after_delete
            AFTER DELETE ON payments
            FOR EACH ROW
            BEGIN
                UPDATE credits
                SET remaining_amount = MAX(0.0, total_amount - (
                    SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE credit_id = OLD.credit_id
                ))
                WHERE id = OLD.credit_id;
            END;
        """

        const val CREATE_TRG_PAYMENT_UPDATE_SQL = """
            CREATE TRIGGER IF NOT EXISTS trg_payment_after_update
            AFTER UPDATE OF amount, credit_id ON payments
            FOR EACH ROW
            BEGIN
                UPDATE credits
                SET remaining_amount = MAX(0.0, total_amount - (
                    SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE credit_id = OLD.credit_id
                ))
                WHERE id = OLD.credit_id;

                UPDATE credits
                SET remaining_amount = MAX(0.0, total_amount - (
                    SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE credit_id = NEW.credit_id
                ))
                WHERE id = NEW.credit_id;
            END;
        """

        // Indexes for 10-year scalable date partitioning and foreign keys
        const val CREATE_IDX_TRANSACTIONS_DATE = "CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(date DESC);"
        const val CREATE_IDX_TRANSACTIONS_DATE_TYPE = "CREATE INDEX IF NOT EXISTS idx_transactions_date_type ON transactions(date DESC, type);"
        const val CREATE_IDX_PAYMENTS_CREDIT_ID = "CREATE INDEX IF NOT EXISTS idx_payments_credit_id ON payments(credit_id);"
        const val CREATE_IDX_PAYMENTS_DATE = "CREATE INDEX IF NOT EXISTS idx_payments_date ON payments(date DESC);"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_TRANSACTIONS.trimIndent())
        db.execSQL(CREATE_TABLE_CREDITS.trimIndent())
        db.execSQL(CREATE_TABLE_PAYMENTS.trimIndent())
        db.execSQL(CREATE_TABLE_SYNC_QUEUE.trimIndent())

        // Indexes
        db.execSQL(CREATE_IDX_TRANSACTIONS_DATE)
        db.execSQL(CREATE_IDX_TRANSACTIONS_DATE_TYPE)
        db.execSQL(CREATE_IDX_PAYMENTS_CREDIT_ID)
        db.execSQL(CREATE_IDX_PAYMENTS_DATE)

        // Triggers
        db.execSQL(CREATE_TRG_PAYMENT_INSERT_SQL.trimIndent())
        db.execSQL(CREATE_TRG_PAYMENT_DELETE_SQL.trimIndent())
        db.execSQL(CREATE_TRG_PAYMENT_UPDATE_SQL.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_SYNC_QUEUE.trimIndent())
        }
        if (oldVersion < 3) {
            db.execSQL(CREATE_IDX_TRANSACTIONS_DATE)
            db.execSQL(CREATE_IDX_PAYMENTS_CREDIT_ID)
        }
        if (oldVersion < 4) {
            db.execSQL(CREATE_IDX_TRANSACTIONS_DATE_TYPE)
            db.execSQL(CREATE_IDX_PAYMENTS_DATE)
            db.execSQL(CREATE_TRG_PAYMENT_INSERT_SQL.trimIndent())
            db.execSQL(CREATE_TRG_PAYMENT_DELETE_SQL.trimIndent())
            db.execSQL(CREATE_TRG_PAYMENT_UPDATE_SQL.trimIndent())
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }
}
