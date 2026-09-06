package com.mahfazty.smart.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity)

    @Update
    suspend fun update(tx: TransactionEntity)

    @Delete
    suspend fun delete(tx: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY id")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings WHERE id = 1")
    fun observe(): Flow<SavingsEntity?>

    @Query("SELECT * FROM savings WHERE id = 1")
    suspend fun get(): SavingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(savings: SavingsEntity)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings")
    fun observeAll(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY id")
    fun observeAll(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients")
    suspend fun getAll(): List<ClientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: ClientEntity)

    @Delete
    suspend fun delete(client: ClientEntity)

    @Query("DELETE FROM clients")
    suspend fun deleteAll()
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE clientId = :clientId")
    fun observeByClient(clientId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    /** إصلاح A4/B6: جلب مباشر بالمعرف بدل تحميل كل الجدول — للاستخدام داخل المعاملات الذرّية */
    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM accounts WHERE clientId = :clientId")
    suspend fun deleteByClient(clientId: Long)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}

@Dao
interface OperationDao {
    @Query("SELECT * FROM operations WHERE accountId = :accountId ORDER BY date DESC")
    fun observeByAccount(accountId: Long): Flow<List<OperationEntity>>

    @Query("SELECT * FROM operations")
    fun observeAll(): Flow<List<OperationEntity>>

    @Query("SELECT * FROM operations")
    suspend fun getAll(): List<OperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: OperationEntity)

    @Delete
    suspend fun delete(op: OperationEntity)

    @Query("DELETE FROM operations WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)

    @Query("DELETE FROM operations")
    suspend fun deleteAll()
}

/** سجل التدقيق (إصلاح sec-6) — أحدث 100 سجل */
@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY ts DESC LIMIT 100")
    fun observeRecent(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AuditLogEntity)

    @Query("DELETE FROM audit_log")
    suspend fun deleteAll()
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY date DESC")
    fun observeAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers")
    suspend fun getAll(): List<TransferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: TransferEntity)

    @Query("DELETE FROM transfers")
    suspend fun deleteAll()
}
