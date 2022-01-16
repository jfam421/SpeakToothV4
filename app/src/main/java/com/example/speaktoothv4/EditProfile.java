package com.example.speaktoothv4;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import yuku.ambilwarna.AmbilWarnaDialog;

public class EditProfile extends AppCompatActivity implements View.OnClickListener {
    //Database of the user
    private final String TABLE_NAME = "Data";
    public SQLiteDatabase db;
    //Default data variables for start
    public int defaultColor = -368128;
    public String name = "";
    public static String DefaultName = "User";
    //View elements of the activity
    public Button setColor;
    public View square;
    public EditText EditName;
    public Button save;
    public TextView LName;
    public String TAG = "Bug";
    public ImageButton goBack;
    //Object of the class user
    public UserData user;

    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Disable dark mode of the application if the user has turned it on
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        //Find view elements of the activity by those id
        square = findViewById(R.id.square);
        setColor = findViewById(R.id.buttonColor);
        save = findViewById(R.id.save);
        LName = findViewById(R.id.nameLetter);
        EditName = findViewById(R.id.edit_name);
        EditName.setMovementMethod(null);
        goBack = findViewById(R.id.go_back);
        goBack.setOnClickListener(this);
        save.setOnClickListener(this);
        setColor.setOnClickListener(this);


        //Check if data has changed in the row edit name
        EditName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //When it`s changed - setup view icon letter
                LName.setText(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        setupDB();


    }
    @SuppressLint("Range")
    public void setupDB() {
        //Select data from database and setup database
        String DB_NAME = "DataUser";
        db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        String SELECT = "SELECT Name, Color FROM " + TABLE_NAME;
        Cursor cur = db.rawQuery(SELECT, null);
        if (cur != null) {
            boolean isNotEmpty = cur.moveToFirst();
            while (isNotEmpty) {
                //If database is not empty - enter all data into view objects
                square.setBackgroundColor(cur.getInt(cur.getColumnIndex("Color")));
                defaultColor = cur.getInt(cur.getColumnIndex("Color"));
                name = cur.getString(cur.getColumnIndex("Name"));
                EditName.setText(cur.getString(cur.getColumnIndex("Name")));
                LName.setText(name);
                setColor.setBackgroundColor(cur.getInt(cur.getColumnIndex("Color")));
                //Move to next row in database
                isNotEmpty = cur.moveToNext();

            }

        }
    }

    //Opens dialog box with color chooser
    public void openColorPicker() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, defaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                Log.d(TAG, "onCreate: " + color);
                //Setup view objects and changes default color
                defaultColor = color;
                setColor.setBackgroundColor(color);
                square.setBackgroundColor(color);

            }
        });
        //Show color picker
        colorPicker.show();
    }

    //Setup on click listener
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save:
                //If edit name do not empty it will add to database an information
                //And create new object of the class
                if (!EditName.getText().toString().matches("")) {
                    user = new UserData(EditName.getText().toString(), defaultColor);
                    db.execSQL("UPDATE " + TABLE_NAME + " SET Color= '" + user.getColor() + "'," + "Name='" + nameChecker(user.getName()) + "' " + " WHERE Name = '" + name + "'");
                    DefaultName = EditName.getText().toString();
                } else {
                    //Otherwise it will create default user and add him to the database
                    user = new UserData(DefaultName, defaultColor);
                    db.execSQL("UPDATE " + TABLE_NAME + " SET Color= '" + user.getColor() + "'," + "Name='" + nameChecker(user.getName()) + "' " + " WHERE Name = '" + name + "'");

                }
                //Creates new intent and moves to another activity
                Intent mainIntent = new Intent(EditProfile.this, MainActivity.class);
                EditProfile.this.startActivity(mainIntent);
                //Animation slide from one activity to another
                overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);
                break;
            case R.id.go_back:
                //Creates new intent and moves to another activity
                mainIntent = new Intent(EditProfile.this, MainActivity.class);
                EditProfile.this.startActivity(mainIntent);
                //Animation slide from one activity to another
                overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);
                break;

            case R.id.buttonColor:
                openColorPicker();
                break;
        }
    }

    public String nameChecker(String name){
        //There was one bug with this symbol("  '  ")
        //If we put this symbol in our database it will throw SQL exception
        //Because when we trying to do this SQL thinks that we are trying to open quotes
        //So that I decided to replace it by another symbol ("  `  ")
        //It doesn't make any difference in a view to users and it will work
        String checkedName;
        if(name.contains("'")){
            checkedName = name.replace("'","`");
            Log.d(TAG, "nameChecker: " + checkedName);
            return checkedName;
        }
        return name;
    }

    @Override
    public void onBackPressed() {
        Intent mainIntent = new Intent(EditProfile.this, MainActivity.class);
        EditProfile.this.startActivity(mainIntent);
        //Animation slide from one activity to another
        overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);

    }
}