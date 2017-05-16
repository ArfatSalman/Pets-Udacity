package com.example.android.pets;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.pets.data.PetContract;
import com.example.android.pets.data.PetContract.PetEntry;
import com.example.android.pets.data.PetDBHelper;

import java.security.KeyStore;


/**
 * Created by Arfat Salman on 10-Apr-17.
 */

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private Spinner mGenderSpinner;
    private int mGender = PetEntry.GENDER_UNKNOWN;
    private PetDBHelper mDBHelper;
    private Uri mCurrentUri;

    private EditText mName;
    private EditText mBreed;
    private EditText mWeight;

    private boolean mPetHasChnaged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mPetHasChnaged = true;
            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    private void insertPet() {

        EditText name_et = (EditText) findViewById(R.id.name_edittext);
        EditText breed_et = (EditText) findViewById(R.id.breed_edittext);
        EditText weight_et = (EditText) findViewById(R.id.weight_edittext);

        String name = name_et.getText().toString().trim();
        String breed = breed_et.getText().toString().trim();
        String weightString = weight_et.getText().toString().trim();
        int weight = 0;
        if (!weightString.isEmpty()){
            weight = Integer.parseInt(weightString);
        }


        if (name.isEmpty() && breed.isEmpty() && weightString.isEmpty() && mGender == PetEntry.GENDER_UNKNOWN) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(PetEntry.COLUMN_PET_NAME, name);
        values.put(PetEntry.COLUMN_PET_BREED, breed);
        values.put(PetEntry.COLUMN_PET_GENDER, mGender);
        values.put(PetEntry.COLUMN_PET_WEIGHT, weight);

        if (mCurrentUri == null) {
            Uri uri = getContentResolver().insert(PetContract.CONTENT_URI, values);

            if ( uri == null) {
                Toast toast = Toast.makeText(this, "Error while saving toast", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Toast.makeText(this, "Pet saved", Toast.LENGTH_SHORT).show();
            }
        } else {
            int rows = getContentResolver().update(mCurrentUri, values, null, null);

            if (rows == 0) {
                Toast.makeText(this, "Pet Did not get updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Pet updated", Toast.LENGTH_SHORT).show();
            }
        }

    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_save: {
                insertPet();
                finish();
                return true;
            }


            case R.id.action_delete: {
                showDeleteConfirmationDialog();
                break;
            }

            case android.R.id.home:
                if (!mPetHasChnaged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deletePet() {
        int rows = getContentResolver().delete(mCurrentUri, null, null);
        if (rows == 0) {
            Toast.makeText(this, "Pet not deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Pet deleted", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentUri == null ){
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor_activity);

        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);
        mName = (EditText) findViewById(R.id.name_edittext);
        mBreed = (EditText) findViewById(R.id.breed_edittext);
        mWeight = (EditText) findViewById(R.id.weight_edittext);

        mName.setOnTouchListener(mTouchListener);
        mBreed.setOnTouchListener(mTouchListener);
        mWeight.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

        setSpinner();
        mDBHelper = new PetDBHelper(this);

        Intent intent = getIntent();
        mCurrentUri = intent.getData();

        if (mCurrentUri == null) {
            setTitle("Add a Pet");
            invalidateOptionsMenu();
        } else {
            setTitle("Edit Pet");
            getLoaderManager().initLoader(1, null, this);
        }
    }

    @Override
    public void onBackPressed() {

        if (!mPetHasChnaged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonLostener  = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };

        showUnsavedChangesDialog(discardButtonLostener);
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Changes unsaved.");
        builder.setPositiveButton("Exit", discardButtonClickListener);
        builder.setNegativeButton("Keep Editing", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void setSpinner() {
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options,
                android.R.layout.simple_spinner_item);
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = parent.getItemAtPosition(position).toString();
                if (!selection.isEmpty()) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE;
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE;
                    } else
                        mGender = PetEntry.GENDER_UNKNOWN;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = PetEntry.GENDER_UNKNOWN;
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_GENDER,
                PetEntry.COLUMN_PET_WEIGHT
        };

        return new CursorLoader(this,
                mCurrentUri,
                projection,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data.moveToFirst()) {
            String name = data.getString(data.getColumnIndex(PetEntry.COLUMN_PET_NAME));
            String breed = data.getString(data.getColumnIndex(PetEntry.COLUMN_PET_BREED));
            String weight = data.getString(data.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT));

            mName.setText(name);
            mBreed.setText(breed);
            mWeight.setText(weight);

            switch(data.getInt(data.getColumnIndex(PetEntry.COLUMN_PET_GENDER))) {
                case PetEntry.GENDER_MALE:
                    mGenderSpinner.setSelection(PetEntry.GENDER_MALE);
                    break;
                case PetEntry.GENDER_FEMALE:
                    mGenderSpinner.setSelection(PetEntry.GENDER_FEMALE);
                    break;
                default:
                    mGenderSpinner.setSelection(PetEntry.GENDER_UNKNOWN);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mName.setText("");
        mBreed.setText("");
        mWeight.setText("");
        mGenderSpinner.setSelection(PetEntry.GENDER_UNKNOWN);
    }
}
