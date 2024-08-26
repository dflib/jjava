package org.dflib.jjava.jupyter.kernel.display;

import com.google.gson.annotations.SerializedName;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DisplayData {
    public static final String DISPLAY_ID_KEY = "display_id";

    public static final DisplayData EMPTY = new DisplayData(Collections.emptyMap());

    public static final DisplayData EMPTY_STRING = new DisplayData("");

    public static DisplayData emptyIfNull(DisplayData bundle) {
        return bundle == null ? EMPTY : bundle;
    }

    private final Map<String, Object> data;

    private Map<String, Object> metadata = new LinkedHashMap<>();

    @SerializedName("transient")
    private Map<String, Object> transientData = null;

    private DisplayData(Map<String, Object> data) {
        this.data = data;
    }

    public DisplayData(DisplayData that) {
        this.data = that.data;
        this.metadata = that.metadata;
        this.transientData = that.transientData;
    }

    public DisplayData(String textData) {
        this();
        this.putText(textData);
    }

    public DisplayData() {
        this(new LinkedHashMap<>());
    }

    private void ensureTransientDataInitialized() {
        if (this.transientData == null)
            this.transientData = new LinkedHashMap<>();
    }

    public void putData(String mimeType, Object data) {
        this.data.put(mimeType, data);
    }

    public void putMetaData(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * Add a data point (key-value pair) to the transient data dictionary. This is
     * not always applicable but in such cases there is no harm in adding it but the
     * front-end may just ignore it.
     * <p>
     * As of writing this the only transient data key supported by iPython is
     * {@code display_id} which is only used in the {@code display_data} and
     * {@code update_display_data} messages.
     * <p>
     * Third parties may utilize this for other purposes as well.
     *
     * @param key   the data key
     * @param value the data value
     */
    public void putTransientData(String key, Object value) {
        this.ensureTransientDataInitialized();
        this.transientData.put(key, value);
    }

    public void putData(MIMEType type, Object data) {
        this.putData(type.toString(), data);
    }

    public void putMetaData(MIMEType type, Object data) {
        this.putMetaData(type.toString(), data);
    }

    public void putData(MIMEType type, Object data, Object metadata) {
        this.putData(type, data);
        this.putMetaData(type, metadata);
    }

    public Object getData(MIMEType type) {
        return this.data.get(type.toString());
    }

    public boolean hasDataForType(MIMEType type) {
        return this.data.containsKey(type.toString());
    }

    public void assign(DisplayData data) {
        this.data.putAll(data.data);

        if (this.metadata == null)
            this.metadata = data.metadata;
        else if (data.metadata != null)
            this.metadata.putAll(data.metadata);

        if (this.transientData == null)
            this.transientData = data.transientData;
        else if (data.transientData != null)
            this.transientData.putAll(data.transientData);
    }

    public void setDisplayId(String id) {
        this.putTransientData(DISPLAY_ID_KEY, id);
    }

    public boolean hasDisplayId() {
        return this.transientData != null
                && this.transientData.containsKey(DISPLAY_ID_KEY);
    }

    public String getDisplayId() {
        if (this.transientData == null) return null;

        Object id = this.transientData.get(DISPLAY_ID_KEY);
        if (id == null) return null;

        return String.valueOf(id);
    }

    public void putText(String text) {
        this.putData("text/plain", text);
    }

    public void putHTML(String html) {
        this.putData("text/html", html);
    }

    public void putLatex(String latex) {
        this.putData("text/latex", latex);
    }

    /**
     * Add some latex math to the output.
     *
     * @param math the latex math code EXCLUDING the starting and
     *             trailing {@code $$} or other latex math mode switchers
     */
    public void putMath(String math) {
        this.putLatex("$$" + math + "$$");
    }

    public void putMarkdown(String markdown) {
        this.putData("text/markdown", markdown);
    }

    public void putJavaScript(String javascript) {
        this.putData("application/javascript", javascript);
    }

    public void putJSON(String json) {
        this.putData("application/json", json);
    }

    public void putJSON(String json, boolean expanded) {
        this.putJSON(json);
        this.putMetaData("expanded", expanded);
    }

    public void putSVG(String svg) {
        this.putData("image/svg+xml", svg);
    }
}
