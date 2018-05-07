package com.redbear.chat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Chat extends Activity implements View.OnClickListener {
	private final static String TAG = Chat.class.getSimpleName();

	public static final String EXTRAS_DEVICE = "EXTRAS_DEVICE";
	private TextView tv = null;
	private Button btnSlowWag = null;
    private Button btnSlowSway = null;
    private Button btnSlowLift = null;
    private Button btnMediumWag = null;
    private Button btnMediumSway = null;
    private Button btnMediumLift = null;
    private Button btnFastWag = null;
    private Button btnFastSway = null;
    private Button btnFastLift = null;
	private String mDeviceName;
	private String mDeviceAddress;
	private RBLService mBluetoothLeService;
	private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((RBLService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
			} else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				getGattService(mBluetoothLeService.getSupportedGattService());
			} else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getByteArrayExtra(RBLService.EXTRA_DATA));
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.second);

        btnSlowWag = (Button) findViewById(R.id.btnSlowWag);
        btnSlowWag.setOnClickListener(this); // calling onClick() method
        btnSlowSway = (Button) findViewById(R.id.btnSlowSway);
        btnSlowSway.setOnClickListener(this);
        btnSlowLift = (Button) findViewById(R.id.btnSlowLift);
        btnSlowLift.setOnClickListener(this);

        btnMediumWag = (Button) findViewById(R.id.btnMediumWag);
        btnMediumWag.setOnClickListener(this); // calling onClick() method
        btnMediumSway = (Button) findViewById(R.id.btnMediumSway);
        btnMediumSway.setOnClickListener(this);
        btnMediumLift = (Button) findViewById(R.id.btnMediumLift);
        btnMediumLift.setOnClickListener(this);

        btnFastWag = (Button) findViewById(R.id.btnFastWag);
        btnFastWag.setOnClickListener(this); // calling onClick() method
        btnFastSway = (Button) findViewById(R.id.btnFastSway);
        btnFastSway.setOnClickListener(this);
        btnFastLift = (Button) findViewById(R.id.btnFastLift);
        btnFastLift.setOnClickListener(this);

		Intent intent = getIntent();

		mDeviceAddress = intent.getStringExtra(Device.EXTRA_DEVICE_ADDRESS);
		mDeviceName = intent.getStringExtra(Device.EXTRA_DEVICE_NAME);

		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		Intent gattServiceIntent = new Intent(this, RBLService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
    public void onClick(View v) {
        String str = "";

        switch (v.getId()){

            case R.id.btnSlowWag: str = "slowWag";
            break;
            case R.id.btnSlowSway: str = "slowSway";
            break;
            case R.id.btnSlowLift: str = "slowLift";
            break;

            case R.id.btnMediumWag: str = "MediumWag";
                break;
            case R.id.btnMediumSway: str = "MediumSway";
                break;
            case R.id.btnMediumLift: str = "MediumLift";
                break;

            case R.id.btnFastWag: str = "FastWag";
                break;
            case R.id.btnFastSway: str = "FastSway";
                break;
            case R.id.btnFastLift: str = "FastLift";
                break;
        }

        BluetoothGattCharacteristic characteristic = map
                .get(RBLService.UUID_BLE_SHIELD_TX);
        byte b = 0x00;
        byte[] tmp = str.getBytes();
        byte[] tx = new byte[tmp.length + 1];
        tx[0] = b;
        for (int i = 1; i < tmp.length + 1; i++) {
            tx[i] = tmp[i - 1];
        }

        characteristic.setValue(tx);
        mBluetoothLeService.writeCharacteristic(characteristic);
    }


	@Override
	protected void onResume() {
		super.onResume();

		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			mBluetoothLeService.disconnect();
			mBluetoothLeService.close();

			System.exit(0);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		super.onStop();

		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mBluetoothLeService.disconnect();
		mBluetoothLeService.close();

		System.exit(0);
	}

	private void displayData(byte[] byteArray) {
		if (byteArray != null) {
			String data = new String(byteArray);
			tv.append(data);
			// find the amount we need to scroll. This works by
			// asking the TextView's internal layout for the position
			// of the final line and then subtracting the TextView's height
			final int scrollAmount = tv.getLayout().getLineTop(
					tv.getLineCount())
					- tv.getHeight();
			// if there is no need to scroll, scrollAmount will be <=0
			if (scrollAmount > 0)
				tv.scrollTo(0, scrollAmount);
			else
				tv.scrollTo(0, 0);
		}
	}

	private void getGattService(BluetoothGattService gattService) {
		if (gattService == null)
			return;

		BluetoothGattCharacteristic characteristic = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
		map.put(characteristic.getUuid(), characteristic);

		BluetoothGattCharacteristic characteristicRx = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
		mBluetoothLeService.setCharacteristicNotification(characteristicRx,
				true);
		mBluetoothLeService.readCharacteristic(characteristicRx);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);

		return intentFilter;
	}
}
