package com.example.smarthouse;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.view.View;
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
    public TextView d10, d11, d12, d13;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        d10 = (TextView)findViewById(R.id.d10);
        d11 = (TextView)findViewById(R.id.d11);
        d12 = (TextView)findViewById(R.id.d12);
        d13 = (TextView)findViewById(R.id.d13);

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
                                switch (sbprint)
                                {
                                    case "D10 ON":
                                        d10.setText(sbprint);
                                        break;

                                    case "D10 OFF":
                                        d10.setText(sbprint);
                                        break;

                                    case "D11 ON":
                                        d11.setText(sbprint);
                                        break;

                                    case "D11 OFF":
                                        d11.setText(sbprint);
                                        break;

                                    case "D12 ON":
                                        d12.setText(sbprint);
                                        break;

                                    case "D12 OFF":
                                        d12.setText(sbprint);
                                        break;

                                    case "D13 ON":
                                        d13.setText(sbprint);
                                        break;

                                    case "D13 OFF":
                                        d13.setText(sbprint);
                                        break;

                                    default:
                                        break;
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
/////////////////////////D10////////////////////////////

    public void onClickBut1(View v)
    {
        if(myThreadConnected!=null)
        {
            byte[] bytesToSend = "a".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

    public void onClickBut2(View v)
    {
        if(myThreadConnected!=null)
        {
            byte[] bytesToSend = "A".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

////////////////////////D11////////////////////////////

    public void onClickBut3(View v)
    {
        if(myThreadConnected!=null)
        {
            byte[] bytesToSend = "b".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

    public void onClickBut4(View v)
    {
        if(myThreadConnected!=null)
        {
            byte[] bytesToSend = "B".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

//////////////////////D12//////////////////////////

    public void onClickBut5(View v)
    {
        if(myThreadConnected!=null)
        {
            byte[] bytesToSend = "c".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

    public void onClickBut6(View v)
    {
        if(myThreadConnected!=null)
        {
            byte[] bytesToSend = "C".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

//////////////////////D13//////////////////////////

    public void onClickBut7(View v)
    {
        if(myThreadConnected!=null)
        {
            byte[] bytesToSend = "d".getBytes();
            myThreadConnected.write(bytesToSend );
        }
    }

    public void onClickBut8(View v)
    {
        if(myThreadConnected!=null)
        {
            byte[] bytesToSend = "D".getBytes();
            myThreadConnected.write(bytesToSend );
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