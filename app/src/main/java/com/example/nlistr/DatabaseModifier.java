package com.example.nlistr;

public abstract class DatabaseModifier {
    public static final String FAILED_UPDATE_MESSAGE = "Failed while trying to update.";
    public static final String FORMATTED_TOAST_MESSAGE = "Updated %d contact(s)";

    protected final AppDatabase db;
    protected final MainActivity context;

    public DatabaseModifier(MainActivity context, AppDatabase db) {
        this.db = db;
        this.context = context;
    }
    public abstract void modifyDatabase();
}
