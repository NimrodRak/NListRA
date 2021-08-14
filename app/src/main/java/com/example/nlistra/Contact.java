package com.example.nlistra;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class Contact {
    @ColumnInfo(name = "name")
    public String name = "No Name Given.";

    @ColumnInfo(name = "room_number")
    public String roomNumber = "No Room Given.";

    @ColumnInfo(name = "phone_number")
    public String phoneNumber = "No Phone Given.";

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "hash")
    public String hash;
}
