package io.edkek.tplink;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class SmartBulb {
    public static final short PORT = 9999;

    private Logger logger = LogManager.getLogger(getClass());

    private DatagramSocket udpSocket;
    private InetAddress address;

    private String ip;

    private Thread udpThread;

    public SmartBulb(String ip, Logger logger) throws IOException {
        this(ip);
        this.logger = logger;
    }

    public SmartBulb(String ip) throws IOException {
        address = InetAddress.getByName(ip);
        this.ip = ip;

        udpSocket = new DatagramSocket();
        udpSocket.connect(address, PORT);

        udpThread = new Thread(UDP_READ_THREAD);
        udpThread.start();
    }

    public void turnOn() {
        turnOn(0);
    }

    public void turnOff() {
        turnOff(0);
    }

    public void setBrightness(int brightness) {
        setBrightness(brightness, 0);
    }

    public void setColor(int brightness, int hue, int saturation) {
        setColor(brightness, hue, saturation, 0);
    }

    public void setColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

        setColor((int)(hsb[2] * 100), (int)(hsb[0] * 360), (int)(hsb[1] * 100));
    }

    public void turnOn(int transitionPeriod) {
        BulbAction.On on = new BulbAction.On();
        on.setTransitionPeriod(transitionPeriod);

        performAction(on);
    }


    public void turnOff(int transitionPeriod) {
        BulbAction.Off off = new BulbAction.Off();
        off.setTransitionPeriod(transitionPeriod);
        performAction(off);
    }


    public void setBrightness(int brightness, int transitionPeriod) {
        if (brightness < 0 || brightness > 100)
            throw new IllegalArgumentException("brightness must be between 0-100");

        BulbAction.Brightness action = new BulbAction.Brightness(brightness);
        action.setTransitionPeriod(transitionPeriod);
        performAction(action);
    }

    public void setColor(int brightness, int hue, int saturation, int transitionPeriod) {
        if (brightness < 0 || brightness > 100)
            throw new IllegalArgumentException("brightness must be between 0-100");

        if (hue < 0 || hue > 360)
            throw new IllegalArgumentException("hue must be between 0-360");

        if (saturation < 0 || saturation > 100)
            throw new IllegalArgumentException("saturation must be between 0-100");

        BulbAction.Color action = new BulbAction.Color(brightness, hue, saturation);
        action.setTransitionPeriod(transitionPeriod);
        performAction(action);
    }

    public void performAction(BulbAction.Action action) {
        BulbAction toSend = new BulbAction();
        toSend.addParameter(action);

        try {
            String json = toSend.toJson();
            logger.debug("Sending action to " + ip + ":" + PORT + ":\n" + json);

            sendUDPMessage(toSend.toJson());
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public void sendUDPMessage(String json) throws IOException {
        byte[] data = encrypt(json.getBytes());

        DatagramPacket packet = new DatagramPacket(data, 0, data.length, address, PORT);
        udpSocket.send(packet);
    }

    public void onUDPMessage(byte[] data) { }

    private final Runnable UDP_READ_THREAD = new Runnable() {
        @Override
        public void run() {
            Thread.currentThread().setName("UDP-Read-Thread");
            while (udpSocket.isConnected()) {
                try {
                    byte[] buffer = new byte[2048];

                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    byte[] decrypted = decryptUDP(packet.getData());

                    logger.debug("[UDP] Response from " + ip + ":" + PORT + ":\n" + new String(decrypted));

                    onUDPMessage(decrypted);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public static byte[] encrypt(byte[] source) {
        return xor(source, false, 0);
    }

    public static byte[] decryptUDP(byte[] source) {
        return xor(source, true, 0);
    }

    public static byte[] decryptTCP(byte[] source) {
        return xor(source, true, 4);
    }

    public static byte[] xor(byte[] source, boolean isEncrypted, int bytesToSkip) {
        long key = 171;
        int size = source.length;
        ByteBuffer buf = ByteBuffer.allocate(source.length).put(source);
        buf.position(0);
        ByteBuffer dest = ByteBuffer.allocate(size - bytesToSkip);
        int bytesRead = bytesToSkip;

        while (bytesRead < size) {
            long nextByte = buf.getLong(bytesRead);

            dest.putLong(bytesRead - bytesToSkip, nextByte^key);

            key = isEncrypted ? nextByte : nextByte^key;

            bytesRead += 8;
        }

        return dest.array();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
