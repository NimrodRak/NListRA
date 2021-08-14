package com.example.nlistra;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ContactDao {

    @Query("SELECT * FROM contacts " +
            "WHERE name LIKE '%' || :stringQuery || '%' "
            + "OR name LIKE '%' || REPLACE(:stringQuery, ' ', '-') || '%'"
            + "OR name LIKE '%' || REPLACE(:stringQuery, '-', ' ') || '%'"
            + "OR name LIKE '%' || REPLACE(:stringQuery, ' ', '· ') || '%'"
            + "OR room_number LIKE '%' || :stringQuery || '%' ")
    LiveData<List<Contact>> getContactsByWideSearch(String stringQuery);

    @Query("UPDATE contacts "
            + "SET name = :newName, hash = :newName || :roomNumber "
            + "WHERE hash = :oldName || :roomNumber")
    int updateContact(String oldName, String newName, String roomNumber);
}
