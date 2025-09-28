package org.dflib.jjava.jupyter.kernel.display;

import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RenderContext {
    private final RenderRequestTypes requestedTypes;
    private final Renderer renderer;
    private final Map<String, Object> params;
    private final DisplayData out;

    public RenderContext(RenderRequestTypes requestedTypes, Renderer renderer, Map<String, Object> params, DisplayData out) {
        this.requestedTypes = requestedTypes;
        this.renderer = renderer;
        this.params = params;
        this.out = out;
    }

    public Renderer getRenderer() {
        return this.renderer;
    }

    public DisplayData getOutputContainer() {
        return this.out;
    }

    public Map<String, Object> getParams() {
        return Collections.unmodifiableMap(this.params);
    }

    public Object getParameter(String key) {
        return this.params.get(key);
    }

    public Object getParameter(String key, Object defaultValue) {
        return this.params.getOrDefault(key, defaultValue);
    }

    public String getParameterAsString(String key) {
        Object value = this.getParameter(key);
        return value == null ? null : String.valueOf(value);
    }

    public String getParameterAsString(String key, String defaultValue) {
        String value = this.getParameterAsString(key);
        return value == null ? defaultValue : value;
    }

    public Integer getParameterAsInt(String key) {
        Object value = this.getParameter(key);
        return value == null
                ? null
                : value instanceof Number
                ? ((Number) value).intValue()
                : Integer.parseInt(String.valueOf(value));
    }

    public Integer getParameterAsInt(String key, Integer defaultValue) {
        Integer value = this.getParameterAsInt(key);
        return value == null ? defaultValue : value;
    }

    public Double getParameterAsDouble(String key) {
        Object value = this.getParameter(key);
        return value == null
                ? null
                : value instanceof Number
                ? ((Number) value).doubleValue()
                : Double.parseDouble(String.valueOf(value));
    }

    public Double getParameterAsDouble(String key, Double defaultValue) {
        Double value = this.getParameterAsDouble(key);
        return value == null ? defaultValue : value;
    }

    public Boolean getParameterAsBoolean(String key) {
        Object value = this.getParameter(key);
        return value == null
                ? null
                : value instanceof Boolean
                ? (Boolean) value
                : Boolean.parseBoolean(String.valueOf(value));
    }

    public Boolean getParameterAsBoolean(String key, Boolean defaultValue) {
        Boolean value = this.getParameterAsBoolean(key);
        return value == null ? defaultValue : value;
    }

    public boolean wantsDataRenderedAs(MIMEType type) {
        return this.requestedTypes.resolveSupportedType(type) != null;
    }

    public MIMEType resolveRequestedType(MIMEType supported) {
        return this.requestedTypes.resolveSupportedType(supported);
    }

    public boolean renderIfRequested(MIMEType supportedType, BiConsumer<MIMEType, DisplayData> renderFunction) {
        MIMEType resolvedType = this.requestedTypes.resolveSupportedType(supportedType);
        if (resolvedType != null) {
            renderFunction.accept(resolvedType, this.getOutputContainer());
            return true;
        }
        return false;
    }

    public boolean renderIfRequested(MIMEType supportedType, Function<MIMEType, Object> renderFunction) {
        MIMEType resolvedType = this.requestedTypes.resolveSupportedType(supportedType);
        if (resolvedType != null) {
            this.getOutputContainer().putData(resolvedType, renderFunction.apply(resolvedType));
            return true;
        }
        return false;
    }

    public boolean renderIfRequested(MIMEType supportedType, Supplier<Object> render) {
        MIMEType resolvedType = this.requestedTypes.resolveSupportedType(supportedType);
        if (resolvedType != null) {
            this.getOutputContainer().putData(resolvedType, render.get());
            return true;
        }
        return false;
    }
}
