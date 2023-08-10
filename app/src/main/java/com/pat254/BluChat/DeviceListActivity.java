package com.pat254.BluChat;

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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity {

    // CREATING INSTANCES
    private ProgressBar progressScanDevices;
    private TextView stateDescriptionClick;
    private TextView showScanning;
    private TextView clickIfAlreadyPaired;
    FloatingActionButton floatingActionButtonScan;
    private ArrayAdapter<String> adapterAvailableDevices;
    private ArrayAdapter<String> adapterPairedDevices;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        this.setTitle("Available & Paired Devices");
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true); //to set the back arrow button on the action bar
        context = this; //initializing context
        inBluChat2();
        scanDevices();
    }

    private void inBluChat2() {
        progressScanDevices = findViewById(R.id.progress_scan_devices);
        floatingActionButtonScan = findViewById(R.id.floatingActionBtnScan);
        stateDescriptionClick = findViewById(R.id.stateDescriptionC);
        showScanning = findViewById(R.id.scanning___);
        clickIfAlreadyPaired = findViewById(R.id.clickIfAlreadyPairedD);
        TextView noPairedDevices = findViewById(R.id.noPairedDevices);

        ListView listPairedDevices = findViewById(R.id.list_paired_devices);
        ListView listAvailableDevices = findViewById(R.id.list_available_devices);

        adapterAvailableDevices = new ArrayAdapter<>(context, R.layout.device_list_item);
        adapterPairedDevices = new ArrayAdapter<>(context, R.layout.device_list_item);

        //setting adapters in the list
        listAvailableDevices.setAdapter(adapterAvailableDevices);
        listPairedDevices.setAdapter(adapterPairedDevices);

        //adding onItemClickListener
        floatingActionButtonScan.setOnClickListener(v -> scanDevices());

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
            String address = info.substring(info.length() - 17); // get the clicked device address
            //pass the device address back to the MainActivity
            Intent intent = new Intent();
            intent.putExtra("deviceAddress", address);
            setResult(RESULT_OK, intent);
            finish();
        });

        // Listing paired devices
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Initializing bluetoothAdapter
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices(); // returns all paired devices
        if (pairedDevices != null && pairedDevices.size() > 0) { // if pairedDevice is not empty and there is at least 1 paired device
            for (BluetoothDevice device : pairedDevices) { // loop/add BluetoothDevice device with pairedDevices
                adapterPairedDevices.add(device.getName() + "\n" + device.getAddress()); // list paired devices in adapterPairedDevices- device name and address
            }
        } else { //if (pairedDevices == null && pairedDevices.size() == 0)
            clickIfAlreadyPaired.setVisibility(View.GONE);
//            clickIfAlreadyPaired.setText("");
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
                    adapterAvailableDevices.add(device.getName() + "\n" + device.getAddress()); //add it to the AvailableDeviceAdapter - device name and address
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) { //if the discovering of the devices was finished
                floatingActionButtonScan.setVisibility(View.VISIBLE);
                stateDescriptionClick.setVisibility(View.VISIBLE);
                progressScanDevices.setVisibility(View.GONE);
                showScanning.setText(R.string.str_scanFinished);
                if (adapterAvailableDevices.getCount() == 0) { //if no devices found
                    stateDescriptionClick.setText("No new device found.\nClick the scan button bellow to rescan");
                } else if (adapterAvailableDevices.getCount() > 0) { // if found at least 1 available device
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
        int n = item.getItemId();

        if (n == android.R.id.home) { //back arrow button on the action bar
            onBackPressed();
            return true;
        }
        if (n == R.id.menu_scan_devices) {
            if (bluetoothAdapter.isDiscovering()) {
                Toast.makeText(getApplicationContext(), "Scan in progress", Toast.LENGTH_SHORT).show();
            } else {
                scanDevices();
                return true; //'return true' coz this function expect a boolean value
            }

            /*
             switch (item.getItemId()) {
                 case R.id.menu_scan_
                     return true;
                 default:
                     return super.onOptionsItemSelected(item);
             }
            */
        }
        return super.onOptionsItemSelected(item);
    }

    private void scanDevices() {
        showScanning.setVisibility(View.VISIBLE);
        progressScanDevices.setVisibility(View.VISIBLE);
        adapterAvailableDevices.clear();
        stateDescriptionClick.setVisibility(View.GONE);
        floatingActionButtonScan.setVisibility(View.GONE);
        showScanning.setText(R.string.str_scanning);

        if (bluetoothAdapter.isDiscovering()) { //if bluetoothAdapter is currently looking for available devices
            bluetoothAdapter.cancelDiscovery(); // if so, cancel it
        }
        bluetoothAdapter.startDiscovery();     // and start again
    }
}