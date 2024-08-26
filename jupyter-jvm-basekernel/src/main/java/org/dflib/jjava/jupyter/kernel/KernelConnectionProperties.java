package org.dflib.jjava.jupyter.kernel;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.messages.HMACGenerator;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class KernelConnectionProperties {

    public static KernelConnectionProperties parse(String raw) {
        return new Gson().fromJson(raw, KernelConnectionProperties.class);
    }

    private String ip;

    @SerializedName("control_port")
    private int controlPort;
    @SerializedName("shell_port")
    private int shellPort;
    @SerializedName("stdin_port")
    private int stdinPort;
    @SerializedName("hb_port")
    private int hbPort;
    @SerializedName("iopub_port")
    private int iopubPort;

    private String transport;

    @SerializedName("signature_scheme")
    private String signatureScheme;
    private String key;

    private KernelConnectionProperties() {
    }

    public KernelConnectionProperties(String ip, int controlPort, int shellPort, int stdinPort, int hbPort, int iopubPort, String transport, String signatureScheme, String key) {
        this.ip = ip;
        this.controlPort = controlPort;
        this.shellPort = shellPort;
        this.stdinPort = stdinPort;
        this.hbPort = hbPort;
        this.iopubPort = iopubPort;
        this.transport = transport;
        this.signatureScheme = signatureScheme;
        this.key = key;
    }

    public String getIp() {
        return ip;
    }

    public int getControlPort() {
        return controlPort;
    }

    public int getShellPort() {
        return shellPort;
    }

    public int getStdinPort() {
        return stdinPort;
    }

    public int getHbPort() {
        return hbPort;
    }

    public int getIopubPort() {
        return iopubPort;
    }

    public String getTransport() {
        return transport;
    }

    public String getSignatureScheme() {
        return signatureScheme;
    }

    public String getKey() {
        return key;
    }

    public HMACGenerator createHMACGenerator() throws InvalidKeyException, NoSuchAlgorithmException {
        if (key == null || key.isEmpty())
            return HMACGenerator.NO_AUTH_INSTANCE;
        else
            return new HMACGenerator(signatureScheme, key);
    }

    public String toJsonString() {
        return new Gson().toJson(this);
    }

    @Override
    public String toString() {
        return "KernelConnectionProperties{" +
                "ip='" + ip + '\'' +
                ", controlPort=" + controlPort +
                ", shellPort=" + shellPort +
                ", stdinPort=" + stdinPort +
                ", hbPort=" + hbPort +
                ", iopubPort=" + iopubPort +
                ", transport='" + transport + '\'' +
                ", signatureScheme='" + signatureScheme + '\'' +
                ", key='" + key + '\'' +
                '}';
    }
}
