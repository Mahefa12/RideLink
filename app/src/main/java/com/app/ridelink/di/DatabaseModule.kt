package com.app.ridelink.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.ridelink.auth.AuthenticationManager
import com.app.ridelink.auth.GoogleSignInManager
import com.app.ridelink.data.dao.UserDao
import com.app.ridelink.data.dao.MessageDao
import com.app.ridelink.data.dao.ConversationDao
import com.app.ridelink.data.dao.RideDao
import com.app.ridelink.data.database.RideLinkDatabase
import com.app.ridelink.data.database.DatabaseSeeder
import com.app.ridelink.data.repository.AuthRepository
import com.app.ridelink.data.repository.MessagingRepository
import com.app.ridelink.data.repository.RideRepository
import com.app.ridelink.location.LocationManager
import com.app.ridelink.ui.theme.ThemeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create conversations table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS conversations (
                    id TEXT NOT NULL PRIMARY KEY,
                    user1Id TEXT NOT NULL,
                    user2Id TEXT NOT NULL,
                    rideId TEXT,
                    lastMessage TEXT,
                    lastMessageTimestamp INTEGER NOT NULL,
                    lastMessageSenderId TEXT,
                    unreadCount INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(user1Id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(user2Id) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            
            // Create messages table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT NOT NULL PRIMARY KEY,
                    conversationId TEXT NOT NULL,
                    senderId TEXT NOT NULL,
                    receiverId TEXT NOT NULL,
                    content TEXT NOT NULL,
                    messageType TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    isRead INTEGER NOT NULL,
                    isDelivered INTEGER NOT NULL,
                    rideId TEXT,
                    FOREIGN KEY(senderId) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(receiverId) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            
            // Create indices
            database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_user1Id ON conversations(user1Id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_user2Id ON conversations(user2Id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_lastMessageTimestamp ON conversations(lastMessageTimestamp)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_rideId ON conversations(rideId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_senderId ON messages(senderId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_receiverId ON messages(receiverId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversationId ON messages(conversationId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_messages_timestamp ON messages(timestamp)")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create rides table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS rides (
                    id TEXT NOT NULL PRIMARY KEY,
                    driverId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    originAddress TEXT NOT NULL,
                    destinationAddress TEXT NOT NULL,
                    originLatitude REAL,
                    originLongitude REAL,
                    destinationLatitude REAL,
                    destinationLongitude REAL,
                    destinationState TEXT NOT NULL,
                    departureTime INTEGER NOT NULL,
                    availableSeats INTEGER NOT NULL,
                    pricePerSeat REAL NOT NULL,
                    vehicleType TEXT NOT NULL DEFAULT 'Car',
                    vehicleModel TEXT,
                    vehiclePlateNumber TEXT,
                    petsAllowed INTEGER NOT NULL DEFAULT 0,
                    smokingAllowed INTEGER NOT NULL DEFAULT 0,
                    recurringDays TEXT NOT NULL DEFAULT '',
                    isActive INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(driverId) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            
            // Create indices for rides table
            database.execSQL("CREATE INDEX IF NOT EXISTS index_rides_driverId ON rides(driverId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_rides_departureTime ON rides(departureTime)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_rides_originLatitude_originLongitude ON rides(originLatitude, originLongitude)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_rides_destinationLatitude_destinationLongitude ON rides(destinationLatitude, destinationLongitude)")
        }
    }

    @Provides
    @Singleton
    fun provideRideLinkDatabase(@ApplicationContext context: Context): RideLinkDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            RideLinkDatabase::class.java,
            "ridelink_database"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }

    @Provides
    fun provideUserDao(database: RideLinkDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideMessageDao(database: RideLinkDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun provideConversationDao(database: RideLinkDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideRideDao(database: RideLinkDatabase): RideDao {
        return database.rideDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideGoogleSignInManager(@ApplicationContext context: Context): GoogleSignInManager {
        return GoogleSignInManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        userDao: UserDao,
        googleSignInManager: GoogleSignInManager
    ): AuthRepository {
        return AuthRepository(firebaseAuth, firestore, userDao, googleSignInManager)
    }

    @Provides
    @Singleton
    fun provideAuthenticationManager(
        authRepository: AuthRepository
    ): AuthenticationManager {
        return AuthenticationManager(authRepository)
    }

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager {
        return LocationManager(context)
    }

    @Provides
    @Singleton
    fun provideMessagingRepository(
        messageDao: MessageDao,
        conversationDao: ConversationDao,
        userDao: UserDao
    ): MessagingRepository {
        return MessagingRepository(messageDao, conversationDao, userDao)
    }

    @Provides
    @Singleton
    fun provideThemeManager(@ApplicationContext context: Context): ThemeManager {
        return ThemeManager(context)
    }
    
    @Provides
    @Singleton
    fun provideRideRepository(
        rideDao: RideDao,
        userDao: UserDao
    ): RideRepository {
        return RideRepository(rideDao, userDao)
    }
    
    @Provides
    @Singleton
    fun provideDatabaseSeeder(
        userDao: UserDao,
        conversationDao: ConversationDao,
        messageDao: MessageDao
    ): DatabaseSeeder {
        return DatabaseSeeder(userDao, conversationDao, messageDao)
    }
}