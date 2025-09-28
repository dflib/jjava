package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.ExtensionLoader;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.history.HistoryManager;
import org.dflib.jjava.jupyter.kernel.magic.CellMagic;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicParser;
import org.dflib.jjava.jupyter.kernel.magic.MagicsRegistry;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
import org.dflib.jjava.jupyter.kernel.util.TextColor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A common builder superclass for BaseKernel subclasses.
 */
public abstract class BaseKernelBuilder<
        B extends BaseKernelBuilder<B, K>,
        K extends BaseKernel> {

    protected String name;
    protected String version;
    protected Charset charset;
    protected MagicParser magicParser;
    protected HistoryManager historyManager;
    protected final Map<String, LineMagic<?, ?>> lineMagics;
    protected final Map<String, CellMagic<?, ?>> cellMagics;

    protected BaseKernelBuilder() {
        this.cellMagics = new LinkedHashMap<>();
        this.lineMagics = new LinkedHashMap<>();
    }

    public B name(String name) {
        this.name = name;
        return (B) this;
    }

    public B version(String version) {
        this.version = version;
        return (B) this;
    }

    public B charset(Charset charset) {
        this.charset = charset;
        return (B) this;
    }

    public B historyManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
        return (B) this;
    }

    public B lineMagic(String name, LineMagic<?, ?> magic) {
        lineMagics.put(name, magic);
        return (B) this;
    }

    public B cellMagic(String name, CellMagic<?, ?> magic) {
        cellMagics.put(name, magic);
        return (B) this;
    }

    public B magicParser(MagicParser magicParser) {
        this.magicParser = magicParser;
        return (B) this;
    }

    public abstract K build();

    protected JupyterIO buildJupyterIO() {
        return new JupyterIO(charset != null ? charset : StandardCharsets.UTF_8);
    }

    protected CommManager buildCommManager() {
        return new CommManager();
    }

    protected Renderer buildRenderer() {
        return new Renderer();
    }

    protected List<HelpLink> buildHelpLinks() {
        return List.of(
                new HelpLink("Java tutorial", "https://docs.oracle.com/javase/tutorial/java/nutsandbolts/index.html"),
                new HelpLink("JJava homepage", "https://github.com/dflib/jjava")
        );
    }

    protected ExtensionLoader buildExtensionLoader() {
        return new ExtensionLoader();
    }

    protected MagicsRegistry buildMagicsRegistry() {
        return new MagicsRegistry(lineMagics, cellMagics);
    }

    protected StringStyler buildErrorStyler() {
        return new StringStyler.Builder()
                .addPrimaryStyle(TextColor.BOLD_RESET_FG)
                .addSecondaryStyle(TextColor.BOLD_RED_FG)
                .addHighlightStyle(TextColor.BOLD_RESET_FG)
                .addHighlightStyle(TextColor.RED_BG)

                // TODO map snippet ids to code cells and put the proper line number in the margin here
                .withLinePrefix(TextColor.BOLD_RESET_FG + "|   ")
                .build();
    }

    protected HistoryManager buildHistoryManager() {
        // null is acceptable (for now)
        return historyManager;
    }
}
