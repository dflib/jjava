package org.dflib.jjava.jupyter.kernel;

import org.dflib.jjava.jupyter.ExtensionLoader;
import org.dflib.jjava.jupyter.kernel.comm.CommManager;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.history.HistoryManager;
import org.dflib.jjava.jupyter.kernel.magic.CellMagic;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicParser;
import org.dflib.jjava.jupyter.kernel.magic.MagicTranspiler;
import org.dflib.jjava.jupyter.kernel.magic.MagicsRegistry;
import org.dflib.jjava.jupyter.kernel.util.StringStyler;
import org.dflib.jjava.jupyter.kernel.util.TextColor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A common builder superclass for BaseKernel subclasses.
 */
public abstract class BaseKernelBuilder<
        B extends BaseKernelBuilder<B, K>,
        K extends BaseKernel> {

    protected String name;
    protected String version;
    protected Charset jupyterIOEncoding;
    protected MagicParser magicParser;
    protected MagicTranspiler magicTranspiler;
    protected HistoryManager historyManager;
    protected Boolean extensionsEnabled;
    protected final MagicsRegistry magicsRegistry;

    protected BaseKernelBuilder() {
        this.magicsRegistry = new MagicsRegistry();
    }

    public B name(String name) {
        this.name = name;
        return (B) this;
    }

    public B version(String version) {
        this.version = version;
        return (B) this;
    }

    public B jupyterIOEncoding(Charset jupyterIOEncoding) {
        this.jupyterIOEncoding = jupyterIOEncoding;
        return (B) this;
    }

    public B historyManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
        return (B) this;
    }

    public B lineMagic(String name, LineMagic<?, ?> magic) {
        magicsRegistry.registerLineMagic(name, magic);
        return (B) this;
    }

    public B cellMagic(String name, CellMagic<?, ?> magic) {
        magicsRegistry.registerCellMagic(name, magic);
        return (B) this;
    }

    public B magicParser(MagicParser magicParser) {
        this.magicParser = magicParser;
        return (B) this;
    }

    public B magicTranspiler(MagicTranspiler magicTranspiler) {
        this.magicTranspiler = magicTranspiler;
        return (B) this;
    }

    public B extensionsEnabled(boolean extensionsEnabled) {
        this.extensionsEnabled = extensionsEnabled;
        return (B) this;
    }

    public abstract K build();

    protected String buildName() {
        return name != null ? name : "";
    }

    protected String buildVersion() {
        return version != null ? version : "";
    }

    protected Charset buildJupyterIOEncoding() {
        return jupyterIOEncoding != null ? jupyterIOEncoding : StandardCharsets.UTF_8;
    }

    protected JupyterIO buildJupyterIO(Charset encoding) {
        return new JupyterIO(encoding);
    }

    protected CommManager buildCommManager() {
        return new CommManager();
    }

    protected Renderer buildRenderer() {
        return new Renderer();
    }

    protected ExtensionLoader buildExtensionLoader() {
        return new ExtensionLoader();
    }

    protected boolean buildExtensionsEnabled() {
        return extensionsEnabled != null ? extensionsEnabled : true;
    }

    protected MagicsRegistry buildMagicsRegistry() {
        return magicsRegistry;
    }

    protected MagicTranspiler buildMagicTranspiler() {
        return magicTranspiler != null ? magicTranspiler : new MagicTranspiler();
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
