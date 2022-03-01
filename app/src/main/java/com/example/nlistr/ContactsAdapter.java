package com.example.nlistr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class ContactsAdapter extends ArrayAdapter<Contact> {
    public static final String BACKSLASH = "/";

    public ContactsAdapter(Context context, ArrayList<Contact> contacts) {
        super(context, 0, contacts);
    }

    private void applyNewContactRowValues(Contact c, View view) {
        String[] rowValues = getCleanContactDetails(c);
        for (int j = 0; j < rowValues.length; j++) {
            ((TextView) ((TableRow) view)
                    .getChildAt(j))
                    .setText(rowValues[j]);
        }
    }

    @NonNull
    private String[] getCleanContactDetails(Contact c) {
        return new String[]{c.phoneNumber.substring(0,
                c.phoneNumber.contains(BACKSLASH) ? c.phoneNumber.indexOf(BACKSLASH)
                        : c.phoneNumber.length()),
                c.name.replace(getContext().getString(R.string.force_order_on_identical_names_symbol), getContext().getString(R.string.force_order_space_replacement)),
                c.roomNumber.equals(getContext().getString(R.string.empty_phone_number_in_db)) || c.roomNumber.equals("") ? "N/A" : c.roomNumber
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Contact contact = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.contacts_row, parent, false);
        }
        applyNewContactRowValues(contact, convertView);
        return convertView;
    }
}
