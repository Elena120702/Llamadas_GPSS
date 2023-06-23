package com.example.llamadas_gpss;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity2 extends AppCompatActivity implements LocationListener {
    private TextView numeroGuardadoTextView;
    private TextView coordenadasTextView;
    private LocationManager locationManager;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private Handler handler;
    private Runnable runnable;
    private boolean callAnswered = false;
    private String incomingPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        numeroGuardadoTextView = findViewById(R.id.numero_guardado);
        Intent intent = getIntent();
        if (intent != null) {
            incomingPhoneNumber = intent.getStringExtra("numero_guardado");
            numeroGuardadoTextView.setText(incomingPhoneNumber);
        }

        coordenadasTextView = findViewById(R.id.coordenadasTextView);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            startCallDetection();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 2);
        }

        startService();

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!callAnswered) {
                    // Obtener las coordenadas actuales
                    double latitude = 0.0;  // Valor de latitud predeterminado
                    double longitude = 0.0; // Valor de longitud predeterminado

                    // Verificar si se ha obtenido la última ubicación conocida
                    if (ActivityCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null) {
                            latitude = lastKnownLocation.getLatitude();
                            longitude = lastKnownLocation.getLongitude();
                        }
                    }

                    // Llamar al método sendMessageWithCoordinates con las coordenadas
                    sendMessageWithCoordinates(latitude, longitude);

                    // Ejecutar el código cada cierto intervalo de tiempo (por ejemplo, cada 5 minutos)
                    handler.postDelayed(this, 300000); // 300000 milisegundos = 5 minutos
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isGPSEnabled()) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        stopCallDetection();
        handler.removeCallbacks(runnable);
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        serviceIntent.putExtra("numero_guardado", incomingPhoneNumber);
        startService(serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        stopService(serviceIntent);
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
    }

    private void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    private void startCallDetection() {
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        callAnswered = false;
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        callAnswered = true;
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        callAnswered = false;
                        handler.postDelayed(runnable, 5000); // Esperar 5 segundos después de finalizar la llamada
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void stopCallDetection() {
        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void sendMessageWithCoordinates(double latitude, double longitude) {
        String message = "Hola, mis coordenadas son: " + latitude + ", " + longitude;
        sendSMS(incomingPhoneNumber, message);
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Mensaje enviado correctamente", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al enviar el mensaje", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        coordenadasTextView.setText("Latitud: " + latitude + "\nLongitud: " + longitude);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}