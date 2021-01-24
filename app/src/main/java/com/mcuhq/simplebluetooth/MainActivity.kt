package com.mcuhq.simplebluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName

    // GUI Components
    private var mBluetoothStatus: TextView? = null
    private var mReadBuffer: TextView? = null
    private var mScanBtn: Button? = null
    private var mOffBtn: Button? = null
    private var mListPairedDevicesBtn: Button? = null
    private var mDiscoverBtn: Button? = null
    private var mDevicesListView: ListView? = null
    private val mLED1: CheckBox? = null
    private var mBTAdapter: BluetoothAdapter? = null
    private var mPairedDevices: Set<BluetoothDevice>? = null
    private var mBTArrayAdapter: ArrayAdapter<String>? = null
    private var mHandler // Our main handler that will receive callback notifications
            : Handler? = null
    private var mConnectedThread // bluetooth background worker thread to send and receive data
            : ConnectedThread? = null
    private var mBTSocket: BluetoothSocket? = null // bi-directional client-to-client data path
    private var mSeekBar: SeekBar? = null
    private var mButton1: Button? = null
    private var mButton2: Button? = null
    private var count1: Int = 0
    private var count2: Int = 0
    private var dept: Int = 125
    private var color: String = "0"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mBluetoothStatus = findViewById(R.id.bluetooth_status) as TextView?
        mReadBuffer = findViewById(R.id.read_buffer) as TextView?
        mScanBtn = findViewById(R.id.scan) as Button?
        mOffBtn = findViewById(R.id.off) as Button?
        mDiscoverBtn = findViewById(R.id.discover) as Button?
        mListPairedDevicesBtn = findViewById(R.id.paired_btn) as Button?
        //mLED1 = (CheckBox)findViewById(R.id.checkbox_led_1);
        val mRadioGroup = findViewById(R.id.radioGroupColor) as RadioGroup?
        mBTArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        mBTAdapter = BluetoothAdapter.getDefaultAdapter() // get a handle on the bluetooth radio
        mDevicesListView = findViewById(R.id.devices_list_view) as ListView?
        mSeekBar = findViewById(R.id.seekBar) as SeekBar?
        mButton1 = findViewById(R.id.button1) as Button?
        mButton2 = findViewById(R.id.button2) as Button?

        mButton1!!.setOnClickListener{
            count1++;
            writeThread()
        }

        mButton2!!.setOnClickListener{
            count2++;
            writeThread()
        }
        
        mSeekBar!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                dept = p1;
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                writeThread()
            }

        })
        mDevicesListView!!.adapter = mBTArrayAdapter // assign model to view
        mDevicesListView!!.onItemClickListener = mDeviceClickListener

        // Ask for location permission if not already allowed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_READ) {
                    var readMessage: String? = null
                    try {
                        readMessage = String((msg.obj as ByteArray), charset("UTF-8"))
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                    mReadBuffer!!.text = readMessage
                }
                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1) mBluetoothStatus!!.text = "Connected to Device: " + msg.obj else mBluetoothStatus!!.text = "Connection Failed"
                }
            }
        }
        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus!!.text = "Status: Bluetooth not found"
            Toast.makeText(applicationContext, "Bluetooth device not found!", Toast.LENGTH_SHORT).show()
        } else {

//            mLED1.setOnClickListener(new View.OnClickListener(){
//                @Override
//                public void onClick(View v){
//                    if(mConnectedThread != null) //First check to make sure thread created
//                        if(((CheckBox)v).isChecked())
//                            mConnectedThread.write("1");
//                        else
//                            mConnectedThread.write("0");
//                }
//            });
            mScanBtn!!.setOnClickListener { bluetoothOn() }
            mOffBtn!!.setOnClickListener { bluetoothOff() }
            mListPairedDevicesBtn!!.setOnClickListener { listPairedDevices() }
            mDiscoverBtn!!.setOnClickListener { discover() }
            mRadioGroup!!.setOnCheckedChangeListener { _, checkedId ->
                // checkedId is the RadioButton selected
                if (mConnectedThread != null) { //First check to make sure thread created
                    when (checkedId) {
                        R.id.radioButtonRed -> color = "1"
                        R.id.radioButtonGreen -> color = "2"
                        R.id.radioButtonBlue -> color = "3"
                        R.id.radioButtonOff -> {
                            color = "0"
                            count1 = 0;
                            count2 = 0;
                        }
                    }
                    writeThread();
                }
            }
        }
    }

    private fun writeThread() {
        if(this.mConnectedThread != null) {
            var output = "" + color + ("000" + dept.toString()).takeLast(3);
            when (count1) {
                0 -> output += "0000"
                1 -> output += "0001"
                2 -> output += "0010"
                3 -> output += "0100"
                4 -> output += "1000"
                5 -> output += "1001"
                6 -> output += "1011"
                7 -> output += "1111"
                8 -> {
                    count1 = 0
                    output += "0000"
                }
            }
            when (count2) {
                0 -> output += "0000"
                1 -> output += "1000"
                2 -> output += "0100"
                3 -> output += "0010"
                4 -> output += "0001"
                5 -> {
                    count2 = 0
                    output += "0000"
                }
            }

            mConnectedThread!!.write(output);
        }
    }

    private fun bluetoothOn() {
        if (!mBTAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            mBluetoothStatus!!.text = "Bluetooth enabled"
            Toast.makeText(applicationContext, "Bluetooth turned on", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(applicationContext, "Bluetooth is already on", Toast.LENGTH_SHORT).show()
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus!!.text = "Enabled"
            } else mBluetoothStatus!!.text = "Disabled"
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun bluetoothOff() {
        mBTAdapter!!.disable() // turn off
        mBluetoothStatus!!.text = "Bluetooth disabled"
        Toast.makeText(applicationContext, "Bluetooth turned Off", Toast.LENGTH_SHORT).show()
    }

    private fun discover() {
        // Check if the device is already discovering
        if (mBTAdapter!!.isDiscovering) {
            mBTAdapter!!.cancelDiscovery()
            Toast.makeText(applicationContext, "Discovery stopped", Toast.LENGTH_SHORT).show()
        } else {
            if (mBTAdapter!!.isEnabled) {
                mBTArrayAdapter!!.clear() // clear items
                mBTAdapter!!.startDiscovery()
                Toast.makeText(applicationContext, "Discovery started", Toast.LENGTH_SHORT).show()
                registerReceiver(blReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            } else {
                Toast.makeText(applicationContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val blReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // add the name to the list
                mBTArrayAdapter!!.add("""
    ${device.name}
    ${device.address}
    """.trimIndent())
                mBTArrayAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun listPairedDevices() {
        mBTArrayAdapter!!.clear()
        mPairedDevices = mBTAdapter!!.bondedDevices
        val pairedDevices = mPairedDevices!!
        if (mBTAdapter!!.isEnabled) {
            // put it's one to the adapter
            for (device in pairedDevices) {
                mBTArrayAdapter!!.add(device.name + "\n" + device.address)
            }
            Toast.makeText(applicationContext, "Show Paired Devices", Toast.LENGTH_SHORT).show()
        } else Toast.makeText(applicationContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
    }

    private val mDeviceClickListener = OnItemClickListener { parent, view, position, id ->
        if (!mBTAdapter!!.isEnabled) {
            Toast.makeText(baseContext, "Bluetooth not on", Toast.LENGTH_SHORT).show()
            return@OnItemClickListener
        }
        mBluetoothStatus!!.text = "Connecting..."
        // Get the device MAC address, which is the last 17 chars in the View
        val info = (view as TextView).text.toString()
        val address = info.substring(info.length - 17)
        val name = info.substring(0, info.length - 17)

        // Spawn a new thread to avoid blocking the GUI one
        object : Thread() {
            override fun run() {
                var fail = false
                val device = mBTAdapter!!.getRemoteDevice(address)
                try {
                    mBTSocket = createBluetoothSocket(device)
                } catch (e: IOException) {
                    fail = true
                    Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
                }
                // Establish the Bluetooth socket connection.
                try {
                    mBTSocket!!.connect()
                } catch (e: IOException) {
                    try {
                        fail = true
                        mBTSocket!!.close()
                        mHandler!!.obtainMessage(CONNECTING_STATUS, -1, -1)
                                .sendToTarget()
                    } catch (e2: IOException) {
                        //insert code to deal with this
                        Toast.makeText(baseContext, "Socket creation failed", Toast.LENGTH_SHORT).show()
                    }
                }
                if (!fail) {
                    mConnectedThread = ConnectedThread(mBTSocket!!, mHandler!!)
                    mConnectedThread!!.start()
                    mHandler!!.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                            .sendToTarget()
                }
            }
        }.start()
    }

    @Throws(IOException::class)
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        try {
            val m = device.javaClass.getMethod("createInsecureRfcommSocketToServiceRecord", UUID::class.java)
            return m.invoke(device, BT_MODULE_UUID) as BluetoothSocket
        } catch (e: Exception) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e)
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID)
    }

    companion object {
        private val BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // "random" unique identifier

        // #defines for identifying shared types between calling functions
        private const val REQUEST_ENABLE_BT = 1 // used to identify adding bluetooth names
        const val MESSAGE_READ = 2 // used in bluetooth handler to identify message update
        private const val CONNECTING_STATUS = 3 // used in bluetooth handler to identify message status
    }
}