package com.example.speaktoothv4;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
//fix send apk
//make design 

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Bug";
    //Bluetooth Adapter in the phone
    private static BluetoothAdapter bluetoothAdapter;
    //Set of Bluetooth devices and selected device
    private BluetoothDevice selectedDevice = null;
    public Set<BluetoothDevice> pairedDevicesToList;
    //Adapter of the recycle view
    public ArrayList<DeviceItem> listDevices;
    public DeviceListAdapter deviceListAdapter;
    //Database of the user
    public SQLiteDatabase db, db_2;
    String TABLE_NAME_2 = "ListUsers";
    String DATABASE_CREATE_2 = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_2 + "(Name VARCHAR, Color INTEGER, Mac VARCHAR);";
    String SELECT_2 = "SELECT Name, Color, Mac FROM " + TABLE_NAME_2;
    //View elements of the activity
    private ListView devices;
    public View square;
    public TextView LName, Name, status_text_main;
    public ImageButton ButtSettings, icon_share;
    public SwipeRefreshLayout swiper;
    private BroadcastReceiver mBroadcastReceiver1;

    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Disable dark mode of the application if the user has turned it on
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Find view elements of the activity by those id
        swiper = findViewById(R.id.swipeRefresh);
        icon_share = findViewById(R.id.icon_share);
        listDevices = new ArrayList<>();
        status_text_main = findViewById(R.id.status_text_main);
        ButtSettings = findViewById(R.id.change_settings_main);
        square = findViewById(R.id.left_icon_main);
        LName = findViewById(R.id.icon_letter_main);
        Name = findViewById(R.id.icon_name_main);
        devices = findViewById(R.id.deviceList);
        //Get Bluetooth adapter of the phone
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        db_2 = openOrCreateDatabase("DataUser", MODE_PRIVATE, null);
        db_2.execSQL(DATABASE_CREATE_2);
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        } else {

            pairedDevices();
        }
        //Setup adapter for the listview


        //Setup adapter for the listview
        devices.setAdapter(deviceListAdapter);

        //Activity has refresh swiper
        //so that if you swipe listview down
        //the listview will be refreshed
        swiper.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                listDevices.clear();
                pairedDevices();
                //Setup adapter for the listview
                devices.setAdapter(deviceListAdapter);
                //Disable refreshing after user left his finger of the screen
                swiper.setRefreshing(false);
            }
        });

        mBroadcastReceiver1 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        status_text_main.setText("Status: offline");
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        status_text_main.setText("Status: online");
                    }

                }

            }

        };
        //Setup filter for broadcast receiver and register it
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, filter1);

        //Setup on item click listener and goes to chat with selected device
        devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Gets pressed item
                DeviceItem deviceItem = (DeviceItem) adapterView.getItemAtPosition(i);
                String deviceCh = deviceItem.getMac();
                //Iterating whole set of paired devices
                //And sets selected item as selected device
                for (BluetoothDevice device : pairedDevicesToList) {
                    if (deviceCh.equals(device.getAddress())) {
                        selectedDevice = device;
                        //Break loop if device was found
                        break;
                    }
                }
                //If bluetooth enabled it goes to chat activity
                if (bluetoothAdapter.isEnabled()) {
                    Intent intent = new Intent(MainActivity.this, ChatPattern.class);
                    intent.putExtra("deviceToConnect", selectedDevice);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);
                }
                //Otherwise it will make toast
                else {
                    Toast toast = Toast.makeText(MainActivity.this, "You have to turn on the Bluetooth", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

        //Setup listener on settings button in toolbar
        ButtSettings.setOnClickListener(this);
        icon_share.setOnClickListener(this);
        setupDB();

    }

    private void shareApplication() {
        ApplicationInfo app = getApplicationContext().getApplicationInfo();
        String filePath = app.sourceDir;


        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");


        // Append file and send Intent
        File originalApk = new File(filePath);

        try {
            //Make new directory in new location
            File tempFile = new File(getExternalCacheDir() + "/ExtractedApk");
            //If directory doesn't exists create new
            if (!tempFile.isDirectory())
                if (!tempFile.mkdirs())
                    return;
            //Get application's name and convert to lowercase
            tempFile = new File(tempFile.getPath() + "/" + getString(app.labelRes).replace(" ", "").toLowerCase() + ".apk");
            //If file doesn't exists create new
            if (!tempFile.exists()) {
                if (!tempFile.createNewFile()) {
                    return;
                }
            }
            //Copy file to new location
            InputStream in = new FileInputStream(originalApk);
            OutputStream out = new FileOutputStream(tempFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            Uri uri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", tempFile);
            List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                this.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            //Open share dialog
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(intent, "Share app via"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("Range")
    public void setupDB() {
        //Setup database
        String DB_NAME = "DataUser";
        String TABLE_NAME = "Data";
        String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(Name VARCHAR, Color INTEGER);";
        db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        //Creates table in the database
        db.execSQL(DATABASE_CREATE);
        //Select data from database
        String SELECT = "SELECT Name, Color FROM " + TABLE_NAME;
        Cursor cur = db.rawQuery(SELECT, null);
        boolean isNotEmpty = cur.moveToFirst();
        if (cur != null) {
            if (isNotEmpty) {
                while (isNotEmpty) {
                    //If database is not empty - enter all data into view objects
                    square.setBackgroundColor(cur.getInt(cur.getColumnIndex("Color")));
                    String name = cur.getString(cur.getColumnIndex("Name"));
                    LName.setText(name);
                    Name.setText(name);
                    //Move to next row in database
                    isNotEmpty = cur.moveToNext();
                }
            } else {
                //If database is empty - insert new data in a table
                db.execSQL("INSERT INTO " + TABLE_NAME + " Values ('User' ,'-368128');");
            }

        }
    }


    @SuppressLint("Range")
    public void pairedDevices() {
        //Checks if Bluetooth enabled
        //Otherwise it will start intent to turn on Bluetooth
        listDevices.clear();
        if (!bluetoothAdapter.isEnabled()) {
            Toast toast = Toast.makeText(MainActivity.this, "You have to turn on the Bluetooth", Toast.LENGTH_LONG);
            toast.show();
        } else {
            //Get set of bounded devices and making new adaptor for a listview
            //Iterating
            pairedDevicesToList = bluetoothAdapter.getBondedDevices();
            ArrayList pairedDevicesToListString = new ArrayList();

            if (pairedDevicesToList.size() > 0) {
                for (BluetoothDevice device : pairedDevicesToList) {
                    if (device.getBluetoothClass().getDeviceClass() == 524) {
                        pairedDevicesToListString.add(device.getAddress());
                        boolean checker = searchDataDb(device.getAddress());
                        if (!checker) {
                            db_2.execSQL("INSERT INTO " + TABLE_NAME_2 + " Values ('" + device.getName() + "' ,'-368128','" + device.getAddress() + "');");
                        }
                    }
                }
                deviceListAdapter = new DeviceListAdapter(MainActivity.this, R.layout.mlist_item, listDevices);
            }

            Cursor cur_2 = db_2.rawQuery(SELECT_2, null);
            boolean isNotEmpty_2 = cur_2.moveToFirst();
            while (isNotEmpty_2) {
                listDevices.add(new DeviceItem(cur_2.getString(cur_2.getColumnIndex("Name")), cur_2.getInt(cur_2.getColumnIndex("Color")), cur_2.getString(cur_2.getColumnIndex("Mac"))));
                //Move to next row in database
                isNotEmpty_2 = cur_2.moveToNext();
            }
            if (pairedDevicesToListString.size() < listDevices.size()) {
                for (DeviceItem item : listDevices) {
                    if (!pairedDevicesToListString.contains(item.getMac())) {
                        db_2.execSQL("DELETE FROM " + TABLE_NAME_2 + " Where Mac = '" + item.getMac() + "'");
                    }
                }
            }
        }
    }


    @SuppressLint("Range")
    public boolean searchDataDb(String mac) {
        boolean toReturn = false;
        Cursor cur_2 = db_2.rawQuery(SELECT_2, null);
        boolean isNotEmpty_2 = cur_2.moveToFirst();
        while (isNotEmpty_2) {
            if (cur_2.getString(cur_2.getColumnIndex("Mac")).equals(mac)) {
                toReturn = true;
            }
            Log.d(TAG, "searchDataDb: " + mac + "----" + cur_2.getString(cur_2.getColumnIndex("Mac")));
            isNotEmpty_2 = cur_2.moveToNext();
        }
        return toReturn;
    }

    //Setup onClickListener for buttons
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.change_settings_main) {
            //Create another intent and goes to EditProfile activity
            Intent mainIntent = new Intent(MainActivity.this, EditProfile.class);
            MainActivity.this.startActivity(mainIntent);
            overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);
            //Setup popup menu for settings button
        } else if (view.getId() == R.id.icon_share) {
            shareApplication();
        }
    }

    @Override
    public void onBackPressed() {
        //Don`t do anything when back button pressed
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver1);
        super.onDestroy();
    }
}