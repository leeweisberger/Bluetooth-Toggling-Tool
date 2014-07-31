package com.example.bluetooth_app;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainScreenFragment extends Fragment {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Button bluetoothButton;
    private TextView bluetoothMessage;
    private ArrayAdapter<String> mArrayAdapter;
    private static AlertDialog.Builder mDialogue; 
    private BluetoothSocket mmSocket = null;


    private String BLUETOOTH_BUTTON_OFF="Enable Bluetooth";
    private String BLUETOOTH_BUTTON_SEARCH="Search For Device";
    private String BLUETOOTH_BUTTON_ON="Sync With Device";

    private String BLUETOOTH_MESSAGE_OFF="Bluetooth Is Off";
    private String BLUETOOTH_MESSAGE_SEARCH="No Devices Connected";
    private String BLUETOOTH_MESSAGE_ON="Device Connected";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(mReceiver, filter);
        getActivity().registerReceiver(mReceiver, filter2);
        mArrayAdapter=new ArrayAdapter<String>(getActivity(),R.layout.simple_text_view);
        mDialogue = new AlertDialog.Builder(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {


        View v = inflater.inflate(R.layout.main_activity_fragment, container, false);
        bluetoothMessage = (TextView) v.findViewById(R.id.bluetooth_message);
        bluetoothButton = (Button) v.findViewById(R.id.bluetooth_button);
        Button editPaymentButton = (Button) v.findViewById(R.id.edit_payments_button);

        initializeBluetoothText();


        bluetoothButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mArrayAdapter.clear();
                if(bluetoothButton.getText().equals(BLUETOOTH_BUTTON_OFF))
                    enableBluetooth();
                else if(bluetoothButton.getText().equals(BLUETOOTH_BUTTON_SEARCH)){
                    if(getPairedDevices()){
                        setArray();
                    }
                    else{
                        mBluetoothAdapter.startDiscovery(); 
                        setArray();
                    }

                }
            }
        });

        return v;
    }

    private boolean isBluetooth(){
        return mBluetoothAdapter == null; 
    }

    private void enableBluetooth(){
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } 
    }

    private void setBluetoothText(String buttonText, String messageText){
        bluetoothButton.setText(buttonText);
        bluetoothMessage.setText(messageText);
    }

    private void initializeBluetoothText(){
        if (!mBluetoothAdapter.isEnabled()) 
            setBluetoothText(BLUETOOTH_BUTTON_OFF, BLUETOOTH_MESSAGE_OFF);
        else
            setBluetoothText(BLUETOOTH_BUTTON_SEARCH, BLUETOOTH_MESSAGE_SEARCH);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    setBluetoothText(BLUETOOTH_BUTTON_OFF, BLUETOOTH_MESSAGE_OFF);
                    break;

                case BluetoothAdapter.STATE_ON:
                    setBluetoothText(BLUETOOTH_BUTTON_SEARCH, BLUETOOTH_MESSAGE_SEARCH);
                    break;

                }
            }
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());   
                mArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private boolean getPairedDevices(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
               
            }
        }
        return pairedDevices.size() > 0;
    }

    private void setArray(){
        //        dialogue= new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View convertView = (View) inflater.inflate(R.layout.bluetooth_list, null);
        mDialogue.setView(convertView);
        mDialogue.setIcon(R.drawable.ic_launcher);
        mDialogue.setTitle("Select a Bluetooth Device");

        mDialogue.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {       
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mBluetoothAdapter.cancelDiscovery();
            }
        });

        setDialogueAdapter();
        mDialogue.show();
    }

    private void setDialogueAdapter() {
        mDialogue.setAdapter(mArrayAdapter,
                new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String macAddress = mArrayAdapter.getItem(which).split("\n")[1];
                Log.d("Lee", "click");
                dialog.dismiss();
                mBluetoothAdapter.cancelDiscovery();
                new ConnectThread(mBluetoothAdapter.getRemoteDevice(macAddress)).run();
            }
        });
    }

    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mReceiver);
        try {
            mmSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
     
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
     
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                TelephonyManager tManager = (TelephonyManager)getActivity().getSystemService(Context.TELEPHONY_SERVICE);
                String id = tManager.getDeviceId();
                
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) { }
            mmSocket = tmp;
        }
     
        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();
     
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
     
            // Do work to manage the connection (in a separate thread)
//            manageConnectedSocket(mmSocket);
        }
     
        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}
