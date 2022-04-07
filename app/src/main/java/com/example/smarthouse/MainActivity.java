package com.example.smarthouse;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{
    String MAC = "98:DA:60:04:5E:E0";
    BluetoothAdapter bluetoothAdapter;
    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;
    private UUID myUUID;
    final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
    private StringBuilder sb = new StringBuilder();

    private ProgressBar progressBar;
    private TextView textViewProgress;
    private RadioButton radioButtonHome;
    private RadioButton radioButtonStreet;
    private Switch switchPump;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("tag1");
        tabSpec.setContent(R.id.linearLayout);
        tabSpec.setIndicator("Бак");
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("tag2");
        tabSpec.setContent(R.id.linearLayout2);
        tabSpec.setIndicator("Скоро");
        tabHost.addTab(tabSpec);

        tabHost.setCurrentTab(0);

        progressBar = findViewById(R.id.progress_bar);
        textViewProgress = findViewById(R.id.text_view_progress);
        radioButtonHome = findViewById(R.id.radioButtonModeHome);
        radioButtonStreet = findViewById(R.id.radioButtonModeStreet);

        radioButtonHome.setOnClickListener(radioButtonClickListener);
        radioButtonStreet.setOnClickListener(radioButtonClickListener);

        switchPump = findViewById(R.id.switchPump);

        if (switchPump != null)
        {
            switchPump.setOnCheckedChangeListener(this::onCheckedChanged);
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);
        BluetoothDevice device2 = bluetoothAdapter.getRemoteDevice(MAC);
        myThreadConnectBTdevice = new ThreadConnectBTdevice(device2);
        myThreadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
    }

    private class ThreadConnectBTdevice extends Thread // Поток для коннекта с Bluetooth
    {
        private BluetoothSocket bluetoothSocket = null;
        private ThreadConnectBTdevice(BluetoothDevice device)
        {
            try
            {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }


        @Override
        public void run() // Коннект
        {
            boolean success = false;
            try
            {
                bluetoothSocket.connect();
                success = true;
            }

            catch (IOException e)
            {
                e.printStackTrace();

                runOnUiThread(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        Toast.makeText(MainActivity.this, "Нет коннекта, проверьте Bluetooth-устройство с которым хотите соединиться!", Toast.LENGTH_LONG).show();
                    }
                });

                try
                {
                    bluetoothSocket.close();
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
            }

            if(success)   // Если законнектились, тогда запускаем поток приёма и отправки данных
            {
                myThreadConnected = new ThreadConnected(bluetoothSocket);
                myThreadConnected.start(); // запуск потока приёма и отправки данных
                refreshData();
            }
        }


        public void cancel()
        {
            Toast.makeText(getApplicationContext(), "Close - BluetoothSocket", Toast.LENGTH_LONG).show();
            try
            {
                bluetoothSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private class ThreadConnected extends Thread    // Поток - приём и отправка данных
    {
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        private String sbprint;

        public ThreadConnected(BluetoothSocket socket)
        {
            InputStream in = null;
            OutputStream out = null;
            try
            {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }


        @Override
        public void run() // Приём данных
        {
            while (true)
            {
                try
                {
                    byte[] buffer = new byte[1];
                    int bytes = connectedInputStream.read(buffer);
                    String strIncom = new String(buffer, 0, bytes);
                    sb.append(strIncom); // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("\r\n"); // определяем конец строки

                    if (endOfLineIndex > 0)
                    {
                        sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());

                        runOnUiThread(new Runnable() // Вывод данных
                        {
                            @Override
                            public void run()
                            {
                                if (sbprint.charAt(0) == 'X')
                                {
                                    String percentString = sbprint.substring(1);
                                    try
                                    {
                                        int percentInt = Integer.parseInt(percentString);
                                        progressBar.setProgress(percentInt);
                                    }
                                    catch (Exception e)
                                    {
                                        Toast toast = Toast.makeText(getApplicationContext(), "Ошибка распознания процентов!", Toast.LENGTH_SHORT);
                                        toast.show();
                                    }
                                    percentString = percentString + "%";
                                    textViewProgress.setText(percentString);
                                }
                                else {
                                    switch (sbprint)
                                    {
                                        case "HOME":
                                            if (!radioButtonHome.isChecked())
                                            {
                                                radioButtonHome.setChecked(true);
                                                radioButtonStreet.setChecked(false);
                                                switchPump.setClickable(false);
                                            }
                                            break;

                                        case "STREET":
                                            if (!radioButtonStreet.isChecked())
                                            {
                                                radioButtonHome.setChecked(false);
                                                radioButtonStreet.setChecked(true);
                                                switchPump.setClickable(true);
                                            }
                                            break;

                                        case "PON":
                                            switchPump.setChecked(true);
                                            break;

                                        case "POFF":
                                            switchPump.setChecked(false);

                                        default:
                                            break;
                                    }
                                }
                            }
                        });
                    }
                }
                catch (IOException e)
                {
                    break;
                }
            }
        }


        public void write(byte[] buffer)
        {
            try
            {
                connectedOutputStream.write(buffer);
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

/////////////////// Нажатие кнопок /////////////////////

    View.OnClickListener radioButtonClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            RadioButton rb = (RadioButton)v;
            switch (rb.getId())
            {
                case R.id.radioButtonModeHome:
                    if (myThreadConnected != null)
                    {
                        byte[] bytesToSend = "h".getBytes();    // home mode
                        myThreadConnected.write(bytesToSend);
                    }
                    switchPump.setClickable(false);
                    break;

                case R.id.radioButtonModeStreet:
                    if (myThreadConnected != null)
                    {
                        byte[] bytesToSend = "s".getBytes();    // street mode
                        myThreadConnected.write(bytesToSend);
                    }
                    switchPump.setClickable(true);
                    break;

                default:
                    break;
            }
        }
    };

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        if (isChecked)
        {
            if (myThreadConnected != null)
            {
                byte[] bytesToSend = "n".getBytes();    // pump on
                myThreadConnected.write(bytesToSend);
            }
        }
        if (!isChecked)
        {
            if (myThreadConnected != null)
            {
                byte[] bytesToSend = "f".getBytes();    // pump off
                myThreadConnected.write(bytesToSend);
            }
        }
    }

////////////////////Refresh////////////////////////

    public void onClickRefresh(View view)
    {
        refreshData();
    }

    public void refreshData()
    {
        if(myThreadConnected != null)
        {
            byte[] bytesToSend = "r".getBytes();
            myThreadConnected.write(bytesToSend);
        }
    }

    @Override
    protected void onDestroy() // Закрытие приложения
    {
        super.onDestroy();
        if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();
    }
}