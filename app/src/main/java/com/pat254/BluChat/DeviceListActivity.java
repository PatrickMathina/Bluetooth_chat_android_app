package com.pat254.BluChat;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    // CREATING INSTANCES
    private ProgressBar progressScanDevices;
    private TextView stateDescriptionClick;
    private TextView showScanning;
    private TextView clickIfAlreadyPaired;
    private ArrayAdapter<String> adapterAvailableDevices;
    private ArrayAdapter<String> adapterPairedDevices;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true); //to set the back arrow button on the action bar
        context = this; //initializing context
        inBluChat2();
    }

    private void inBluChat2() {
        ListView listPairedDevices = findViewById(R.id.list_paired_devices);
        ListView listAvailableDevices = findViewById(R.id.list_available_devices);
        progressScanDevices = findViewById(R.id.progress_scan_devices);
        FloatingActionButton floatingActionButtonScan = findViewById(R.id.floatingActionBtnScan);
        stateDescriptionClick=findViewById(R.id.stateDescriptionC);
        showScanning=findViewById(R.id.scanning___);
        clickIfAlreadyPaired=findViewById(R.id.clickIfAlreadyPairedD);
        TextView noPairedDevices = findViewById(R.id.noPairedDevices);
        adapterPairedDevices = new ArrayAdapter<>(context, R.layout.device_list_item);
        adapterAvailableDevices = new ArrayAdapter<>(context, R.layout.device_list_item);

        // Add OnClickListener - for floatingActionButtonScan
        floatingActionButtonScan.setOnClickListener(v -> scanDevices());

        //setting adapters in the list
        listPairedDevices.setAdapter(adapterPairedDevices);
        listAvailableDevices.setAdapter(adapterAvailableDevices);

        //adding onItemClickListener
        listAvailableDevices.setOnItemClickListener((AdapterView, view, i, l) -> {
            String info = ((TextView) view).getText().toString(); //returns the name and address of the clicked device
            String address = info.substring(info.length() - 17); // get the clicked device address
            //pass the device address back to the MainActivity
            Intent intent = new Intent();
            intent.putExtra("deviceAddress", address);
            setResult(RESULT_OK, intent);
            finish();
        });
        listPairedDevices.setOnItemClickListener((AdapterView, view, i, l) -> {
            String info = ((TextView) view).getText().toString(); //returns the name and address of the clicked device
            String address1 = info.substring(info.length() - 17); // get the clicked device address
            //pass the device address back to the MainActivity
            Intent intent = new Intent();
            intent.putExtra("deviceAddress", address1);
            setResult(RESULT_OK, intent);
            finish();
        });

       // Listing paired devices
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Initializing bluetoothAdapter
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices(); // returns all paired devices
        if (pairedDevices != null && pairedDevices.size() > 0) { // if pairedDevice is not empty and there is at least 1 paired device
            for (BluetoothDevice device : pairedDevices) { // loop/add BluetoothDevice device with pairedDevices
                adapterPairedDevices.add( device.getName() + "\n" +  device.getAddress()); // list paired devices in adapterPairedDevices- device name and address
            }
        }
        else { //if (pairedDevices == null && pairedDevices.size() == 0)
            clickIfAlreadyPaired.setText("");
            noPairedDevices.setText(R.string.str_noPairedDevices);
        }

        // Listing available devices
        //Registering a broadcast receiver,
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(bluetoothDeviceListener, intentFilter);
        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothDeviceListener, intentFilter1);
    }

    // Defining a broadcast receiver - a mechanism to listen to all incoming devices
    private final BroadcastReceiver bluetoothDeviceListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); // found the device
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) { // if device is not paired device,
                    adapterAvailableDevices.add( device.getName() + "\n" +  device.getAddress()); //add it to the AvailableDeviceAdapter - device name and address
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) { //if the discovering of the devices was finished
                progressScanDevices.setVisibility(View.GONE); // hide the progress bar
                if (adapterAvailableDevices.getCount() == 0) { //if no devices found
                    showScanning.setText(R.string.str_scanFinished);
                    stateDescriptionClick.setText("No new device found.\nClick the scan button bellow to start scanning again.");
                } else if (adapterAvailableDevices.getCount() > 0) { // if found at least 1 available device
                    showScanning.setText(R.string.str_scanFinished);
                    stateDescriptionClick.setText("Click on the device to connect and start the chat.\nClick the scan button to start scanning again.");
                }
            }
        }
    };

    // inflating menu activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //adding onClickListener for item selected
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int n= item.getItemId();

        if (n==android.R.id.home) { //back arrow button on the action bar
            onBackPressed();
            return true;
        }
        if (n == R.id.menu_scan_devices) {
            scanDevices();
            return true; //'return true' coz this function expect a boolean value

            /*
             switch (item.getItemId()) {
                 case R.id.menu_scan_devices:
                     scanDevices(); //calling function scanDevices()
                     return true; //'return true' coz this function expect a boolean value
                 default:
                     return super.onOptionsItemSelected(item);
             }
            */
        }
        return super.onOptionsItemSelected(item);
    }

    private void scanDevices() {
        progressScanDevices.setVisibility(View.VISIBLE); //show progress bar when floatingActionButtonScan is clicked
        adapterAvailableDevices.clear();//clear all the available devices - to get the currently available devices
        stateDescriptionClick.setText("");
        showScanning.setText(R.string.str_scanning);
        clickIfAlreadyPaired.setText("");

        if (bluetoothAdapter.isDiscovering()) { //if bluetoothAdapter is currently looking for available devices
            bluetoothAdapter.cancelDiscovery(); // if so, cancel it
        }
        bluetoothAdapter.startDiscovery();     // and start again
        progressScanDevices.setVisibility(View.VISIBLE);
        stateDescriptionClick.setText("");
        showScanning.setText(R.string.str_scanning);
    }
}