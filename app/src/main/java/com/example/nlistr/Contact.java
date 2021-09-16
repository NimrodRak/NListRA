package com.example.nlistr;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class Contact {
    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "room_number")
    public String roomNumber;

    @ColumnInfo(name = "phone_number")
    public String phoneNumber;

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "hash")
    public String hash;

    public Contact(String roomNumber, String name, String phoneNumber) {
        this.roomNumber = roomNumber;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.hash = name + roomNumber;
    }
}
