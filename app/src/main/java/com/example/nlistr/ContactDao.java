package com.example.nlistr;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ContactDao {

    @Query("SELECT * FROM contacts " +
            "WHERE (room_number <> \"0000\" OR :sortOnName)"
            + "AND (name <> \"\" OR NOT :sortOnName)"
            + "AND (name LIKE '%' || :stringQuery || '%' "
            + "OR name LIKE '%' || REPLACE(:stringQuery, ' ', '-') || '%'"
            + "OR name LIKE '%' || REPLACE(:stringQuery, '-', ' ') || '%'"
            + "OR name LIKE '%' || REPLACE(:stringQuery, ' ', '· ') || '%'"
            + "OR room_number LIKE '%' || :stringQuery || '%')"
            + "ORDER BY CASE :sortOnName WHEN 1 THEN name ELSE CAST(room_number AS INT) END")
    LiveData<List<Contact>> getContactsByWideSearch(String stringQuery, boolean sortOnName);

    @Query("UPDATE contacts "
            + "SET name = :newName, hash = :newName || :roomNumber "
            + "WHERE hash = :oldName || :roomNumber")
    int updateContact(String oldName, String newName, String roomNumber);
}
