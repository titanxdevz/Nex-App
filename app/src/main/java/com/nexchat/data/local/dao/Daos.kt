package com.nexchat.data.local.dao

import androidx.room.*
import com.nexchat.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<UserEntity>)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE archivedAt IS NULL ORDER BY pinnedAt DESC NULLS LAST, updatedAt DESC")
    fun observeActive(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE archivedAt IS NOT NULL ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: String): Flow<ConversationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE conversations SET unreadCount = :count WHERE id = :id")
    suspend fun updateUnreadCount(id: String, count: Int)

    @Query("UPDATE conversations SET lastMessageId = :msgId, lastMessageContent = :content, lastMessageSenderId = :senderId, lastMessageCreatedAt = :createdAt, lastMessageType = :type, updatedAt = :createdAt WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, msgId: String, content: String, senderId: String, createdAt: String, type: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt ASC")
    fun observeMessages(convId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY createdAt ASC")
    suspend fun getMessages(convId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteByConversation(convId: String)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE messages SET starred = :starred WHERE id = :id")
    suspend fun updateStarred(id: String, starred: Boolean)

    @Query("UPDATE messages SET pinnedAt = :pinnedAt, pinnedById = :pinnedById WHERE id = :id")
    suspend fun updatePinned(id: String, pinnedAt: String?, pinnedById: String?)

    @Query("UPDATE messages SET content = :content, editedAt = :editedAt WHERE id = :id")
    suspend fun updateContent(id: String, content: String, editedAt: String)

    @Query("SELECT * FROM messages WHERE status = 'PENDING'")
    suspend fun getPendingMessages(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :convId")
    suspend fun countByConversation(convId: String): Int
}

@Dao
interface ParticipantDao {
    @Query("SELECT * FROM participants WHERE conversationId = :convId")
    suspend fun getByConversation(convId: String): List<ParticipantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(participants: List<ParticipantEntity>)

    @Query("DELETE FROM participants WHERE conversationId = :convId")
    suspend fun deleteByConversation(convId: String)
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY displayName ASC")
    fun observeAll(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(friends: List<FriendEntity>)

    @Query("DELETE FROM friends")
    suspend fun clearAll()
}

@Dao
interface CallDao {
    @Query("SELECT * FROM call_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CallRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(calls: List<CallRecordEntity>)

    @Query("DELETE FROM call_records")
    suspend fun clearAll()
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE expiresAt > :now ORDER BY createdAt DESC")
    fun observeActive(now: String = java.time.Instant.now().toString()): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stories: List<StoryEntity>)

    @Query("DELETE FROM stories WHERE expiresAt <= :now")
    suspend fun deleteExpired(now: String = java.time.Instant.now().toString())
}

@Dao
interface NotificationQueueDao {
    @Query("SELECT * FROM notification_queue WHERE delivered = 0 ORDER BY timestamp ASC")
    suspend fun getPending(): List<NotificationQueueEntity>

    @Query("SELECT * FROM notification_queue WHERE batchKey = :batchKey AND delivered = 0")
    suspend fun getByBatchKey(batchKey: String): List<NotificationQueueEntity>

    @Insert
    suspend fun insert(item: NotificationQueueEntity)

    @Insert
    suspend fun insertAll(items: List<NotificationQueueEntity>)

    @Query("UPDATE notification_queue SET delivered = 1 WHERE id = :id")
    suspend fun markDelivered(id: Long)

    @Query("UPDATE notification_queue SET delivered = 1 WHERE batchKey = :batchKey")
    suspend fun markBatchDelivered(batchKey: String)

    @Query("DELETE FROM notification_queue WHERE delivered = 1 AND timestamp < :before")
    suspend fun cleanupDelivered(before: Long = System.currentTimeMillis() - 3600000)

    @Query("DELETE FROM notification_queue")
    suspend fun clearAll()
}
