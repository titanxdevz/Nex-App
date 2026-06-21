package com.nexchat.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nexchat.data.local.dao.*
import com.nexchat.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        ParticipantEntity::class,
        FriendEntity::class,
        CallRecordEntity::class,
        StoryEntity::class,
        NotificationQueueEntity::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NexChatDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun participantDao(): ParticipantDao
    abstract fun friendDao(): FriendDao
    abstract fun callDao(): CallDao
    abstract fun storyDao(): StoryDao
    abstract fun notificationQueueDao(): NotificationQueueDao

    companion object {
        const val DATABASE_NAME = "nexchat.db"
    }
}

/**
 * Type converters for Room.
 */
class Converters {
    @androidx.room.TypeConverter
    fun fromStringList(value: List<String>?): String = value?.joinToString(",") ?: ""

    @androidx.room.TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",")
}
