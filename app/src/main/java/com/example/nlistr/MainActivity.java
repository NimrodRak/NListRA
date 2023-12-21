package com.example.nlistr;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends ComponentActivity {
    public static final String MOD_INDEX_SHARED_PREF_ID = "modificationIndex";
    public static final String DB_HASH_SHARED_PREF_ID = "dbHash";
    public static final String EMPTY_HASH = "";
    public static final String EMPTY_QUERY = "";
    private static final String TEL_SCHEME = "tel";

    private AppDatabase db;
    private ListView lv;
    private ContactsAdapter adapter;
    private boolean sortOnName = true;
    private String query = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv = findViewById(R.id.listView);
        adapter = new ContactsAdapter(this, new ArrayList<>());
        lv.setAdapter(adapter);
        applyPhoneMode();

        generateDatabase();

        // populate the contacts table with all contacts
        getContactsFromDB();

        OverrideDatabaseModifier modifier = new OverrideDatabaseModifier(this,
                db,
                getPreferences(Context.MODE_PRIVATE).getString(DB_HASH_SHARED_PREF_ID, EMPTY_HASH));

        Executors.newSingleThreadExecutor()
                .execute(modifier::modifyDatabase);

        addListeners();
    }


    @Override
    protected void onPause() {
        super.onPause();
        EditText searchBar = findViewById(R.id.editTextTextPersonName);
        searchBar.setText(EMPTY_QUERY);
        searchBar.clearFocus();
    }

    private void getContactsFromDB() {
        db.contactDao()
                .getContactsByWideSearch(query, sortOnName)
                .observe(MainActivity.this, this::fillContactsView);
    }

    private void generateDatabase() {
        RoomDatabase.Builder<AppDatabase> dbBuilder = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class,
                getString(R.string.room_contacts_db_id));
        // check whether DB has already been initialized, if not, initialize now
        dbBuilder.addMigrations(new Migration(1, 3) {
            @Override
            public void migrate(@NonNull SupportSQLiteDatabase database) {
                database.execSQL("ALTER TABLE contacts"
                        + " ADD COLUMN cell_number VARCHAR(12)"
                        + " DEFAULT ''");
            }
        });
        db = getApplicationContext().getDatabasePath(getString(R.string.room_contacts_db_id)).exists()
                ? dbBuilder.build()
                : dbBuilder.createFromAsset(getString(R.string.pre_room_db_assets_path)).build();
    }

    private void addListeners() {
        ((EditText) findViewById(R.id.editTextTextPersonName)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // query the DB using the given search query and apply the changes to the contacts table
                query = charSequence.toString();
                if (!query.matches(getString(R.string.invalid_characters_pattern))) {
                    query = EMPTY_QUERY;
                    ((EditText) findViewById(R.id.editTextTextPersonName)).setText(EMPTY_QUERY);
                } else {
                    getContactsFromDB();
                }
            }
        });
        ((ListView) findViewById(R.id.listView)).setOnItemClickListener((adapterView, view, i, l) -> {
            boolean prompted = getPreferences(Context.MODE_PRIVATE)
                    .getBoolean(getString(R.string.did_prompt_user), false);
            if (!prompted) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this, R.style.Theme_MyApp_Dialog_Alert);
                builder.setTitle(getString(R.string.alert_dialog_title))
                        .setMessage(R.string.alert_dialog_message)
                        .setPositiveButton(R.string.ok, (dialog, id) -> {
                            MainActivity.this.getPreferences(Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean(getString(R.string.did_prompt_user), true)
                                    .apply();
                            launchCallIntent(i);
                        });
                builder.show();
            } else {
                launchCallIntent(i);
            }
        });
    }

    private void launchCallIntent(int i) {
        Contact contact = (Contact) lv.getItemAtPosition(i);

        boolean useCellular = getPreferences(Context.MODE_PRIVATE)
                .getBoolean(getString(R.string.should_use_cellular), false);

        if (useCellular && (contact.cellNumber == null || contact.cellNumber.equals(""))) {
            toastLongMessage(String.format(getString(R.string.cell_number_not_found), adapter.getCleanName(contact)));
            return;
        }

        String phone = useCellular ? contact.cellNumber : (getString(R.string.nofim_phone_prefix) + contact.phoneNumber);
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts(TEL_SCHEME, phone, null));
        startActivity(intent);
    }

    public void toastMessage(String message) {
        runOnUiThread(() -> Toast.makeText(
                        getApplicationContext(),
                        message,
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    public void toastLongMessage(String message) {
        runOnUiThread(() -> Toast.makeText(
                        getApplicationContext(),
                        message,
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private void fillContactsView(List<Contact> contacts) {
        adapter.clear();
        adapter.addAll(contacts);
    }

    public void roomNumberTextView_onClick(View view) {
        sortOnName = false;
        getContactsFromDB();
    }

    public void nameTextView_onClick(View view) {
        sortOnName = true;
        getContactsFromDB();
    }

    public void switch_onClick(View view) {
        getPreferences(Context.MODE_PRIVATE)
                .edit()
                .putBoolean(getString(R.string.should_use_cellular), ((SwitchCompat)view).isChecked())
                .apply();
        applyPhoneMode();
    }

    public void applyPhoneMode() {
        toastMessage(String.format("%s phone numbers will be used", getPreferences(Context.MODE_PRIVATE).getBoolean(getString(R.string.should_use_cellular), false) ? "Cellular" : "Home"));
    }
}
