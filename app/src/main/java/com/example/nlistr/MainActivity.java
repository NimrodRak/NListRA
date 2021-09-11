package com.example.nlistr;

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

import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends ComponentActivity {
    private static final String BACKSLASH = "/";
    private List<Contact> contacts;
    private AppDatabase db;
    private int updatesIndex;
    private TableLayout tl;
    private final String[] UPDATE_ATTRIBUTES_TAG_NAMES = new String[]{"oldName", "newName", "roomNumber"};
    private boolean sortOnName;
    private String query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tl = findViewById(R.id.tableLayout);
        updatesIndex = getPreferences(Context.MODE_PRIVATE).getInt(getString(R.string.update_index_shared_preferences_id), 0);

        generateDatabase();

        // populate the contacts table with all contacts
        sortOnName = true;
        query = getString(R.string.empty_query);
        getContactsFromDB();

        Executors.newSingleThreadExecutor()
                .execute(this::updateContacts);

        addTextSearchListener(db.contactDao());
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

    private void addTextSearchListener(ContactDao contactDao) {
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
    }

    private void toastMessage(String message) {
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
        String[] rowValues = new String[]{
                getCleanPhoneNumber(c.phoneNumber),
                c.name.replace(getString(R.string.force_order_on_identical_names_symbol), getString(R.string.force_order_space_replacement)),
                getCleanRoomNumber(c)
        };
        for (int j = 0; j < rowValues.length; j++) {
            ((TextView) ((TableRow) tl.getChildAt(row))
                    .getChildAt(j))
                    .setText(rowValues[j]);
        }
    }

    @NonNull
    private String getCleanRoomNumber(Contact c) {
        return c.roomNumber.equals(getString(R.string.empty_phone_number_in_db)) || c.roomNumber.equals("") ? "N/A" : c.roomNumber;
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
                getString(R.string.snackbar_credits_text),
                Snackbar.LENGTH_LONG
        ).show();
    }

    private static String getCleanPhoneNumber(String phoneNumber) {
        return phoneNumber.substring(0,
                phoneNumber.contains(BACKSLASH)
                        ? phoneNumber.indexOf(BACKSLASH)
                        : phoneNumber.length());
    }

    public void updateContacts() {
        URL url;
        NodeList updates;
        try {
            // read in the remote XML file that includes entries of updates
            url = new URL(getString(R.string.updates_xml_url));
            updates = getUpdateNodes(url);
        } catch (Exception e) {
            toastMessage(getString(R.string.web_update_error_message));
            return;
        }
        boolean applySuccess = applyUpdatesToDatabase(updates);
        // if updates were applied successfully to the underlying DB, notify the user and update updatesIndex
        if (applySuccess) {
            updateUpdatesIndex(updates.getLength());
        }
    }

    private boolean applyUpdatesToDatabase(NodeList updateEntries) {
        // iterate over all new updates
        for (int i = updatesIndex; i < updateEntries.getLength(); i++) {
            NodeList updateAttributes = updateEntries.item(i).getChildNodes();
            // parse what the update is
            String[] updateDetails = getUpdateDetails(updateAttributes);
            // if a malfunction occurred during details parsing, return no success
            if (updateDetails == null) {
                return false;
            }
            // apply the update details to the underlying DB
            ContactDao contactDao = db.contactDao();
            contactDao.updateContact(updateDetails[0], updateDetails[1], updateDetails[2]);
        }
        return true;
    }

    private void updateUpdatesIndex(int updatesCount) {
        int updateDiff = updatesCount - updatesIndex;
        if (updateDiff > 0) {
            toastMessage(String.format(Locale.ENGLISH, getString(R.string.contacts_updated_format), updateDiff));
        }
        // apply new updatesIndex to sharedPreferences
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putInt(getString(R.string.update_index_shared_preferences_id), updatesCount);
        editor.apply();
    }

    private NodeList getUpdateNodes(URL url)
            throws IOException, SAXException, ParserConfigurationException {
        // get XML update entries from remote file
        return DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(url.openStream())
                .getElementsByTagName(getString(R.string.updateTagName));
    }

    private String[] getUpdateDetails(NodeList nl) {
        int UPDATE_ATTRIBUTES_COUNT = 3;
        String[] updateValues = new String[UPDATE_ATTRIBUTES_COUNT];
        // parse XML node attributes into more convenient data structure
        for (int j = 0; j < UPDATE_ATTRIBUTES_COUNT; j++) {
            if (nl.item(2 * j + 1).getNodeName().equals(UPDATE_ATTRIBUTES_TAG_NAMES[j])) {
                updateValues[j] = nl.item(2 * j + 1).getTextContent();
            } else {
                return null;
            }
        }
        return updateValues;
    }
}