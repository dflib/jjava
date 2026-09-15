package org.dflib.jjava.jupyter.kernel.display.common;

import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SwingTest {

    static {
        // Snapshotting works with or without a display, but force headless so the suite behaves identically on CI
        // servers and developer machines.
        System.setProperty("java.awt.headless", "true");
    }

    private Renderer renderer;

    @BeforeEach
    public void setUp() {
        this.renderer = new Renderer();
        Swing.registerAll(this.renderer);
    }

    @Test
    public void rendersComponentAsPng() throws Exception {
        JButton button = new JButton("Click me");
        button.setSize(120, 40);

        DisplayData data = this.renderer.render(button);

        BufferedImage image = decodePng(data);
        assertEquals(120, image.getWidth());
        assertEquals(40, image.getHeight());
    }

    @Test
    public void fallsBackToPreferredSizeWhenNotSized() throws Exception {
        JLabel label = new JLabel("hello");
        Dimension preferred = label.getPreferredSize();

        DisplayData data = this.renderer.render(label);

        BufferedImage image = decodePng(data);
        assertEquals(preferred.width, image.getWidth());
        assertEquals(preferred.height, image.getHeight());
    }

    @Test
    public void snapshotHonoursRequestedSize() {
        JPanel panel = new JPanel();

        BufferedImage image = Swing.snapshot(panel, 200, 100);

        assertEquals(200, image.getWidth());
        assertEquals(100, image.getHeight());
    }

    @Test
    public void laysOutChildrenBeforePainting() {
        // A nested component that was never realized in a window: layout() must give the child non-zero bounds.
        JPanel panel = new JPanel(new BorderLayout());
        JButton child = new JButton("child");
        panel.add(child, BorderLayout.CENTER);

        Swing.snapshot(panel, 300, 80);

        assertTrue(child.getWidth() > 0, "child should have been laid out with a non-zero width");
        assertTrue(child.getHeight() > 0, "child should have been laid out with a non-zero height");
    }

    private static BufferedImage decodePng(DisplayData data) throws Exception {
        Object png = data.getData(MIMEType.IMAGE_PNG);
        assertNotNull(png, "expected an image/png rendering");
        byte[] bytes = Base64.getDecoder().decode(png.toString());
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertNotNull(image, "image/png payload should decode to an image");
        return image;
    }
}
