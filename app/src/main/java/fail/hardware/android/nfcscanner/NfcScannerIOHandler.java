package fail.hardware.android.nfcscanner;

import android.app.Application;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class NfcScannerIOHandler {
    Socket client;
    public NfcScannerIOHandler(String ident, Application hnd) {
        final Application handler = hnd;
        if(!ident.contains("@"))
            throw new RuntimeException("Failde to parse ident, missing @");
        String[] parts = ident.split("@");
        if(parts.length != 2)
            throw new RuntimeException("Failed to parse ident, unexpected length");

        final String endpoint = parts[0];
        String address = parts[1];
        IO.Options opts = new IO.Options();
        opts.reconnection = true;
        try {
            client = IO.socket(String.format("http://%s", address), opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to address, invalid format");
        }

        client.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("NfcScanner", "Connected to middleware, sending scanner.register");
                String id = Settings.Secure.getString(handler.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                JSONObject obj = new JSONObject();
                try {
                    obj.put("device_id", id);
                    obj.put("device_name", android.os.Build.MODEL);
                    obj.put("endpoint", endpoint);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                client.emit("scanner.register", obj);
                Log.d("NfcScanner", "Sent scanner.register");
            }
        }).on("scanner.vibrate", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

            }
        }).on("scanner.settings", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

            }
        }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("NfcScanner", "Failed to connect");
                Log.d("NfcScanner", args.toString());
            }
        });
        client.connect();
        Log.d("NfcScanner", "Connecting");
    }
}