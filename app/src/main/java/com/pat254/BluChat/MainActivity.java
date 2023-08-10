package com.pat254.BluChat;

import static com.pat254.BluChat.ChatManager.STATE_CONNECTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.FileNotFoundException;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    //    DBHandler dbHandler;
//    static int count = 0;
//    ChatListAdapter mConversationArrayAdapter;
    public DrawerLayout drawerLayout;
    public NavigationView navigationView;
    public ActionBarDrawerToggle actionBarDrawerToggle;
    private ListView listMainChat;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ChatManager chatManager;
    private ImageView imageViewLoadImage;
    private EditText editTxtCreateMessage;
    private TextView textViewDescription;
    private TextView textViewBtVsDiscoverableStatus;
    private TextView btOnOff;
    private ArrayAdapter<String> adapterMainChat;

    private static final int ADD_PHOTO_REQUEST = 11; //request code to add photo from gallery
    private static final int MY_GALLERY_PERMISSION_CODE = 10; // request code to access EXTERNAL_STORAGE

    private static final int CAMERA_REQUEST = 9; //request code to set captured image
    private static final int MY_CAMERA_PERMISSION_CODE = 8; //request code to access CAMERA

    private final int LOCATION_PERMISSION_REQUEST = 7;  //request code to access FINE_LOCATION
    private final int SELECT_DEVICE = 5; // request code to send data from DeviceListActivity to MainActivity when a device is clicked

    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";

    private String connectedDevice;

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (message.arg1) {
                        case ChatManager.STATE_NONE:
//                            setState("Click \"+\" button to add a chatting partner");
//                            break;
                        case ChatManager.STATE_LISTEN:
                            setState("Not connected");
                            break;
                        case ChatManager.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case STATE_CONNECTED:
                            setState("Connected to: " + connectedDevice);
                            textViewBtVsDiscoverableStatus.setVisibility(View.GONE);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1 = (byte[]) message.obj;

                    // construct a string from the buffer
                    String outputBuffer = new String(buffer1);
//                    String outputBufferBitmap = String.valueOf(bitmap1);
                    adapterMainChat.add("Me: " + outputBuffer + "\n");
                    break;
                case MESSAGE_READ:
                    byte[] buffer2 = (byte[]) message.obj;

                    // construct a string from the valid bytes in the buffer
                    String inputBuffer = new String(buffer2, 0, message.arg1);
                    adapterMainChat.add("\n" + connectedDevice + ": " + inputBuffer + "\n");
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save and display the connected device's name
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, "Successfully Connected with " + connectedDevice, Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    });


    private void setState(CharSequence subTitle) {
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(subTitle);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        this.setTitle("Chat With Close Friends");
        drawerLayout = findViewById(R.id.drawer_layout_chat_screenId);

        inBluChat1();
        chatManager = new ChatManager(context, handler);
        hasBluetooth();

        if (!bluetoothAdapter.isEnabled()) {
            enableBluetoothOnCreate();
            btOnOff.setVisibility(View.VISIBLE);
            btOnOff.setText("Bluetooth: OFF\nDiscoverable: NO");
        } else if (bluetoothAdapter.isEnabled() && (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)) {
            Intent dIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            dIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
            startActivity(dIntent);
            btOnOff.setVisibility(View.VISIBLE);
            btOnOff.setText(R.string.str_bluetooth_on);
        } else if (bluetoothAdapter.isEnabled() && (bluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)) {
            btOnOff.setVisibility(View.VISIBLE);
            btOnOff.setText("Bluetooth: ON\nDiscoverable: YES");
            textViewBtVsDiscoverableStatus.setVisibility(View.VISIBLE);
            textViewBtVsDiscoverableStatus.setText("Bluetooth is ON and DISCOVERABLE.\n\nClick the floating button (one with +) at the bottom right to find the chatting partner.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int x = item.getItemId();
//        if (x==R.id.menu_settings)
//            Intent intent = new Intent(context, Setting.class);
//            startActivity(intent);
//        }
        if (x == R.id.menu_bluetooth) {
            if ((!bluetoothAdapter.isEnabled())) {
                enableBluetooth();
            } else if (bluetoothAdapter.isEnabled()) {
                disableBluetooth();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void inBluChat1() {
        setState("Not Connected. Click \"+\" button");
        drawerLayout = findViewById(R.id.drawer_layout_chat_screenId);
        navigationView = findViewById(R.id.navigationView);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.str_menu_open, R.string.str_menu_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(item -> {
            int n = item.getItemId();

            if (n == android.R.id.home) {
                onBackPressed();
                closeDrawer();
                return true;
            }

            if (n == R.id.drawer_settings) {
//                closeDrawer();
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }

            if (n == R.id.drawer_rate) {
//                closeDrawer();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                }
                return true;
            }

            if (n == R.id.drawer_share) {
//                closeDrawer();
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("text/plain");
                intent.putExtra("android.intent.extra.TEXT", "Hey there! Here is an amazing Bluetooth Chat app! It is free and does not require internet connection. Click the link below to download it from Google PlayStore \n\nhttps://play.google.com/store/apps/details?id=com.pat254.BluChat");
                startActivity(Intent.createChooser(intent, "Share App Using"));
                return true;
            }

            if (n == R.id.drawer_moreApps) {
//                closeDrawer();
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/developer?id=pat254+Conglomerate")));
                return true;
            }

            if (n == R.id.drawer_developer) {
                startActivity(new Intent(getApplicationContext(), DeveloperActivity.class));
                return true;
            }

            if (n == R.id.drawer_checkUpdates) {
//                closeDrawer();
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                return true;
            }

            //        if (x==R.id.help) {
//            new AlertDialog.Builder(context)
//                    .setCancelable(true)
//                    .setMessage("In any case the connection to other device fails try out the following solutions: \n1. Check if the two devices have bluetooth enabled and discoverable. \n2. Ask the other device to send the chatting request. \n3. Forget the paired devices in device bluetooth settings and try the session newly. \n\nIf none of the solution works please contact\nPhone: +254799858285 \nE-mail: patrickmathina335@gmail.com")
//                    .show();

            if (n == R.id.drawer_feedback) {
//                closeDrawer();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri data = Uri.parse("mailto:?subject=" + "BluChat Feedback" + "&body=" +
                        "Hey, ... " + "&to=" + "patsofts.help@gmail.com");
                intent.setData(data);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), "Type an email", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "No application can handle the operation", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            return true;
        });

        listMainChat = findViewById(R.id.list_conversation);
        ImageView imageViewAddPhoto = findViewById(R.id.add_photo);
        ImageView imageViewStartCamera = findViewById(R.id.start_camera);
        ImageView imageViewEmoji = findViewById(R.id.imgVw_emoji);
        editTxtCreateMessage = findViewById(R.id.ed_enter_message);
        ImageView imageViewSend = findViewById(R.id.imgVw_sendData);
        imageViewLoadImage = findViewById(R.id.loadImage);
        textViewDescription = findViewById(R.id.picDescription);
        textViewBtVsDiscoverableStatus = findViewById(R.id.btVsDiscoverableStatus);
        FloatingActionButton floatingActionButtonAdd = findViewById(R.id.floatingActionAdd);
        btOnOff = findViewById(R.id.BtOnOff);

        adapterMainChat = new ArrayAdapter<>(context, R.layout.device_list_item);
        listMainChat.setAdapter(adapterMainChat); //setting adapter

        //Adding onClickListener
        imageViewAddPhoto.setOnClickListener(v -> checkGalleryPermission());

        imageViewStartCamera.setOnClickListener(v -> checkCameraPermission());

        imageViewEmoji.setOnClickListener(v -> {
//                Toast.makeText(context,"Working on it", Toast.LENGTH_SHORT).show();
        });

        imageViewSend.setOnClickListener(v -> {
            String description = textViewDescription.getText().toString();
            String message = editTxtCreateMessage.getText().toString();

            if (!message.isEmpty() || !description.isEmpty() || !imageViewAddPhoto.isEnabled()) {
                editTxtCreateMessage.setText("");
//                textViewDescription.setText("");
                textViewDescription.setVisibility(View.GONE);
//                imageViewAddPhoto.setImageBitmap(null);
                chatManager.write(message.getBytes());

//                    onAddChatMessages(connectedDevice, null,message);
//                Toast.makeText(context, "Sent", Toast.LENGTH_SHORT).show();
            }
//                else { //if (message.isEmpty())  //if message is empty
////                    Toast.makeText(context, "Type a message", Toast.LENGTH_SHORT).show();
//                }
        });

        floatingActionButtonAdd.setOnClickListener(v -> {
            if (bluetoothAdapter.isEnabled() && (bluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)) { //if bluetoothAdapter is enabled and discoverable
                checkLocationPermission();
            } else if (!bluetoothAdapter.isEnabled()) { //if bluetoothAdapter is not enabled
                Toast.makeText(context, "Bluetooth is off.\nClick the bluetooth icon at the top right to turn on bluetooth  ", Toast.LENGTH_LONG).show();
            } else if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent dIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                dIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
                startActivity(dIntent);
                btOnOff.setVisibility(View.VISIBLE);
                btOnOff.setText(R.string.str_bluetooth_on);
            }
        });
    }

    private void closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void hasBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) { //checks if the bluetooth adapter is null - means the device has no bluetooth adapter
            Toast.makeText(context, "Bluetooth is not available on this device", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    //check if bluetooth is enabled
    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setMessage("Turn on Bluetooth?")
                    .setPositiveButton("Turn On", (dialogInterface, which) -> {
                        if ((bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)) {
                            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
                            startActivity(intent);
                            btOnOff.setVisibility(View.VISIBLE);
                            btOnOff.setText(R.string.str_bluetooth_on);
                            textViewBtVsDiscoverableStatus.setVisibility(View.GONE);
                        }
                        bluetoothAdapter.enable();
                        Toast.makeText(context, "Bluetooth Enabled", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", (dialogInterface, which) -> {
//                                MainActivity.this.finish(); // close the application
                    })
                    .show();
        }
    }

    //check if bluetooth is enabled OnCreate
    private void enableBluetoothOnCreate() {
        if (!bluetoothAdapter.isEnabled()) {
            new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setMessage("Bluetooth is turned off. \nPlease Turn On Bluetooth")
                    .setPositiveButton("Turn On", (dialogInterface, which) -> {
                        if ((bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)) {
                            Intent dIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                            dIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
                            startActivity(dIntent);
                            btOnOff.setVisibility(View.VISIBLE);
                            btOnOff.setText(R.string.str_bluetooth_on);
//                                Toast.makeText(context, "Bluetooth discoverable", Toast.LENGTH_LONG).show();
                        }
                        bluetoothAdapter.enable();
                        Toast.makeText(context, "Bluetooth enabled", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", (dialogInterface, which) -> {
//                                MainActivity.this.finish(); // close the application
                        new AlertDialog.Builder(context)
                                .setCancelable(false)
                                .setMessage("This app requires bluetooth ON for it functionality. \nPlease consider turning on bluetooth")
                                .setPositiveButton("Turn On", (dialogInterface12, which12) -> {
                                    bluetoothAdapter.enable();
                                    Toast.makeText(context, "Bluetooth enabled", Toast.LENGTH_LONG).show();
                                    Intent dIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                                    dIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);
                                    startActivity(dIntent);
                                    btOnOff.setVisibility(View.VISIBLE);
                                    btOnOff.setText(R.string.str_bluetooth_on);
//                                                Toast.makeText(context, "Bluetooth discoverable", Toast.LENGTH_LONG).show();
//                                                enableBluetoothOnCreate(); //check for permission again
                                })
                                .setNegativeButton("Deny & EXIT", (dialogInterface1, which1) -> {
                                    MainActivity.this.finish(); // close class MainActivity
//                                                                           alternatively can use
//                                                android.os.Process.killProcess(android.os.Process.myPid());   // kill all the running processes in the app and exit
//                                                                           alternatively can use
//                                                onBackPressed(); //close running class
//                                                                           alternatively can use
//                                                moveTaskToBack(true);
//                                                android.os.Process.killProcess(android.os.Process.myPid());
//                                                System.exit(1);
//                                                                           alternatively can use
//                                                finish();
                                })
                                .show();
                    })
                    .show();
        }
    }

    public void disableBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setMessage("This app requires bluetooth ON for it functionality. \nDo you still want to turn off Bluetooth?")
                    .setPositiveButton("Turn Off", (dialogInterface, which) -> {
                        bluetoothAdapter.disable();
                        btOnOff.setVisibility(View.VISIBLE);
                        btOnOff.setText(R.string.str_bluetooth_off);
                        textViewBtVsDiscoverableStatus.setVisibility(View.GONE);
                        Toast.makeText(context, "Bluetooth disabled", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", (dialogInterface, which) -> {
//                            MainActivity.this.finish(); // close the application
                    })
                    .show();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //checking if the FINE_LOCATION permission not granted
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST); //if not granted then request for permission
        } else {
            Intent intent = new Intent(context, DeviceListActivity.class);
            startActivityForResult(intent, SELECT_DEVICE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkGalleryPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_GALLERY_PERMISSION_CODE);
        } else {
            Intent openGallery = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(openGallery, ADD_PHOTO_REQUEST);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
        } else {
            Intent openCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(openCamera, CAMERA_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // setting photo in image view
        if (requestCode == ADD_PHOTO_REQUEST && resultCode == RESULT_OK) {
            Uri targetUri = data.getData();
            textViewDescription.setVisibility(View.VISIBLE);
            textViewDescription.setText(targetUri.toString());
            Bitmap image;
            try {
                image = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
                textViewBtVsDiscoverableStatus.setVisibility(View.GONE);
                imageViewLoadImage.setImageBitmap(image);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // setting captured image in image view
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap capturedPhoto = (Bitmap) data.getExtras().get("data");
            textViewBtVsDiscoverableStatus.setVisibility(View.GONE);
            imageViewLoadImage.setImageBitmap(capturedPhoto);
        }

        // passing the address of the clicked device in DeviceListActivity back to mainActivity
        if (requestCode == SELECT_DEVICE && resultCode == RESULT_OK) {
            String address = data.getStringExtra("deviceAddress");
            chatManager.connect(bluetoothAdapter.getRemoteDevice(address));
            Toast.makeText(context, "Address: " + address, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        //grant location permission
        if (requestCode == LOCATION_PERMISSION_REQUEST) { //checking if the requestCode is LOCATION_PERMISSION_REQUEST
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //grantResults.length > 0 checks if there are some results, and if there are .PERMISSION_GRANTED
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(context, DeviceListActivity.class); //if permission is granted then call DeviceListActivity
                startActivityForResult(intent, SELECT_DEVICE);

            } else { //if this(grantResults.length > 0) not true, request for permission again
                // (dialogInterface, which) ->
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setMessage("Location permission is required. \nPlease grant")
                        .setPositiveButton("GRANT", (dialogInterface, which) -> {
                            checkLocationPermission(); //check for permission again
                        })
                        .setNegativeButton("DENY & EXIT", (dialogInterface, which) -> {
                            MainActivity.this.finish(); // close the application
//                                                           alternatively can use
//                              android.os.Process.killProcess(android.os.Process.myPid()); // kill all the running processes in the app and exit
//                                                            alternatively can use
//                                onBackPressed(); //close running class
//                                                            alternatively can use
//                                moveTaskToBack(true);
//                                android.os.Process.killProcess(android.os.Process.myPid());
//                                System.exit(1);
//                                                             alternatively can use
//                                finish();
                        })
                        .show();
            }
        } else { //if the requestCode is not LOCATION_PERMISSION_REQUEST
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        // grant gallery permission
        if (requestCode == MY_GALLERY_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Gallery permission granted", Toast.LENGTH_LONG).show();

                Intent intentGallery = new Intent(Intent.ACTION_GET_CONTENT);
                intentGallery.setType("*/*");
                intentGallery.addCategory(Intent.CATEGORY_OPENABLE);
//                startActivity(intentGallery);
//                startActivityForResult(Intent.createChooser(intentGallery, "Select file or dir"), ADD_PHOTO_REQUEST);

//                Intent startGallery = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentGallery, ADD_PHOTO_REQUEST);
            } else {
                Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_LONG).show();
            }
        }

        // grant camera permission
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_LONG).show();

                Intent startCamera = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(startCamera, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatManager != null) {
            chatManager.stop();
        }
    }
}