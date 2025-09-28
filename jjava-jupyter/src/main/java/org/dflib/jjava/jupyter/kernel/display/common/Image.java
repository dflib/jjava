package org.dflib.jjava.jupyter.kernel.display.common;

import org.dflib.jjava.jupyter.kernel.display.RenderContext;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class Image {
    public static final MIMEType PNG = MIMEType.IMAGE_PNG;
    public static final MIMEType JPEG = MIMEType.IMAGE_JPEG;
    public static final MIMEType GIF = MIMEType.IMAGE_GIF;
    public static final MIMEType SVG = MIMEType.IMAGE_SVG;

    public static void registerAll(Renderer renderer) {
        renderer.createRegistration(java.awt.image.RenderedImage.class)
                .preferring(PNG)
                .supporting(JPEG, GIF)
                .register(Image::renderImage);
        renderer.createRegistration(InputStream.class)
                .preferring(PNG)
                .supporting(JPEG, GIF)
                .register(Image::renderImageFromStream);
    }

    private static String imageTob64(java.awt.image.RenderedImage image, String fmt) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            ImageIO.write(image, fmt, Base64.getEncoder().wrap(out));

            return out.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void renderImage(java.awt.image.RenderedImage data, RenderContext context) {
        context.renderIfRequested(PNG, () -> imageTob64(data, "png"));
        context.renderIfRequested(JPEG, () -> imageTob64(data, "jpeg"));
        context.renderIfRequested(GIF, () -> imageTob64(data, "gif"));
    }

    public static void renderImageFromStream(InputStream data, RenderContext context) {
        try {
            renderImage(ImageIO.read(data), context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
