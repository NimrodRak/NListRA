package com.example.nlistr;

import android.content.Context;
import android.content.SharedPreferences;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class IncrementDatabaseModifier extends DatabaseModifier {

    private final int modificationIndex;
    public static final String ACTION_TAGNAME = "action";
    public static final String UPDATE_ACTION = "update";
    public static final String ROOM_NUMBER_TAGNAME = "roomNumber";
    public static final String NAME_TAGNAME = "name";
    public static final String NEW_NAME_TAGNAME = "newName";
    public static final String OLD_NAME_TAGNAME = "oldName";
    public static final String PHONE_NUMBER_TAGNAME = "phoneNumber";
    public static final String INSERT_ACTION = "insert";
    public static final String DELETE_ACTION = "delete";
    public static final String ENTRIES_SERVER_URL = "https://raw.githubusercontent.com/NimrodRak/nlist-updates/main/updates.xml";

    public IncrementDatabaseModifier(MainActivity appContext, AppDatabase appDb, int modIndex) {
        super(appContext, appDb);
        this.modificationIndex = modIndex;
    }

    public void modifyDatabase() {
        URL url;
        NodeList updates;
        try {
            // read in the remote XML file that includes entries of updates
            url = new URL(ENTRIES_SERVER_URL);
            updates = getEntryNodes(url);
            // if updates were applied successfully to the underlying DB, notify the user and update updatesIndex
            if (applyAllEntries(updates)) {
                updateModificationIndex(updates.getLength());
            } else {
                context.toastMessage(FAILED_UPDATE_MESSAGE);
            }
        } catch (Exception e) {
            context.toastMessage(FAILED_UPDATE_MESSAGE);
        }
    }

    private boolean applyAllEntries(NodeList entryList) {
        // iterate over all new updates
        for (int i = modificationIndex; i < entryList.getLength(); i++) {
            Node node = entryList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (!applyEntry(element)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean applyEntry(Element element) {
        ContactDao contactDao = db.contactDao();
        switch (element.getElementsByTagName(ACTION_TAGNAME).item(0).getTextContent()) {
            case UPDATE_ACTION:
                contactDao.updateContact(
                        element.getElementsByTagName(OLD_NAME_TAGNAME).item(0).getTextContent(),
                        element.getElementsByTagName(NEW_NAME_TAGNAME).item(0).getTextContent(),
                        element.getElementsByTagName(ROOM_NUMBER_TAGNAME).item(0).getTextContent()
                );
                break;
            case INSERT_ACTION:
                contactDao.insertContact(
                        element.getElementsByTagName(NAME_TAGNAME).item(0).getTextContent(),
                        element.getElementsByTagName(PHONE_NUMBER_TAGNAME).item(0).getTextContent(),
                        element.getElementsByTagName(ROOM_NUMBER_TAGNAME).item(0).getTextContent(),
                        ""
                );
                break;
            case DELETE_ACTION:
                contactDao.deleteContact(
                        element.getElementsByTagName(NAME_TAGNAME).item(0).getTextContent(),
                        element.getElementsByTagName(ROOM_NUMBER_TAGNAME).item(0).getTextContent()
                );
                break;
            default:
                return false;
        }
        return true;
    }


    private NodeList getEntryNodes(URL url)
            throws IOException, SAXException, ParserConfigurationException {
        // get XML update entries from remote file
        return DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(url.openStream())
                .getElementsByTagName(UPDATE_ACTION);
    }

    private void updateModificationIndex(int entryCount) {
        int modificationDiff = entryCount - this.modificationIndex;
        if (modificationDiff > 0) {
            context.toastMessage(String.format(Locale.ENGLISH, FORMATTED_TOAST_MESSAGE, modificationDiff));
        }
        // apply new updatesIndex to sharedPreferences
        SharedPreferences.Editor editor = context.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putInt(MainActivity.MOD_INDEX_SHARED_PREF_ID, entryCount);
        editor.apply();
    }
}
