package com.jdgames.serversocket;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button connectMachine;

    String responseString = "";
    Button clearButton;
    TextView responseTextView;

    String sendString;
    Button sendButton;
    EditText editText;

    ServerSocket serverSocket = null;
    Socket socket = null;
    String serverIP = "0.0.0.0";
    int serverPort = 12345;

    Handler handler;

    boolean isSocketOpen = true;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);

        connectMachine = findViewById(R.id.button0);
        clearButton = findViewById(R.id.button1);
        sendButton = findViewById(R.id.button2);

        responseTextView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);

        handler = new Handler(Looper.getMainLooper());

        findViewById(R.id.button01).setOnClickListener(view -> {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            serverIP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            updateResponse("Your IP Address: " + serverIP);
            connectMachine.setEnabled(!serverIP.equalsIgnoreCase("0.0.0.0"));
        });

        findViewById(R.id.buttonDis).setOnClickListener(view -> handler.postDelayed(() -> {
            try {
                socket.close();
                isSocketOpen = false;
                serverSocket.close();
                updateResponse("Connection closed");
                findViewById(R.id.buttonDis).setEnabled(false);
            } catch (IOException e) {
                updateResponse(e.toString());
            }
        }, 500));

        connectMachine.setOnClickListener(view -> handler.postDelayed(() -> {
            try {
                CheckConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500));

        clearButton.setOnClickListener(view -> {
            responseString = "";
            responseTextView.setText(getString(R.string.hint_response));
            clearButton.setEnabled(false);
        });

        sendButton.setOnClickListener(view -> {
            String msg = "Sending '" + sendString + "'";
            updateResponse(msg);
            SendString(sendString);
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                sendString = editText.getText().toString().trim();
                sendButton.setEnabled(!sendString.isEmpty());
            }
        });

        editText.setText("@1307");
    }

    private void CheckConnection() throws Exception {
        updateResponse("Initializing...");
        if (serverSocket == null || serverSocket.isClosed()) {
            serverSocket = new ServerSocket(12345, 50, InetAddress.getByName(serverIP));
            updateResponse("Server socket created.");
        }
        isSocketOpen = true;
        new Thread(() -> {
            while (true) {
                if (isSocketOpen) {
                    try {
                        runOnUiThread(() -> updateResponse("Waiting for client accept..."));
                        socket = serverSocket.accept();
                        runOnUiThread(() -> updateResponse("Accepted by client."));

                        InputStream inputStream = socket.getInputStream();

                        Thread thread = new ClientThread(socket, inputStream);
                        thread.start();

                        Thread thread1 = new ReadClient(socket, inputStream);
                        thread1.start();

                        runOnUiThread(() -> findViewById(R.id.buttonDis).setEnabled(true));
                    } catch (Exception exception) {
                        runOnUiThread(() -> updateResponse(exception.toString()));
                    }
                }
            }
        }).start();
    }

    private class ClientThread extends Thread {
        Socket ss;
        InputStream inputStream;

        public ClientThread(Socket ss, InputStream inputStream) {
            this.ss = ss;
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            runOnUiThread(() -> updateResponse("Starting <space> ' ' write stream..."));
            while (true) {
                if (isSocketOpen) {
                    try {
                        Thread.sleep(200);
                        ss.getOutputStream().write(" ".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException | InterruptedException e) {
                        runOnUiThread(() -> updateResponse(e.toString()));
                    }
                }
            }
        }
    }

    private class ReadClient extends Thread {
        Socket ss;
        InputStream inputStream;

        public ReadClient(Socket ss, InputStream inputStream) {
            this.ss = ss;
            this.inputStream = inputStream;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            if (isSocketOpen) {
                try {
                    runOnUiThread(() -> updateResponse("Starting read stream..."));

                    inputStream = ss.getInputStream();
                    String show;
                    char a;
                    String receive = "";


                    while (true) {
                        int read = inputStream.read();
                        a = (char) read;
                        show = String.valueOf(a);
                        if (show.equals("#")) {
                            receive = String.format("%s%s", receive, show);
                        }
                        if ((!show.equals("\n")) && (!show.equals("#"))) {
                            receive = receive + show;
                            if (receive.contains("Device Ready") && !receive.contains("$")) {
                                String finalReceive = receive;
                                runOnUiThread(() -> updateResponse("Device Ready:" + finalReceive));
                            }
                        } else if (receive.length() > 0) {
                            String finalReceive1 = receive;
                            runOnUiThread(() -> updateResponse(finalReceive1));
                            receive = "";
                        }
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> updateResponse(e.toString()));
                }
            }
        }
    }

    private void SendString(String string) {
        handler.postDelayed(() -> new Thread(() -> {
            try {
                if (socket == null) {
                    runOnUiThread(() -> updateResponse("Connecting socket [" + serverIP + ":" + serverPort + "]"));
                    socket = new Socket(serverIP, serverPort);
                    runOnUiThread(() -> updateResponse("Socket connected."));
                }
                socket.getOutputStream().write(string.getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                runOnUiThread(() -> editText.setText(""));
                runOnUiThread(() -> updateResponse("sent to " + socket));
            } catch (IOException e) {
                runOnUiThread(() -> updateResponse(e.toString()));
            }
        }).start(), 500);
    }

    private void updateResponse(String newResponse) {
        responseString = responseString + newResponse + "\n";
        responseTextView.setText(responseString);
        clearButton.setEnabled(true);
    }
}