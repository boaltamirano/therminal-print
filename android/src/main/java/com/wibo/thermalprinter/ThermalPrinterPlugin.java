package com.wibo.thermalprinter;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;

import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbConnections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@CapacitorPlugin(name = "ThermalPrinter")
public class ThermalPrinterPlugin extends Plugin {

    private final Map<String, DeviceConnection> connections = new HashMap<>();

    private static final String TAG = "printer_agent";

    @PluginMethod
    public void requestPermissions(PluginCall call){
        try {
            JSObject data = call.getData();
            DeviceConnection deviceConnection = getDevice(call, data);
            if (deviceConnection != null){
                UsbDevice usbDevice = ((UsbConnection) deviceConnection).getDevice();
                String intentName = "thermalPrinterUSBRequest" + usbDevice.getDeviceId();
                PendingIntent permissionIntent = PendingIntent.getBroadcast(
                        getContext(),
                        0,
                        new Intent(intentName),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );
                BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (action != null && action.equals(intentName)) {
                            getContext().unregisterReceiver(this);
                            synchronized (this) {
                                UsbManager usbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
                                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                    if (usbManager != null && usbDevice != null) {
                                        call.resolve(new JSObject().put("granted", true));
                                        return;
                                    }
                                }
                                call.reject(String.valueOf(new JSObject().put("granted", false)));
                            }
                        }
                    }
                };
                IntentFilter filter = new IntentFilter(intentName);
                getContext().registerReceiver(broadcastReceiver, filter);

                UsbManager usbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);
                if (usbManager != null) {
                    usbManager.requestPermission(usbDevice, permissionIntent);
                } else {
                    call.reject("UsbManager is null");
                }
            } else {
                call.reject("Device not found");
            }
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void listPrinters(PluginCall call) {
        try {
            JSArray printers = new JSArray();

            UsbConnections printerConnections = new UsbConnections(getContext());

            List<UsbConnection> usbConnectionsList = Arrays.asList(printerConnections.getList());

            // Agregar declaración de depuración para verificar la lista de conexiones USB
            Log.d("listPrinters", "USB Connections found: " + usbConnectionsList.size());

            for (UsbConnection usbConnection : usbConnectionsList) {
                UsbDevice usbDevice = usbConnection.getDevice();
                JSObject printerObj = new JSObject();
                printerObj.put("productName", Objects.requireNonNull(usbDevice.getProductName()).trim());
                printerObj.put("manufacturerName", usbDevice.getManufacturerName());
                printerObj.put("deviceId", usbDevice.getDeviceId());
                printerObj.put("serialNumber", usbDevice.getSerialNumber());
                printerObj.put("vendorId", usbDevice.getVendorId());
                printers.put(printerObj);
            }

            JSObject result = new JSObject();
            result.put("printers", printers);
            call.resolve(result);
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void printFormattedText(PluginCall call) {

        try {
            JSObject data = call.getData();
            EscPosPrinter printer = this.getPrinter(call, data);
            int dotsFeedPaper = data.has("mmFeedPaper")
                    ? printer.mmToPx((float) data.getDouble("mmFeedPaper"))
                    : data.optInt("dotsFeedPaper", 20);
            assert printer != null;
            printer.printFormattedText(data.getString("text"), dotsFeedPaper);
            JSObject result = new JSObject();
            result.put("status", "success");
            result.put("data", data);
            call.resolve(result);
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    private EscPosPrinter getPrinter(PluginCall call, JSObject data) throws EscPosConnectionException {
        DeviceConnection deviceConnection = getDevice(call, data);
        if (deviceConnection == null) {
            call.reject("Device not found");
            return null;
        }

        EscPosCharsetEncoding charsetEncoding = null;
        if (data.has("charsetEncoding")) {
            JSObject charsetEncodingData = data.getJSObject("charsetEncoding");
            assert charsetEncodingData != null;
            charsetEncoding = new EscPosCharsetEncoding(
                    charsetEncodingData.getString("charsetName", "windows-1252"),
                    charsetEncodingData.getInteger("charsetId", 16)
            );
        }

        return new EscPosPrinter(
                deviceConnection,
                data.optInt("printerDpi", 203),
                (float) data.optDouble("printerWidthMM", 48.0),
                data.optInt("printerNbrCharactersPerLine", 32),
                charsetEncoding
        );
    }

    private DeviceConnection getDevice(PluginCall call, JSObject data) {
        String id = data.getString("id");

        String hashKey = "usb-" + id;
        if (connections.containsKey(hashKey)) {
            DeviceConnection connection = connections.get(hashKey);
            assert connection != null;
            if (connection.isConnected()) {
                return connection;
            } else {
                connections.remove(hashKey);
            }
        }

        UsbConnections printerConnections = new UsbConnections(getContext());
        for (UsbConnection usbConnection : Objects.requireNonNull(printerConnections.getList())) {
            UsbDevice usbDevice = usbConnection.getDevice();
            if (usbDevice.getDeviceId() == Integer.parseInt(id) || Objects.requireNonNull(usbDevice.getProductName()).trim().equals(id)) {
                return usbConnection;
            }
        }
        return null;
    }

    @PluginMethod
    public void launchIntent(PluginCall call) {
        String param = call.getString("param");

        if (param == null) {
            call.reject("Param is required");
            return;
        }

        launchPrintIntent(getContext(), param);
        call.resolve();
    }


    private void launchPrintIntent(Context context, String param) {
        String packageName = "la.belltech.devicestesting.test";
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);

        if (launchIntent != null) {
            String className = Objects.requireNonNull(launchIntent.getComponent()).getClassName();

            Log.d(TAG, "launchPrintIntent - 2 " + false);

            Intent printIntent = new Intent();
            printIntent.setClassName(packageName, className);
            printIntent.setAction(Intent.ACTION_SEND);
            printIntent.putExtra(Intent.EXTRA_TEXT, param);
            printIntent.setType("text/plain");
            context.startActivity(printIntent);
        } else {
            Log.d(TAG, "launchPrintIntent - 1 " + true);
        }
    }

}
