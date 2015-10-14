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
    final Application app;
    final String address;
    final String endpoint;

    EndpointMessageListener listener;


    public NfcScannerIOHandler(String ident, Application hnd) {
        app = hnd;
        if (!ident.contains("@"))
            throw new RuntimeException("Failed to parse ident, missing @");
        String[] parts = ident.split("@");
        if (parts.length != 2)
            throw new RuntimeException("Failed to parse ident, unexpected length");

        endpoint = parts[0];
        address = parts[1];
        IO.Options opts = new IO.Options();
        opts.reconnection = true;
        try {
            client = IO.socket(String.format("http://%s", address), opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to address, invalid format");
        }
    }

    private JSONObject getTransmitBlock() {
        String id = Settings.Secure.getString(app.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        JSONObject obj = new JSONObject();
        try {
            obj.put("device_id", id);
            obj.put("device_name", android.os.Build.MODEL);
            obj.put("endpoint", endpoint);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return obj;
    }
    public void connect() {

        client.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("NfcScanner", "Connected to middleware, sending scanner.register");

                JSONObject obj = getTransmitBlock();

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

    public void setEndpointMessageListener(EndpointMessageListener listener) {
        this.listener = listener;
    }

    public void sendScannedCard(int id) throws JSONException {
        JSONObject obj = getTransmitBlock();
        obj.put("card_id", id);

        client.emit("scanner.scanned", obj);
    }

}
class EndpointMessage {
    public String message;
    public JSONObject payload;
}
abstract class EndpointMessageListener {
    public abstract void message(EndpointMessage message);
}