package org.dflib.jjava.jupyter.messages;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HMACGenerator {
    private static final int MASK_INT_TO_BYTE = 0xFF;
    private static final int MASK_BYTE_LOWER = 0x0F;

    public static final HMACGenerator NO_AUTH_INSTANCE = new HMACGenerator() {
        @Override
        public String calculateSignature(byte[]... messageParts) {
            return "";
        }
    };

    private final Mac mac;

    public HMACGenerator(String algorithm, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        this.mac = Mac.getInstance(algorithm.replace("-", ""));
        this.mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.US_ASCII), algorithm));
    }

    private HMACGenerator() {
        this.mac = null;
    }

    private final static char[] HEX_CHAR = "0123456789abcdef".toCharArray();

    public synchronized String calculateSignature(byte[]... messageParts) {
        for (byte[] part : messageParts)
            this.mac.update(part);

        byte[] sig = this.mac.doFinal();

        char[] hex = new char[sig.length * 2];
        for (int j = 0; j < sig.length; j++) {
            int b = sig[j] & MASK_INT_TO_BYTE;
            hex[j * 2] = HEX_CHAR[b >>> 4];
            hex[j * 2 + 1] = HEX_CHAR[b & MASK_BYTE_LOWER];
        }

        return new String(hex);
    }
}
