package org.dflib.jjava.jupyter.kernel.display;

import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.dflib.jjava.jupyter.kernel.util.InheritanceIterator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A default renderer may be set and maps a group to a specific subtype.
 * <p>
 * A suffix may be mapped to a type.
 * <p>
 * A type has a default mime type (may be a list) and is always also rendered
 * as text/plain with toString().
 * <p>
 * A type must also have other supported types.
 * <p>
 * Objects that implement the render interface override their default renders
 * but in the event that renderAs (or displayAs) is invoked the specified types
 * override the defaults.
 */
public class Renderer {
    private static class RenderFunctionProps {
        private final RenderFunction function;
        private final Set<MIMEType> supportedTypes;
        private final Set<MIMEType> preferredTypes;

        public RenderFunctionProps(RenderFunction function, Set<MIMEType> supportedTypes, Set<MIMEType> preferredTypes) {
            this.function = function;
            this.supportedTypes = supportedTypes;
            this.preferredTypes = preferredTypes;
        }

        public RenderFunction getFunction() {
            return function;
        }

        public Set<MIMEType> getSupportedTypes() {
            return supportedTypes;
        }

        public Set<MIMEType> getPreferredTypes() {
            return preferredTypes;
        }
    }

    public class RenderRegistration<T> {
        private final Set<MIMEType> supported;
        private final Set<MIMEType> preferred;
        private final Set<Class<? extends T>> types;

        public RenderRegistration(Class<? extends T> type) {
            this.supported = new LinkedHashSet<>();
            this.preferred = new LinkedHashSet<>();
            this.types = new LinkedHashSet<>();
            this.types.add(type);
        }

        public RenderRegistration<T> supporting(MIMEType... types) {
            Collections.addAll(this.supported, types);
            return this;
        }

        public RenderRegistration<T> preferring(MIMEType... types) {
            supporting(types);
            Collections.addAll(this.preferred, types);
            return this;
        }

        public RenderRegistration<T> supporting(String... types) {
            for (String type : types)
                this.supported.add(MIMEType.parse(type));
            return this;
        }

        public RenderRegistration<T> preferring(String... types) {
            supporting(types);
            for (String type : types)
                this.preferred.add(MIMEType.parse(type));
            return this;
        }

        public RenderRegistration<T> onType(Class<? extends T> type) {
            this.types.add(type);
            return this;
        }

        public void register(RenderFunction<T> function) {
            Set<MIMEType> supported = this.supported.isEmpty() ? DisplayDataRenderable.ANY : this.supported;
            Set<MIMEType> preferred = this.preferred.isEmpty() ? supported : this.preferred;
            Renderer.this.register(supported, preferred, types, function);
        }
    }

    private final Map<Class, List<RenderFunctionProps>> renderFunctions;
    private final Map<String, MIMEType> suffixMappings;

    public Renderer() {
        this.renderFunctions = new HashMap<>();
        this.suffixMappings = new HashMap<>();
    }

    public <T> RenderRegistration<T> createRegistration(Class<T> type) {
        return new RenderRegistration<>(type);
    }

    public <T> void register(Set<MIMEType> supported, Set<MIMEType> preferred, Set<Class<? extends T>> types, RenderFunction<T> function) {
        RenderFunctionProps props = new RenderFunctionProps(function, supported, preferred);

        types.forEach(c -> this.renderFunctions.compute(c, (k, v) -> {
            List<RenderFunctionProps> functions = v != null ? v : new LinkedList<>();
            functions.add(props);
            return functions;
        }));
    }

    private static DisplayData finalizeDisplayData(DisplayData data, Object value) {
        if (!data.hasDataForType(MIMEType.TEXT_PLAIN))
            data.putText(String.valueOf(value));

        return data;
    }

    /**
     * Render the object with the preferred render type.
     * <p>
     * The rendering algorithm is as follows:
     * <ol>
     * <li>
     * The object is rendered as {@code text/plain} with {@link String#valueOf(Object)}.
     * </li>
     * <li>
     * If the object is {@link DisplayDataRenderable} ask it to render itself as the {@link DisplayDataRenderable#getPreferredRenderTypes() preferred types}.
     * </li>
     * <li>
     * Else iterate over the implemented with the {@link InheritanceIterator} until a render function is found. Use this
     * function to render the object.
     * </li>
     * </ol>
     *
     * @param value  the object to render.
     * @param params a map of parameters that render functions may use.
     *
     * @return the data container holding the rendered view of the {@code value}.
     */
    @SuppressWarnings("unchecked")
    public DisplayData render(Object value, Map<String, Object> params) {
        DisplayData out = new DisplayData();

        if (value instanceof DisplayDataRenderable) {
            DisplayDataRenderable renderable = (DisplayDataRenderable) value;

            RenderRequestTypes.Builder requestTypes = new RenderRequestTypes.Builder(this.suffixMappings::get);
            requestTypes.withType(MIMEType.TEXT_PLAIN);
            renderable.getPreferredRenderTypes().forEach(requestTypes::withType);

            renderable.render(new RenderContext(requestTypes.build(), this, params, out));

            return finalizeDisplayData(out, value);
        }

        Iterator<Class> inheritedTypes = new InheritanceIterator(value.getClass());
        while (inheritedTypes.hasNext()) {
            Class type = inheritedTypes.next();

            List<RenderFunctionProps> allRenderFunctionProps = this.renderFunctions.get(type);
            if (allRenderFunctionProps != null && !allRenderFunctionProps.isEmpty()) {
                for (RenderFunctionProps renderFunctionProps : allRenderFunctionProps) {
                    RenderRequestTypes.Builder requestTypes = new RenderRequestTypes.Builder(this.suffixMappings::get);
                    requestTypes.withType(MIMEType.TEXT_PLAIN);
                    renderFunctionProps.getPreferredTypes().forEach(requestTypes::withType);

                    renderFunctionProps.getFunction().render(
                            value,
                            new RenderContext(requestTypes.build(), this, params, out)
                    );
                }

                return finalizeDisplayData(out, value);
            }
        }

        return finalizeDisplayData(out, value);
    }

    /**
     * A {@link #render(Object, Map)} variant that supplies an empty parameter map.
     *
     * @param value the object to render.
     *
     * @return a {@link DisplayData} container with all the rendered data.
     */
    public DisplayData render(Object value) {
        return render(value, new LinkedHashMap<>());
    }

    /**
     * Render the object as the specified types if possible.
     * <p>
     * The rendering algorithm is as follows:
     * <ol>
     * <li>
     * The object is rendered as {@code text/plain} with {@link String#valueOf(Object)} no
     * matter what types are requested.
     * </li>
     * <li>
     * If the object is {@link DisplayDataRenderable} and any of it's {@link DisplayDataRenderable#getSupportedRenderTypes() supported types}
     * are requested, it is asked to render itself.
     * </li>
     * <li>
     * While all of the requested types have not be rendered yet:
     * <ol>
     * <li>
     * For every type in the {@link InheritanceIterator}, apply the same scheme as step 2.
     * </li>
     * <li>
     * Remove all rendered types from the request.
     * </li>
     * </ol>
     * </li>
     * </ol>
     *
     * @param value  the object to render.
     * @param params a map of parameters that render functions may use.
     * @param types  the {@link MIMEType#parse(String) MIME types} to render the object as.
     *
     * @return a {@link DisplayData} container with all the rendered data.
     */
    @SuppressWarnings("unchecked")
    public DisplayData renderAs(Object value, Map<String, Object> params, String... types) {
        DisplayData out = new DisplayData();

        RenderRequestTypes.Builder builder = new RenderRequestTypes.Builder(this.suffixMappings::get);
        builder.withType(MIMEType.TEXT_PLAIN);
        for (String type : types)
            builder.withType(type);

        RenderRequestTypes requestTypes = builder.build();
        RenderContext context = new RenderContext(requestTypes, this, params, out);

        if (value instanceof DisplayDataRenderable) {
            DisplayDataRenderable renderable = (DisplayDataRenderable) value;
            if (requestTypes.anyRequestedIsSupported(renderable.getSupportedRenderTypes())) {
                renderable.render(context);
                requestTypes.removeFulfilledRequests(out);
            }
        }

        Iterator<Class> inheritedTypes = new InheritanceIterator(value.getClass());
        while (inheritedTypes.hasNext() && !requestTypes.isEmpty()) {
            Class type = inheritedTypes.next();
            List<RenderFunctionProps> allRenderFunctionProps = this.renderFunctions.get(type);
            if (allRenderFunctionProps != null) {
                for (RenderFunctionProps renderFunctionProps : allRenderFunctionProps) {
                    if (requestTypes.anyRequestedIsSupported(renderFunctionProps.getSupportedTypes())) {
                        renderFunctionProps.getFunction().render(value, context);
                        requestTypes.removeFulfilledRequests(out);
                    }
                }
            }
        }

        return finalizeDisplayData(out, value);
    }

    /**
     * A {@link #renderAs(Object, Map, String...)} variant that supplies an empty parameter map.
     *
     * @param value the object to render.
     * @param types the {@link MIMEType#parse(String) MIME types} to render the object as.
     *
     * @return a {@link DisplayData} container with all the rendered data.
     */
    public DisplayData renderAs(Object value, String... types) {
        return this.renderAs(value, new LinkedHashMap<>(), types);
    }
}
