package com.app.ridelink.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.app.ridelink.data.dao.UserDao
import com.app.ridelink.data.dao.MessageDao
import com.app.ridelink.data.dao.ConversationDao
import com.app.ridelink.data.dao.RideDao
import com.app.ridelink.data.model.User
import com.app.ridelink.data.model.Message
import com.app.ridelink.data.model.Conversation
import com.app.ridelink.data.model.Ride

@Database(
    entities = [User::class, Message::class, Conversation::class, Ride::class],
    version = 3,
    exportSchema = false
)
abstract class RideLinkDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun rideDao(): RideDao

    companion object {
        @Volatile
        private var INSTANCE: RideLinkDatabase? = null

        fun getDatabase(context: Context): RideLinkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RideLinkDatabase::class.java,
                    "ridelink_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}