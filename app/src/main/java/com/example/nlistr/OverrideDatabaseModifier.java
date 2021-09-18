package com.example.nlistr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class OverrideDatabaseModifier extends DatabaseModifier {
    public static final Charset WINDOWS_1255_CHARSET = Charset.forName("Windows-1255");
    public static final String EMPTY_LINE = "";
    public static final String MD5_ALGORITHM = "MD5";
    public static final String ILLEGAL_FORMAT_ERROR_MESSAGE = "Contact format breached on remote database.";
    public static final String SUCCESSFUL_UPDATE_MESSAGE = "Updated database.";
    public static final String REMOTE_ENCRYPTED_DATABASE_SERVER_URL = "https://raw.githubusercontent.com/NimrodRak/nlist-updates/main/EncryptedDB";
    public static final String REMOTE_AES_KEY = "c72635c6151640e02aaa4da4f7a4615a";
    public static final Pattern CONTACT_ROW_PATTERN = Pattern.compile("^(.*?)\\s+\\[X]\\s+(.*?)\\s+\\[Y]\\s+(.*?)$");
    public static final String AES_ECB_PKCS_CIPHER = "AES/ECB/PKCS5Padding";
    public static final String AES_CIPHER = "AES";
    public static final int HEXADECIMAL_RADIX = 16;

    private final String oldHash;

    public OverrideDatabaseModifier(MainActivity context, AppDatabase db, String oldHash) {
        super(context, db);
        this.oldHash = oldHash;
    }

    public void modifyDatabase() {
        try {
            MessageDigest sha = MessageDigest.getInstance(MD5_ALGORITHM);
            ArrayList<Contact> contacts = retrieveContactsFromServer(sha);
            String newHash = android.util.Base64.encodeToString(sha.digest(),
                    Base64.DEFAULT | Base64.NO_WRAP);

            if (!oldHash.equals(newHash)) {
                ContactDao contactDao = db.contactDao();
                contactDao.deleteAll();
                contactDao.insertAll(contacts);

                context.getPreferences(Context.MODE_PRIVATE)
                        .edit()
                        .putString(MainActivity.DB_HASH_SHARED_PREF_ID, newHash)
                        .apply();
                context.toastMessage(SUCCESSFUL_UPDATE_MESSAGE);
            }
        } catch (Exception e) {
            context.toastMessage(FAILED_UPDATE_MESSAGE);
        }
    }

    private ArrayList<Contact> retrieveContactsFromServer(MessageDigest md) throws IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        ArrayList<Contact> contacts = new ArrayList<>();
        InputStream remoteStream = new URL(REMOTE_ENCRYPTED_DATABASE_SERVER_URL)
                .openConnection()
                .getInputStream();

        byte[] keyBytes = getByteArrayKey();
        Key k1 = new SecretKeySpec(keyBytes, 0, keyBytes.length, AES_CIPHER);

        @SuppressLint("GetInstance") Cipher aes2 = Cipher.getInstance(AES_ECB_PKCS_CIPHER);
        aes2.init(Cipher.DECRYPT_MODE, k1);

        CipherInputStream in = new CipherInputStream(remoteStream, aes2);
        DigestInputStream din = new DigestInputStream(in, md);
        BufferedReader reader = new BufferedReader(new InputStreamReader(din, WINDOWS_1255_CHARSET));
        String line;
        while ((line = reader.readLine()) != null && !line.equals(EMPTY_LINE)) {
            Contact cur = parseContactRow(line);
            if (cur != null) {
                contacts.add(cur);
            } else {
                din.close();
                throw new IllegalArgumentException(ILLEGAL_FORMAT_ERROR_MESSAGE);
            }
        }
        din.close();
        return contacts;
    }

    @NonNull
    private byte[] getByteArrayKey() {
        byte[] keyBytes = new BigInteger(REMOTE_AES_KEY, HEXADECIMAL_RADIX).toByteArray();
        byte[] tmp = new byte[keyBytes.length - 1];
        System.arraycopy(keyBytes, 1, tmp, 0, tmp.length);
        return tmp;
    }

    private Contact parseContactRow(String line) {
        Matcher matcher = CONTACT_ROW_PATTERN.matcher(line);
        return matcher.find()
                ? new Contact(matcher.group(1), matcher.group(2), matcher.group(3))
                : null;
    }
}
