package com.example.speaktoothv4;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class ChatPattern extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_TAKE_PHOTO = 1;
    //Database of the user and messages
    public String TABLE_NAME_2 = "Messages";
    public SQLiteDatabase db, db_2, db_3;
    public String TABLE_NAME_3;
    //Bluetooth Adapter in the phone
    public BluetoothAdapter bAdapter;
    private BluetoothDevice selectedDevice;
    //Thread objects
    public SendReceive sendReceive = null;
    public Client client;
    public Server server;
    //Data variables for start
    public String msgSendData;
    public int color = 0;
    public String MyName = "";
    private static final String TAG = "Data";
    private static final String APP_NAME = "SpeakTooth";
    private static final UUID MY_UUID = UUID.fromString("afbdd139-29f4-4e15-8846-3e23c3b0f48e");
    //Some other objects
    private Uri photoURI;
    public BroadcastReceiver mBroadcastReceiver1;
    public ArrayList<MessageModel> messagesList;
    //Notification service
    public NotificationService notificationService;
    //View elements of the activity
    public CustomAdapter messagesAdapter;
    public RecyclerView recyclerView;
    public FloatingActionButton sendBtn;
    public TextView userConnectedTo, icon_letter, status_text;
    public ImageView status;
    public ImageButton ButtSettings;
    public View icon;
    public EditText editTextMessage;
    public AlertDialog.Builder builder;
    public PopupMenu popup;
    //Handler states
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_CONNECTION_FAILED = 4;
    public static final int STATE_MESSAGE_RECEIVED = 5;


    @SuppressLint({"Range", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Disable dark mode of the application if the user has turned it on
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_pattern);
        //Find view elements of the activity by those id
        icon = findViewById(R.id.left_icon);
        messagesList = new ArrayList<>();
        status_text = findViewById(R.id.status_text);
        messagesAdapter = new CustomAdapter(this, messagesList);
        recyclerView = findViewById(R.id.rvChat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ButtSettings = findViewById(R.id.connectBtn);
        userConnectedTo = findViewById(R.id.icon_name);
        status = findViewById(R.id.status);
        icon_letter = findViewById(R.id.icon_letter);
        sendBtn = findViewById(R.id.fab);
        editTextMessage = findViewById(R.id.input);
        ButtSettings = findViewById(R.id.connectBtn);
        sendBtn.setOnClickListener(this);
        builder = new AlertDialog.Builder(this);
        ButtSettings.setOnClickListener(this);
        popup = new PopupMenu(ChatPattern.this, ButtSettings);
        popup.getMenuInflater().inflate(R.menu.connect_menu, popup.getMenu());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true);
        }
        //Get selected device from intent get extra
        selectedDevice = getIntent().getParcelableExtra("deviceToConnect");

        //First setup of the selected device in view layout
        if (selectedDevice != null) {
            if (selectedDevice.getName().length() > 12) {
                //If the name length bigger than 12 so it will right in the end 3 points("...")
                userConnectedTo.setText(selectedDevice.getName().substring(0, 12) + "...");
            } else {
                //Otherwise it will set the whole name of the device
                userConnectedTo.setText(selectedDevice.getName());
            }
            icon_letter.setText(selectedDevice.getName());

        } else {
            //If in the same time turn off bluetooth and go to the chat pattern activity
            //It will cause bug that we cannot get any information of the selected device
            //For that reason this else sector was made
            icon_letter.setText("Unknown");
            userConnectedTo.setText("Turn on bluetooth");
            Toast toast = Toast.makeText(ChatPattern.this, "Turn on bluetooth and reopen chat", Toast.LENGTH_LONG);
            toast.show();

        }
        setupDB();
        editTextMessage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (editTextMessage.getRight() - editTextMessage.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        if (bAdapter.isEnabled() && sendReceive != null) {
                            selectImage();
                        }else {
                            Toast toast = Toast.makeText(ChatPattern.this, "You have to connect to the user", Toast.LENGTH_LONG);
                            toast.show();
                        }
                        return true;
                    }
                    return false;
                }
                return false;
            }
        });

        //Get Bluetooth adapter of the phone
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        //Setup broadcast receiver to check the bluetooth state
        mBroadcastReceiver1 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        if (sendReceive == null) {
                            //If state of the bluetooth adapter is off and user has not connected yet
                            //It will start intent to turn on Bluetooth
                            Intent startBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivity(startBluetooth);
                        }
                    } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                        if (sendReceive != null) {
                            //If state of the bluetooth adapter is turning off and user has connected already
                            //It will start intent that returns to the last activity and close the connection
                            closeConnection();
                            Intent ChatIntent = new Intent(ChatPattern.this, MainActivity.class);
                            startActivity(ChatIntent);
                            overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);
                        }

                    }

                }

            }

        };
        //Setup filter for broadcast receiver and register it
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, filter1);
        //Checks if Bluetooth enabled
        //Otherwise it will start intent to turn on Bluetooth
        if (!bAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
        }
        //Check if selected device is not null
        if (selectedDevice != null) {
            //I have made special for the messages so that user could transfer some special information to other user
            //Example("@color@$nameOfTheUser$*User`sMacAddress*[0]")
            //Read more in SendReceive thread class
            msgSendData = "@" + color + "@$" + MyName + "$*" + selectedDevice.getAddress() + "*[0]";
            Log.d(TAG, "SendReceive: " + msgSendData);
        } else {
            msgSendData = "";
        }

        //If selected device is not null start server thread
        server = new Server();
        if (selectedDevice != null) {
            server.start();
        }
    }

    private void selectImage() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatPattern.this);
        builder.setTitle("Add Photo:");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

                    }
                } else if (options[item].equals("Choose from Gallery")) {
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (bAdapter.isEnabled()) {
            if (requestCode == 1 && resultCode == RESULT_OK) {
                Intent intent = new Intent();
                intent.setPackage("com.android.bluetooth");
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, photoURI);
                intent.setType("image/*");

                startActivity(intent);
            } else if (requestCode == 2 && resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                Intent intent = new Intent();
                intent.setPackage("com.android.bluetooth");
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, selectedImage);
                intent.setType("image/*");

                startActivity(intent);
            }

        } else {
            Toast toast = Toast.makeText(ChatPattern.this, "You have to turn on Bluetooth", Toast.LENGTH_LONG);
            toast.show();
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        return image;
    }

    @Override
    protected void onStop() {
        //If the user has left the application but did not close it
        //It will start Notification service
        //Every time user gets a message it will make notification
        ServiceConnection mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                //Bind notification service
                NotificationService.NotificationServiceBinder binder = (NotificationService.NotificationServiceBinder) service;
                notificationService = binder.getNotificationService();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }
        };
        //Start service
        Intent service = new Intent(ChatPattern.this, NotificationService.class);
        bindService(service, mConnection, Context.BIND_AUTO_CREATE);
        startService(service);
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        //If the application has been closed
        //It will close the connection thread
        unregisterReceiver(mBroadcastReceiver1);
        closeConnection();
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        //When user returns to the activity it will stop the service
        notificationService.serviceIsRunning = false;
        super.onRestart();
    }

    private void smoothScrollToBottom() {
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row and smooth scrolls to it
                recyclerView.smoothScrollToPosition(messagesList.size());
            }
        });
    }

    @SuppressLint("Range")
    public void setupDB() {
        //Setup database
        String DB_NAME = "DataUser";
        db = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        //Make other object db_2 so that I could work with another table
        db_2 = db;
        String TABLE_NAME = "Data";
        String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(Name VARCHAR, Color INTEGER);";
        db.execSQL(DATABASE_CREATE);
        String DATABASE_CREATE_2 = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_2 + "(Mac VARCHAR, Sender INTEGER, Text VARCHAR);";
        db_2.execSQL(DATABASE_CREATE_2);
        //Select data from database
        String SELECT = "SELECT Name, Color FROM " + TABLE_NAME;
        String SELECT_2 = "SELECT Mac, Sender, Text FROM " + TABLE_NAME_2;
        Cursor cur = db.rawQuery(SELECT, null);
        Cursor cur_2 = db_2.rawQuery(SELECT_2, null);

        if (cur != null) {
            boolean isNotEmpty = cur.moveToFirst();
            while (isNotEmpty) {
                //If database is not empty - enter all data into view objects
                color = cur.getInt(cur.getColumnIndex("Color"));
                MyName = cur.getString(cur.getColumnIndex("Name"));
                //Move to next row in database
                isNotEmpty = cur.moveToNext();
            }

        }

        if (cur_2 != null) {
            boolean isNotEmpty = cur_2.moveToFirst();
            while (isNotEmpty) {

                //If database is not empty - add all messages to ArrayList of MessageModel
                if (cur_2.getString(cur_2.getColumnIndex("Mac")).equals(selectedDevice.getAddress())) {
                    //Check type of the message (in or out)
                    //About messages read more in MessageModel class
                    if (cur_2.getInt(cur_2.getColumnIndex("Sender")) == 1) {
                        messagesList.add(new MessageModel(cur_2.getString(cur_2.getColumnIndex("Text")), 1));
                    } else {
                        messagesList.add(new MessageModel(cur_2.getString(cur_2.getColumnIndex("Text")), 0));
                    }
                }

                //Move to next row in database
                isNotEmpty = cur_2.moveToNext();
            }
            //Setup adapter to the recycle view
            recyclerView.setAdapter(messagesAdapter);
            smoothScrollToBottom();
        }
        db_3 = openOrCreateDatabase("DataUser", MODE_PRIVATE, null);
        TABLE_NAME_3 = "ListUsers";
        String DATABASE_CREATE_3 = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_3 + "(Name VARCHAR, Color INTEGER, Mac VARCHAR);";
        db_3.execSQL(DATABASE_CREATE_3);
        String SELECT_3 = "SELECT Name, Color, Mac FROM " + TABLE_NAME_3;
        Cursor cur_3 = db.rawQuery(SELECT_3, null);

        if (cur_3 != null) {
            boolean isNotEmpty_3 = cur_3.moveToFirst();
            while (isNotEmpty_3) {
                //If database is not empty - enter all data into view objects
                if(cur_3.getString(cur_3.getColumnIndex("Mac")).equals(selectedDevice.getAddress())){
                icon_letter.setText(cur_3.getString(cur_3.getColumnIndex("Name")));
                    if (selectedDevice.getName().length() > 12) {
                        //If the name length bigger than 12 so it will right in the end 3 points("...")
                        userConnectedTo.setText(cur_3.getString(cur_3.getColumnIndex("Name")).substring(0, 12) + "...");
                    } else {
                        //Otherwise it will set the whole name of the device
                        userConnectedTo.setText(cur_3.getString(cur_3.getColumnIndex("Name")));
                    }

                icon.setBackgroundColor(cur_3.getInt(cur_3.getColumnIndex("Color")));}
                //Move to next row in database
                isNotEmpty_3 = cur_3.moveToNext();
            }

        }

    }

    //Setup onClickListener for buttons
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.connectBtn:


                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    //Setup on item click listener for item in popup menu
                    @SuppressLint("Range")
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getTitle().toString()) {
                            case "Connect":

                                //On "Connect" item click
                                if (bAdapter.isEnabled()) {

                                    //Check if Bluetooth enabled and selected device is not null
                                    //Although it checks if the connection has already established
                                    if (sendReceive == null) {
                                        if (selectedDevice != null) {
                                            //Starts new client connect thread
                                            client = new Client(selectedDevice);
                                            client.start();



                                            //Change connection status icon
                                            status_text.setText("Connecting");
                                            status.setBackground(ContextCompat.getDrawable(ChatPattern.this, R.drawable.ic_connectig));

                                        }
                                    } else {
                                        //If connection is already established it will make toast
                                        Toast toast = Toast.makeText(ChatPattern.this, "You have already connected to the user", Toast.LENGTH_LONG);
                                        toast.show();
                                    }
                                } else {
                                    //If bluetooth is disabled it will make a toast
                                    Toast toast = Toast.makeText(ChatPattern.this, "You have to turn on Bluetooth", Toast.LENGTH_LONG);
                                    toast.show();
                                }
                                break;
                            case "Disconnect":

                                //On click "Go back" closes connection and goes to Main activity
                                closeConnection();
                                Intent ChatIntent = new Intent(ChatPattern.this, MainActivity.class);
                                startActivity(ChatIntent);
                                overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);


                                break;
                            case "Delete the chat":
                                //On click "Delete the chat" it delete whole chat from database and clear recycle view
                                db_2.execSQL("DELETE FROM " + TABLE_NAME_2 + " WHERE MAC = '" + selectedDevice.getAddress() + "'");
                                messagesList.clear();
                                recyclerView.setAdapter(messagesAdapter);
                                break;
                        }

                        return true;
                    }
                });

                popup.show();
                break;
            case R.id.fab:
                //On click send message button
                //Creates message string and add special code to the end of the message
                //So that when another device would check the type of message
                //To get more information about message types check SendReceive class
                String msgSend = editTextMessage.getText() + "[1]";
                String msgCleared = editTextMessage.getText().toString();
                if (!msgSend.equals("")) {
                    //Checks if edit message is not empty and if connection is established
                    if (sendReceive != null) {
                        //Make bytes from message send it to another user
                        sendReceive.write(msgSend.getBytes());
                        //Add message to the recycle view
                        messagesList.add(new MessageModel(msgCleared, 1));
                        recyclerView.setAdapter(messagesAdapter);
                        smoothScrollToBottom();
                        //Add message to database
                        db_2.execSQL("INSERT INTO " + TABLE_NAME_2 + " Values ('" + selectedDevice.getAddress() + "','" + 1 + "', '" + msgCleared + "');");
                    } else {
                        //Makes toast
                        Toast toast = Toast.makeText(ChatPattern.this, "You have to connect to the user", Toast.LENGTH_LONG);
                        toast.show();
                    }

                } else {
                    //Makes toast
                    Toast toast = Toast.makeText(ChatPattern.this, "You have to fill the message box", Toast.LENGTH_LONG);
                    toast.show();

                }
                //Clear edit text
                editTextMessage.getText().clear();
                break;
        }
    }


    //If back button pressed it close connection and goes to the last activity
    @Override
    public void onBackPressed() {
        closeConnection();
        Intent ChatIntent = new Intent(ChatPattern.this, MainActivity.class);
        startActivity(ChatIntent);
        overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);

    }


    public String messageChecker(String msg) {
        //There was one bug with this symbol("  '  ")
        //If we put this symbol in our database it will throw SQL exception
        //Because when we trying to do this SQL thinks that we are trying to open quotes
        //So that I decided to replace it by another symbol ("  `  ")
        //It doesn't make any difference in a view to users and it will work
        String checkedMsg;
        if (msg.contains("'")) {
            checkedMsg = msg.replace("'", "`");
            return checkedMsg;
        }
        return msg;
    }

    //Make handler object to get information and status from thread objects
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            //Handler has got message
            switch (msg.what) {
                case STATE_CONNECTING:
                    //Change status icon
                    status.setBackground(ContextCompat.getDrawable(ChatPattern.this, R.drawable.ic_disconnected));
                    status_text.setText("Disconnected");
                    break;
                case STATE_CONNECTED:
                    //Change status icon
                    status.setBackground(ContextCompat.getDrawable(ChatPattern.this, R.drawable.ic_connected));
                    status_text.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    //Change status icon
                    status.setBackground(ContextCompat.getDrawable(ChatPattern.this, R.drawable.ic_failed));
                    status_text.setText("Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    //When message is received
                    byte[] readBuffer = (byte[]) msg.obj;
                    //Get message part of the handler(object)
                    //Get text of the byte array
                    String tempMsg = new String(readBuffer, 0, msg.arg1);
                    //Get cleared message(without code in the end )
                    String msgCleared = messageChecker(tempMsg.substring(0, tempMsg.length() - 3));
                    //Get type of the message
                    String type = tempMsg.substring((tempMsg.length() - 3));
                    //If service is running it will send data to the service
                    if (notificationService != null && notificationService.serviceIsRunning) {
                        notificationService.sendNotification(selectedDevice.getName(), msgCleared);
                    }
                    //Make switch for type of the message
                    //Log.d(TAG, "handleMessage: " + type);
                    switch (type) {
                        case "[0]":
                            //[0] - data of the user connected to
                            String tmpColor = "";
                            int color = 0;
                            StringBuilder name = new StringBuilder();
                            //Iterate whole data string got
                            for (int i = 0; i < tempMsg.length(); i++) {
                                //Gets color of the user from the message
                                if (tempMsg.charAt(i) == '@') {
                                    for (int j = i + 1; j < tempMsg.length(); j++) {
                                        if (tempMsg.charAt(j) == '@') {
                                            //Make integer from string
                                            color = Integer.parseInt(tmpColor);
                                            //Move main loop to the "j" position
                                            i = j;
                                            //Stop for loop
                                            j = tempMsg.length();
                                        } else {
                                            //Append data to the color variable
                                            tmpColor += tempMsg.charAt(j);
                                        }
                                    }
                                }
                                //Gets name of the user from the message
                                if (tempMsg.charAt(i) == '$') {
                                    for (int b = i + 1; b < tempMsg.length(); b++) {
                                        if (tempMsg.charAt(b) == '$') {
                                            //Move main loop to the "b" position
                                            i = b;
                                            //Stop for loop
                                            b = tempMsg.length();
                                        } else {
                                            //Append data to the name variable
                                            name.append(tempMsg.charAt(b));
                                        }
                                    }
                                }

                            }
                            //Enter all data into view objects
                            icon_letter.setText(name.toString());
                            userConnectedTo.setText(name.toString());
                            icon.setBackgroundColor(color);

                            db_3.execSQL("UPDATE " + TABLE_NAME_3 + " SET Color= '" + color + "'," + "Name='" + name.toString() + "' " + " WHERE Mac = '" + selectedDevice.getAddress() + "'");

                            break;
                        case "[1]":
                            //[1] - normal message with text
                            //Add message to database and to the recycle view
                            db_2.execSQL("INSERT INTO " + TABLE_NAME_2 + " Values ('" + selectedDevice.getAddress() + "','" + 0 + "', '" + msgCleared + "');");
                            messagesList.add(new MessageModel(msgCleared, 0));
                            recyclerView.setAdapter(messagesAdapter);
                            smoothScrollToBottom();

                            break;
                        case "[2]":
                            //[2] - disconnect message that application to close connection
                            //Close connection and go to the Main activity
                            closeConnection();
                            Intent ChatIntent = new Intent(ChatPattern.this, MainActivity.class);
                            startActivity(ChatIntent);
                            overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom);
                            finish();
                            break;

                    }

                    break;

            }

            return true;
        }
    });

    public void closeConnection() {
        //Close and interrupt connection thread
        if (sendReceive != null) {
            sendReceive.close();
            sendReceive.interrupt();
        }
        //Close and interrupt server thread
        if (selectedDevice != null) {
            server.close();
            server.interrupt();
        }

    }

    private class Server extends Thread {
        private BluetoothServerSocket serverSocket;

        public Server() {
            try {
                //Make server socket listener(listen for connection in)
                serverSocket = bAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                //Change status icon
                status.setBackground(ContextCompat.getDrawable(ChatPattern.this, R.drawable.ic_disconnected));
            }
        }

        public void close() {
            try {
                //Close bluetooth socket
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            BluetoothSocket socket = null;
            while (socket == null) {
                try {
                    //Send message to the handler
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                    //Tries to make socket with device that want connect to the user
                    socket = serverSocket.accept();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (socket != null) {
                    //Send message to the handler
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);
                    //Start connected thread with another user
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    break;

                }
                if (this.isInterrupted()) {
                    //Break loop if it is interrupted
                    break;
                }
            }
        }

    }

    private class Client extends Thread {
        BluetoothDevice device;
        BluetoothSocket socket;

        public Client(BluetoothDevice device) {
            this.device = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                this.socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void run() {
            try {
                socket.connect();
                //Send message to the handler
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);
                //Start connected thread with another user
                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                //Send message to the handler
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }

    }

    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        @SuppressLint("Range")
        public SendReceive(BluetoothSocket socket) {

            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Get input and output stream
                tmpIn = bluetoothSocket.getInputStream();
                tmpOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            popup.getMenu().findItem(R.id.connect_item).setTitle("Disconnect");


            inputStream = tmpIn;
            outputStream = tmpOut;
            //[0] - Data of the user
            //[1] - Text message
            //[2] - Close connection code
            Log.d(TAG, "SendReceive: " + msgSendData);
            //If the application did not get an information about user`s bluetooth adapter
            //It will send default information about user
            if (msgSendData.length() < 32) {
                msgSendData = "@-2876902@$User$*48:01:C5:63:65:98*[0]";
            }
            //Send information when connection is established
            this.write(msgSendData.getBytes());

        }


        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                // Get the input and output streams
                try {
                    bytes = inputStream.read(buffer);
                    //Close connection if bluetooth is turned off
                    if (!bluetoothSocket.isConnected()) {
                        closeConnection();
                    }
                    //Make handler object from data input stream and send to the handler
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();

                }
                if (!bluetoothSocket.isConnected()) {
                    //Send message to the handler
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                }
                if (this.isInterrupted()) {
                    //Break loop if it is interrupted
                    break;
                }
            }


        }

        public void write(byte[] bytes) {
            try {
                //Send data to the remote device
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void close() {
            try {
                //Send information to another user that the connection has been destroyed
                this.write("[2]".getBytes());
                //Close bluetooth socket
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}