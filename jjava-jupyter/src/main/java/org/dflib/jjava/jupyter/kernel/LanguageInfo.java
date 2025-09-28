package org.dflib.jjava.jupyter.kernel;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class LanguageInfo {

    public static class Builder {
        private final String name;
        private String version = null;
        private String mimetype = "text/plain";
        private String fileExt = ".txt";
        private String pygmentsLexer = null;
        private Object codemirrorMode = null;
        private String exporter = null;

        public Builder(String name) {
            this.name = name;
        }

        /**
         * Set the version for the language described by this info. It
         * is recommended to be a semantic version (eg. 1.2.3)
         *
         * @param version the version string
         * @return this builder for chaining
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Set the mimetype for scripts written in this language. For example
         * {@code text/html} or {@code application/javascript}.
         *
         * @param mimetype the mimetype for scripts written in this language.
         * @return this builder for chaining
         */
        public Builder mimetype(String mimetype) {
            this.mimetype = mimetype;
            return this;
        }

        /**
         * Set the file extension for scripts written in this language. For
         * example {@code .py} or {@code .mlod}. This allows for a "Download as"
         * menu option for this language.
         *
         * @param ext the file extension <b>including the dot</b>
         * @return this builder for chaining
         */
        public Builder fileExtension(String ext) {
            this.fileExt = ext;
            return this;
        }

        /**
         * Set the {@code pygments} lexer for syntax highlighting. By default
         * it will be the language name. Use this to set it to something
         * different.
         * <p>
         * A list of the default installed lexers can be found
         * <a href="http://pygments.org/docs/lexers/">on the pygments website</a>
         *
         * @param lexer the name of the lexer
         * @return this builder for chaining
         */
        public Builder pygments(String lexer) {
            this.pygmentsLexer = lexer;
            return this;
        }

        /**
         * Set the {@code codemirror} mode for syntax highlighting in the
         * notebook. By default it will be the language name. Use this to set it
         * to something different. See <a href="https://codemirror.net/mode/">default modes</a>
         * and the <a href="https://codemirror.net/doc/manual.html#option_mode">codemirror mode option</a>
         * <p>
         * This may also be a mimetype or a language config (see {@link #codemirror(Map)})
         *
         * @param mode the code mirror mode
         * @return this builder for chaining
         */
        public Builder codemirror(String mode) {
            this.codemirrorMode = mode;
            return this;
        }

        /**
         * Set the {@code codemirror} mode for syntax highlighting in the
         * notebook. By default it will be the the language name. Use this to set it
         * to something different. For setting the mode by name use {@link #codemirror(String)}.
         * <p>
         * This is a <a href="https://codemirror.net/doc/manual.html#option_mode">language config</a>
         *
         * @param mode the code mirror mode config. Must contain a {@code "name"} key
         * @return this builder for chaining
         */
        public Builder codemirror(Map<String, Object> mode) {
            this.codemirrorMode = mode;
            return this;
        }

        /**
         * Set the exported for scripts written in this language. By default it just uses
         * the {@code "script"} exporter which exports all of the code cells into a file.
         *
         * @param exporter the name of the exporter if a custom one is also being loaded by
         *                 the kernel
         * @return this builder for chaining
         */
        public Builder exporter(String exporter) {
            this.exporter = exporter;
            return this;
        }

        public LanguageInfo build() {
            return new LanguageInfo(name, version, mimetype, fileExt, pygmentsLexer, codemirrorMode, exporter);
        }
    }

    protected final String name;

    /**
     * Semantic version string. X.Y.Z. Language version
     */
    protected final String version;

    protected String mimetype;

    @SerializedName("file_extension")
    protected String fileExtension;

    /**
     * If not defined defaults to {@link #name}
     */
    @SerializedName("pygments_lexer")
    protected String pygmentsLexer;

    /**
     * If not defined defaults to {@link #name}.
     * <p>
     * It may be a {@link String} describing the name of the lexer or the
     * MIME type. Otherwise it may be a json object with a `name` field for
     * the name/MIME type of the lexer as well as other configuration options.
     */
    @SerializedName("codemirror_mode")
    protected Object codemirrorMode;

    /**
     * If not defined defaults to the general 'script'
     */
    @SerializedName("nbconvert_exporter")
    protected String nbconvertExporter;

    public LanguageInfo(String name, String version, String mimetype, String fileExtension, String pygmentsLexer, Object codemirrorMode, String nbconvertExporter) {
        this.name = name;
        this.version = version;
        this.mimetype = mimetype;
        this.fileExtension = fileExtension;
        this.pygmentsLexer = pygmentsLexer;
        this.codemirrorMode = codemirrorMode;
        this.nbconvertExporter = nbconvertExporter;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getMimetype() {
        return mimetype;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getPygmentsLexer() {
        return pygmentsLexer;
    }

    public Object getCodemirrorMode() {
        return codemirrorMode;
    }

    public String getNbconvertExporter() {
        return nbconvertExporter;
    }
}
