package com.nrak.nlistr2;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
    entities = {Contact.class},
    version = 3,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ContactDao contactDao();
}
