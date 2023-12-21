package com.example.nlistr;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ContactDao {

    @Query("SELECT * FROM contacts "
            + "WHERE (room_number <> \"0000\" OR :sortOnName)"
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
    void updateContact(String oldName, String newName, String roomNumber);

    @Query("INSERT INTO contacts VALUES (:name, :roomNumber, :phoneNumber, :name || :roomNumber, :cellNumber)")
    void insertContact(String name, String roomNumber, String phoneNumber, String cellNumber);

    @Query("DELETE FROM contacts"
            + " WHERE name =:name"
            + " AND room_number = :roomNumber"
            + " AND hash = :name || :roomNumber")
    void deleteContact(String name, String roomNumber);

    @Insert
    void insertAll(List<Contact> contacts);

    @Query("DELETE FROM contacts")
    void deleteAll();
}
