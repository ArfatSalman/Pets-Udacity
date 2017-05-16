package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;


/**
 * Created by Arfat Salman on 19-Apr-17.
 */

public class PetProvider extends ContentProvider {
    private PetDBHelper mDbHelper;

    private static final int PETS = 100;
    private static final int PET_ID = 101;

    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, "pets", PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, "pets/#", PET_ID);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new PetDBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch(match) {
            case PETS:
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetContract.PetEntry.TABLE_NAME,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unkownn URI"+uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, values);
            default:
                throw new IllegalArgumentException(" Unknown URI"+uri);
        }
    }

    private Uri insertPet(Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String name = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        }

        String breed = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_BREED);
        if(breed==null) {
            throw new IllegalArgumentException("Breed is null");
        }

        int weight = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight is negative");
        }

        int gender = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
        if (gender < 0 && gender > 2) {
            throw new IllegalArgumentException("Wrong gender");
        }

        long newRowId = db.insert(PetContract.PetEntry.TABLE_NAME, null, contentValues);
        if (newRowId == -1) {
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);

        return Uri.withAppendedPath(uri, String.valueOf(newRowId));
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch(match) {
            case PETS:
                return updatePet(uri, values, selection, selectionArgs);
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Unkown uri" + uri);
        }
    }

    private int updatePet(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String name = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        }

        String breed = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_BREED);
        if(breed==null) {
            throw new IllegalArgumentException("Breed is null");
        }

        int weight = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight is negative");
        }

        int gender = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
        if (gender < 0 && gender > 2) {
            throw new IllegalArgumentException("Wrong gender");
        }

        if (contentValues.size() == 0) {
            return 0;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return db.update(PetContract.PetEntry.TABLE_NAME, contentValues, selection, selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        switch (match) {
            case PETS:
                getContext().getContentResolver().notifyChange(uri, null);
                return db.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
            case PET_ID:
                getContext().getContentResolver().notifyChange(uri, null);
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return db.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Unkown uri" + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Uri not known "+ uri);
        }
    }
}
