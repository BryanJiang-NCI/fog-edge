package org.fog.smart;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.Log;
import org.fog.entities.FogBroker;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MyFogBroker extends FogBroker {

    public static final int UPLOAD_DATA_NOW = 9999;

    public MyFogBroker(String name) throws Exception {
        super(name);
    }

    @Override
    public void processEvent(SimEvent ev) {
        Log.formatLine("%.2f: %s received event with tag %d from %s", 
                CloudSim.clock(), getName(), ev.getTag(), CloudSim.getEntityName(ev.getSource()));

        switch (ev.getTag()) {
            // match custom event
            case UPLOAD_DATA_NOW:
                Log.printLine("Broker: Received UPLOAD_DATA_NOW signal. Starting data upload process...");
                uploadData();
                
                Log.printLine("Broker: Data upload finished. Stopping simulation.");
                CloudSim.stopSimulation();
                break;
            
            // other events
            default:
                super.processEvent(ev);
                break;
        }
    }

    private void uploadData() {
        try {
            // simulate data
            for (int i = 0; i < 1; i++) {
                // water meter data
                String waterPayload = "{"
                        + "\"timestamp\":\"" + System.currentTimeMillis() + "\","
                        + "\"device_type\":\"water_meter\","
                        + "\"device_id\":\"WM1001\","
                        + "\"reading\":" + (15000 + i) + ","
                        + "\"unit\":\"L\","
                        + "\"battery\":" + (92 - i) + ","
                        + "\"location\":\"Apt-305\","
                        + "\"status\":\"normal\","
                        + "\"anomaly\":false"
                        + "}";
                httpUploadToAWS(waterPayload);

                // e meter data
                String elecPayload = "{"
                        + "\"timestamp\":\"" + System.currentTimeMillis() + "\","
                        + "\"device_type\":\"electricity_meter\","
                        + "\"device_id\":\"EM2001\","
                        + "\"reading\":" + String.format("%.2f", (3.0 + i * 0.5)) + ","
                        + "\"unit\":\"kWh\","
                        + "\"battery\":" + (85 - i) + ","
                        + "\"location\":\"Apt-305\","
                        + "\"status\":\"normal\","
                        + "\"anomaly\":false"
                        + "}";
                httpUploadToAWS(elecPayload);

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

	// post http request to upload data
    public static void httpUploadToAWS(String jsonPayload) {
        try {
            String endpoint = "http://44.192.64.44/upload";
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int code = conn.getResponseCode();
            Log.formatLine("HTTP upload completed with status code: %d, %s", code, jsonPayload);
            conn.disconnect();
        } catch (Exception ex) {
            Log.formatLine("HTTP upload EXCEPTION: " + ex.getMessage());
        }
    }
}