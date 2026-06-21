package com.nexchat.di

import android.content.Context
import androidx.room.Room
import com.nexchat.data.local.TokenStorage
import com.nexchat.data.local.dao.*
import com.nexchat.data.local.database.NexChatDatabase
import com.nexchat.data.remote.api.*
import com.nexchat.data.remote.interceptor.AuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                // Only log body in debug builds
                if (com.nexchat.BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.92lrcorps.xyz/api/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton fun provideAuthApi(r: Retrofit) = r.create(AuthApi::class.java)
    @Provides @Singleton fun provideUsersApi(r: Retrofit) = r.create(UsersApi::class.java)
    @Provides @Singleton fun provideConversationsApi(r: Retrofit) = r.create(ConversationsApi::class.java)
    @Provides @Singleton fun provideMessagesApi(r: Retrofit) = r.create(MessagesApi::class.java)
    @Provides @Singleton fun provideMediaApi(r: Retrofit) = r.create(MediaApi::class.java)
    @Provides @Singleton fun provideStoriesApi(r: Retrofit) = r.create(StoriesApi::class.java)
    @Provides @Singleton fun provideFriendsApi(r: Retrofit) = r.create(FriendsApi::class.java)
    @Provides @Singleton fun provideCallsApi(r: Retrofit) = r.create(CallsApi::class.java)
    @Provides @Singleton fun provideInvitesApi(r: Retrofit) = r.create(InvitesApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NexChatDatabase =
        Room.databaseBuilder(context, NexChatDatabase::class.java, NexChatDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: NexChatDatabase): UserDao = db.userDao()
    @Provides fun provideConversationDao(db: NexChatDatabase): ConversationDao = db.conversationDao()
    @Provides fun provideMessageDao(db: NexChatDatabase): MessageDao = db.messageDao()
    @Provides fun provideParticipantDao(db: NexChatDatabase): ParticipantDao = db.participantDao()
    @Provides fun provideFriendDao(db: NexChatDatabase): FriendDao = db.friendDao()
    @Provides fun provideCallDao(db: NexChatDatabase): CallDao = db.callDao()
    @Provides fun provideStoryDao(db: NexChatDatabase): StoryDao = db.storyDao()
    @Provides fun provideNotificationQueueDao(db: NexChatDatabase): NotificationQueueDao = db.notificationQueueDao()
}
