package com.example.nlistr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends ComponentActivity {
    public static final String BACKSLASH = "/";
    public static final String MOD_INDEX_SHARED_PREF_ID = "modificationIndex";
    public static final String DB_HASH_SHARED_PREF_ID = "dbHash";
    public static final String EMPTY_HASH = "";

    private List<Contact> contacts;
    private AppDatabase db;
    private TableLayout tl;
    private boolean sortOnName;
    private String query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tl = findViewById(R.id.tableLayout);

        generateDatabase();

        // populate the contacts table with all contacts
        sortOnName = true;
        query = getString(R.string.empty_query);
        getContactsFromDB();

        OverrideDatabaseModifier modifier = new OverrideDatabaseModifier(this,
                db,
                getPreferences(Context.MODE_PRIVATE).getString(DB_HASH_SHARED_PREF_ID, EMPTY_HASH));

        Executors.newSingleThreadExecutor()
                .execute(modifier::modifyDatabase);

        addListeners(db.contactDao());
    }

    @Override
    protected void onPause() {
        super.onPause();
        EditText searchBar = findViewById(R.id.editTextTextPersonName);
        searchBar.setText(getString(R.string.empty_query));
        searchBar.clearFocus();
    }

    private void getContactsFromDB() {
        db.contactDao()
                .getContactsByWideSearch(query, sortOnName)
                .observe(this, this::onChanged);
    }

    private void generateDatabase() {
        RoomDatabase.Builder<AppDatabase> dbBuilder = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class,
                getString(R.string.room_contacts_db_id));
        // check whether DB has already been initialized, if not, initialize now
        db = getApplicationContext().getDatabasePath(getString(R.string.room_contacts_db_id)).exists()
                ? dbBuilder.build()
                : dbBuilder.createFromAsset(getString(R.string.pre_room_db_assets_path)).build();
    }

    private void addListeners(ContactDao contactDao) {
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
                    query = getString(R.string.empty_query);
                    ((EditText) findViewById(R.id.editTextTextPersonName)).setText(R.string.empty_query);
                } else {
                    contactDao
                            .getContactsByWideSearch(query, sortOnName)
                            .observe(MainActivity.this, MainActivity.this::onChanged);
                }
            }
        });
        findViewById(R.id.imageView).setOnLongClickListener(view -> {
            SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
            editor.putInt(MainActivity.MOD_INDEX_SHARED_PREF_ID, 0);
            editor.apply();
            return true;
        });
    }

    public void toastMessage(String message) {
        runOnUiThread(() -> Toast.makeText(
                getApplicationContext(),
                message,
                Toast.LENGTH_SHORT
                ).show()
        );
    }

    private void onChanged(List<Contact> contacts) {
        this.contacts = contacts;
        fillContactsView();
    }

    @SuppressLint("InflateParams")
    private void fillContactsView() {
        tl.removeAllViews();
        LayoutInflater li = LayoutInflater.from(getApplicationContext());
        for (int row = 0; row < contacts.size(); row++) {
            // generate a new view from the custom TableRow template
            tl.addView(li.inflate(R.layout.contacts_row, null));
            applyNewContactRowValues(row, contacts.get(row));
        }
    }

    private void applyNewContactRowValues(int row, Contact c) {
        String[] rowValues = getCleanContactDetails(c);
        for (int j = 0; j < rowValues.length; j++) {
            ((TextView) ((TableRow) tl.getChildAt(row))
                    .getChildAt(j))
                    .setText(rowValues[j]);
        }
    }

    @NonNull
    private String[] getCleanContactDetails(Contact c) {
        return new String[]{c.phoneNumber.substring(0,
                c.phoneNumber.contains(BACKSLASH) ? c.phoneNumber.indexOf(BACKSLASH)
                        : c.phoneNumber.length()),
                c.name.replace(getString(R.string.force_order_on_identical_names_symbol), getString(R.string.force_order_space_replacement)),
                c.roomNumber.equals(getString(R.string.empty_phone_number_in_db)) || c.roomNumber.equals(getString(R.string.empty_query)) ? "N/A" : c.roomNumber
        };
    }

    public void roomNumberTextView_onClick(View view) {
        sortOnName = false;
        getContactsFromDB();
    }

    public void nameTextView_onClick(View view) {
        sortOnName = true;
        getContactsFromDB();
    }

    public void imageView_onClick(View view) {
        Snackbar.make(findViewById(R.id.constraintLayout),
                getString(R.string.snack_bar_credits_text),
                Snackbar.LENGTH_LONG
        ).show();
    }

}