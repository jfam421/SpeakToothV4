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
        //Check if the bluetooth is working
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        } else {
            //Otherwise it will start function paired devices
            pairedDevices();
        }

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
        //Broadcast receiver to check state of bluetooth
        mBroadcastReceiver1 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        //If it`s off - make status offline
                        status_text_main.setText("Status: offline");
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        //If it`s on - make status online
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

    //Share apk function
    private void shareApplication() {
        //Get application info and get apk path
        ApplicationInfo app = getApplicationContext().getApplicationInfo();
        String filePath = app.sourceDir;
        //Make intent send
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");

        //Make uri with file provider
        Uri uri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", new File(filePath));
        //Make sure that all permissions was granted for uri apk
        List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            this.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        //Open share dialog and share the apk
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(intent, "Share app via"));


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
            //Another array list with devices addresses(MAC)
            ArrayList pairedDevicesToListString = new ArrayList();
            //Check if user has paired devices
            if (pairedDevicesToList.size() > 0) {
                //Check all devices in the list
                for (BluetoothDevice device : pairedDevicesToList) {
                    //User may has paired with different devices(it could be phone, computer or headphone)
                    //I had checked all devices class and I have found out that device class = 524 it has to be a phone
                    if (device.getBluetoothClass().getDeviceClass() == 524) {
                        pairedDevicesToListString.add(device.getAddress());
                        //Check if we have this paired device in our database with this address
                        boolean checker = searchDataDb(device.getAddress());
                        if (!checker) {
                            //If it is false we add it to our database
                            db_2.execSQL("INSERT INTO " + TABLE_NAME_2 + " Values ('" + device.getName() + "' ,'-368128','" + device.getAddress() + "');");
                        }
                    }
                }
                //Make adapter to the listview
                deviceListAdapter = new DeviceListAdapter(MainActivity.this, R.layout.mlist_item, listDevices);
            }
            //Check if we have data in the database of the paired users with there names
            Cursor cur_2 = db_2.rawQuery(SELECT_2, null);
            boolean isNotEmpty_2 = cur_2.moveToFirst();
            while (isNotEmpty_2) {
                //Add device to list from database
                listDevices.add(new DeviceItem(cur_2.getString(cur_2.getColumnIndex("Name")), cur_2.getInt(cur_2.getColumnIndex("Color")), cur_2.getString(cur_2.getColumnIndex("Mac"))));
                //Move to next row in database
                isNotEmpty_2 = cur_2.moveToNext();
            }
            //Check if the user had unpaired with some phone from paired devices list
            if (pairedDevicesToListString.size() < listDevices.size()) {
                //Check all list of devices
                for (DeviceItem item : listDevices) {
                    //If the application found out that user does not have this paired device in his list of paired devices
                    if (!pairedDevicesToListString.contains(item.getMac())) {
                        //Delete it from database
                        db_2.execSQL("DELETE FROM " + TABLE_NAME_2 + " Where Mac = '" + item.getMac() + "'");
                    }
                }
            }
        }
    }

    //Search in database function
    @SuppressLint("Range")
    public boolean searchDataDb(String mac) {
        //The function checks if user has some special data(MAC) in his database
        //Result of the searching(boolean variable)
        boolean toReturn = false;
        Cursor cur_2 = db_2.rawQuery(SELECT_2, null);
        boolean isNotEmpty_2 = cur_2.moveToFirst();
        while (isNotEmpty_2) {
            //It has find this data in the database
            if (cur_2.getString(cur_2.getColumnIndex("Mac")).equals(mac)) {
                //Data to return is true
                toReturn = true;
            }
            Log.d(TAG, "searchDataDb: " + mac + "----" + cur_2.getString(cur_2.getColumnIndex("Mac")));
            isNotEmpty_2 = cur_2.moveToNext();
        }
        //Otherwise it will return false
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
        //Unregister broadcast receiver if the application is destroyed
        unregisterReceiver(mBroadcastReceiver1);
        super.onDestroy();
    }
}