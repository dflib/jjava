package org.dflib.jjava.jupyter.channels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.dflib.jjava.jupyter.kernel.ExpressionValue;
import org.dflib.jjava.jupyter.kernel.KernelConnectionProperties;
import org.dflib.jjava.jupyter.kernel.history.HistoryEntry;
import org.dflib.jjava.jupyter.messages.HMACGenerator;
import org.dflib.jjava.jupyter.messages.Header;
import org.dflib.jjava.jupyter.messages.KernelTimestamp;
import org.dflib.jjava.jupyter.messages.Message;
import org.dflib.jjava.jupyter.messages.MessageType;
import org.dflib.jjava.jupyter.messages.ReplyType;
import org.dflib.jjava.jupyter.messages.adapters.ExpressionValueAdapter;
import org.dflib.jjava.jupyter.messages.adapters.HeaderAdapter;
import org.dflib.jjava.jupyter.messages.adapters.HistoryEntryAdapter;
import org.dflib.jjava.jupyter.messages.adapters.HistoryRequestAdapter;
import org.dflib.jjava.jupyter.messages.adapters.KernelTimestampAdapter;
import org.dflib.jjava.jupyter.messages.adapters.MessageTypeAdapter;
import org.dflib.jjava.jupyter.messages.adapters.PublishStatusAdapter;
import org.dflib.jjava.jupyter.messages.adapters.ReplyTypeAdapter;
import org.dflib.jjava.jupyter.messages.publish.PublishStatus;
import org.dflib.jjava.jupyter.messages.reply.ErrorReply;
import org.dflib.jjava.jupyter.messages.request.HistoryRequest;
import org.slf4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class JupyterSocket extends ZMQ.Socket {

    protected static String formatAddress(String transport, String ip, int port) {
        return transport + "://" + ip + ":" + port;
    }

    // Comes from a Python bytestring
    private static final byte[] IDENTITY_BLOB_DELIMITER = "<IDS|MSG>".getBytes(StandardCharsets.US_ASCII);

    private static final Gson replyGson = new GsonBuilder()
            .registerTypeAdapter(HistoryEntry.class, HistoryEntryAdapter.INSTANCE)
            .registerTypeAdapter(ExpressionValue.class, ExpressionValueAdapter.INSTANCE)
            .create();

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(KernelTimestamp.class, KernelTimestampAdapter.INSTANCE)
            .registerTypeAdapter(Header.class, HeaderAdapter.INSTANCE)
            .registerTypeAdapter(MessageType.class, MessageTypeAdapter.INSTANCE)
            .registerTypeAdapter(PublishStatus.class, PublishStatusAdapter.INSTANCE)
            .registerTypeAdapter(HistoryRequest.class, HistoryRequestAdapter.INSTANCE)
            .registerTypeHierarchyAdapter(ReplyType.class, new ReplyTypeAdapter(replyGson))
            .create();
    private static final byte[] EMPTY_JSON_OBJECT = "{}".getBytes(StandardCharsets.UTF_8);
    private static final Type JSON_OBJ_AS_MAP = new TypeToken<Map<String, Object>>() {
    }.getType();

    protected final ZMQ.Context ctx;
    protected final HMACGenerator hmacGenerator;
    protected final Logger logger;
    protected boolean closed;

    protected JupyterSocket(ZMQ.Context context, SocketType type, HMACGenerator hmacGenerator, Logger logger) {
        super(context, type);
        this.ctx = context;
        this.hmacGenerator = hmacGenerator;
        this.logger = logger;
        this.closed = false;
    }

    public abstract void bind(KernelConnectionProperties connProps);

    public synchronized Message<?> readMessage() {
        if (this.closed)
            return null;

        List<byte[]> identities = new ArrayList<>();
        byte[] identity = super.recv();
        while (!Arrays.equals(IDENTITY_BLOB_DELIMITER, identity)) {
            identities.add(identity);
            identity = super.recv();
        }

        //A hex string
        String receivedSig = super.recvStr();

        byte[] headerRaw = super.recv();
        byte[] parentHeaderRaw = super.recv();
        byte[] metadataRaw = super.recv();
        byte[] contentRaw = super.recv();

        List<byte[]> blobs = new ArrayList<>();
        while (super.hasReceiveMore()) {
            blobs.add(super.recv());
        }

        String calculatedSig = hmacGenerator.calculateSignature(headerRaw, parentHeaderRaw, metadataRaw, contentRaw);

        if (calculatedSig != null && !calculatedSig.equals(receivedSig)) {
            throw new SecurityException("Message received had invalid signature");
        }

        Header<?> header = gson.fromJson(new String(headerRaw, StandardCharsets.UTF_8), Header.class);

        Header<?> parentHeader = null;
        JsonElement parentHeaderJson = JsonParser.parseString(new String(parentHeaderRaw, StandardCharsets.UTF_8));
        if (parentHeaderJson.isJsonObject() && !parentHeaderJson.getAsJsonObject().isEmpty()) {
            parentHeader = gson.fromJson(parentHeaderJson, Header.class);
        }

        Map<String, Object> metadata = gson.fromJson(new String(metadataRaw, StandardCharsets.UTF_8), JSON_OBJ_AS_MAP);
        Object content = gson.fromJson(new String(contentRaw, StandardCharsets.UTF_8), header.getType().getContentType());
        if (content instanceof ErrorReply) {
            header = new Header<>(
                    header.getId(),
                    header.getUsername(),
                    header.getSessionId(),
                    header.getTimestamp(),
                    header.getType().error(),
                    header.getVersion());
        }

        Message<?> message = new Message(identities, header, parentHeader, metadata, content, blobs);

        if (logger.isTraceEnabled()) {
            logger.trace("Received from {}:\n{}", super.base().getSocketOptx(zmq.ZMQ.ZMQ_LAST_ENDPOINT), gson.toJson(message));
        }

        return message;
    }

    public <T> Message<T> readMessage(MessageType<T> type) {
        Message<?> message = readMessage();
        if (message.getHeader().getType() != type) {
            throw new RuntimeException("Expected a " + type + " message but received a " + message.getHeader().getType() + " message.");
        }
        return (Message<T>) message;
    }

    public synchronized void sendMessage(Message<?> message) {
        if (this.closed)
            return;

        byte[] headerRaw = gson.toJson(message.getHeader()).getBytes(StandardCharsets.UTF_8);
        byte[] parentHeaderRaw = message.hasParentHeader()
                ? gson.toJson(message.getParentHeader()).getBytes(StandardCharsets.UTF_8)
                : EMPTY_JSON_OBJECT;
        byte[] metadata = message.hasMetadata()
                ? gson.toJson(message.getMetadata()).getBytes(StandardCharsets.UTF_8)
                : EMPTY_JSON_OBJECT;
        byte[] content = gson.toJson(message.getContent()).getBytes(StandardCharsets.UTF_8);

        String hmac = hmacGenerator.calculateSignature(headerRaw, parentHeaderRaw, metadata, content);

        if (logger.isTraceEnabled()) {
            logger.trace("Sending to {}:\n{}", super.base().getSocketOptx(zmq.ZMQ.ZMQ_LAST_ENDPOINT), gson.toJson(message));
        }

        message.getIdentities().forEach(super::sendMore);
        super.sendMore(IDENTITY_BLOB_DELIMITER);
        super.sendMore(hmac.getBytes(StandardCharsets.US_ASCII));
        super.sendMore(headerRaw);
        super.sendMore(parentHeaderRaw);
        super.sendMore(metadata);

        if (message.getBlobs() == null)
            super.send(content);
        else {
            super.sendMore(content);
            //The last call needs to be a "send" call so as long as "blobs.hasNext()"
            //there will be something sent later and so the call needs to be "sendMore"
            Iterator<byte[]> blobs = message.getBlobs().iterator();
            byte[] blob;
            while (blobs.hasNext()) {
                blob = blobs.next();
                if (blobs.hasNext())
                    super.sendMore(blob);
                else
                    super.send(blob);
            }
        }
    }

    @Override
    public void close() {
        super.close();
        this.closed = true;
    }

    public void waitUntilClose() {
    }
}
