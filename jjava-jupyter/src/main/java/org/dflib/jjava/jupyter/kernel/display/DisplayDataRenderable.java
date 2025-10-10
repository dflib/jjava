package org.dflib.jjava.jupyter.kernel.display;

import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface DisplayDataRenderable {
    Set<MIMEType> ANY = Collections.singleton(MIMEType.ANY);

    /**
     * Specifies a set of {@link MIMEType}s that this class may be rendered as.
     * <p>
     * NOTE: Specifying the supported render types does not prevent {@link #render(RenderContext)}
     * from being invoked with other types. Implementations should handle these cases gracefully
     * with a no-op.
     * <p>
     * When used in conjunction with {@link Renderer} this annotation provides information to the
     * routing algorithm.
     * <p>
     * In particular {@link Renderer#render(Object)} will request that the object
     * is rendered as the {@link #getPreferredRenderTypes()} types.
     *
     * @return The set of {@link MIMEType}s that this object can be rendered as.
     */
    default Set<MIMEType> getSupportedRenderTypes() {
        return DisplayDataRenderable.ANY;
    }

    /**
     * Species a subset of {@link #getSupportedRenderTypes()} in which this class
     * prefers to be rendered as.
     * <p>
     * For example a class may support rendering as {@code application/json} and
     * {@code application/xml} but when given a choice should only be rendered as
     * {@code application/json}. In this case {@code getPreferredRenderTypes()} should
     * be {@code "application/json"}.
     *
     * @return a set of {@link MIMEType}s that this class
     * prefers to be rendered as.
     */
    default Set<MIMEType> getPreferredRenderTypes() {
        return this.getSupportedRenderTypes();
    }

    /**
     * Render this object into the {@link RenderContext#getOutputContainer()} based on the requested types
     * from the {@code context}. Implementations may also use the {@code context}
     * to delegate rendering.
     * <p>
     * Implementations should test if the {@link RenderContext#wantsDataRenderedAs(MIMEType)}
     * for all of the supported types and if true store the rendered data in the container
     * <strong>at the resolved MIME type, not the supported one.</strong> Use the type returned
     * by {@link RenderContext#resolveRequestedType(MIMEType)}.
     * <p>
     * For convenience implementations may use {@link RenderContext#renderIfRequested(MIMEType, BiConsumer)}
     * which streamlines these operations:
     * <pre>
     * {@code private static MIMEType PNG = MIMEType.parse("image/png");
     *     private String renderAsPNG() {...}
     *     public void render(RenderContext context) {
     *         context.renderIfRequested(PNG, (type, out) -> {
     *             out.putData(type, this.renderAsPNG());
     *         });
     *         // or to store the return value of renderAsPNG at the correct
     *         // type use
     *         context.renderIfRequested(PNG, this::renderAsPNG);
     *         // or if you need the type to make a rendering decision and then store
     *         // the return value at the correct type
     *         context.renderIfRequested(PNG, type -> this.renderAsPNG());
     *     }
     * }
     * </pre>
     *
     * @param context the context that the render is taking place in.
     */
    void render(RenderContext context);
}
