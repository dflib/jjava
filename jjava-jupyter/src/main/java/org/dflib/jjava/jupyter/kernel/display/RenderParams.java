package org.dflib.jjava.jupyter.kernel.display;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A utility class for inline map construction for use in the context of rendering.
 *
 * See: {@link Renderer#render(Object, Map)} and {@link Renderer#renderAs(Object, Map, String...)}
 * which take a parameter map.
 */
public class RenderParams extends LinkedHashMap<String, Object> {
    //TODO use the path map from MellowD to support a getAll query or one with wildcard patterns
    public static class Param<T> {
        public final String key;
        public final T value;

        public Param(String key, T value) {
            this.key = key;
            this.value = value;
        }
    }

    public static <T> Param<T> param(String key, T value) {
        return new Param<>(key, value);
    }

    public static RenderParams paramsOf(Param... params) {
        RenderParams renderParams = new RenderParams();
        for (Param p : params)
            renderParams.put(p.key, p.value);
        return renderParams;
    }

    public static RenderParams paramsOf(String key, Object value) {
        RenderParams renderParams = new RenderParams();
        renderParams.put(key, value);
        return renderParams;
    }

    public RenderParams with(String key, Object value) {
        this.put(key, value);
        return this;
    }

    public RenderParams with(Param param) {
        this.put(param.key, param.value);
        return this;
    }

    public RenderParams and(String key, Object value) {
        return with(key, value);
    }

    public RenderParams and(Param param) {
        return with(param);
    }
}
