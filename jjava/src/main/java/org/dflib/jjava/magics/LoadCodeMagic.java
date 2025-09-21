package org.dflib.jjava.magics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.dflib.jjava.jupyter.kernel.BaseKernel;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class LoadCodeMagic implements LineMagic<Void> {

    private static final String NOTEBOOK_EXTENSION = ".ipynb";

    // TODO: ThreadLocals can result in memory leaks. Also Gson instances seem to be thread-safe and not require
    //   explicit safety mechanisms
    private static final ThreadLocal<Gson> GSON = ThreadLocal.withInitial(() -> new GsonBuilder().create());

    private static final MagicsArgs LOAD_ARGS = MagicsArgs.builder()
            .required("source")
            .onlyKnownFlags()
            .onlyKnownKeywords()
            .build();

    private final BaseKernel kernel;
    private final String[] fileExtensions;

    public LoadCodeMagic(BaseKernel kernel, String... fileExtensions) {
        this.kernel = kernel;
        this.fileExtensions = fileExtensions;
    }

    @Override
    public Void execute(List<String> args) throws Exception {

        Map<String, List<String>> vals = LOAD_ARGS.parse(args);
        Path sourcePath = Paths.get(vals.get("source").get(0)).toAbsolutePath();

        String file = sourcePath.getFileName().toString();

        // Try and see if adding any of the supported extensions gives a file.
        for (String extension : fileExtensions) {
            Path scriptPath = sourcePath.resolveSibling(file + extension);
            if (Files.isRegularFile(scriptPath)) {

                if (scriptPath.getFileName().endsWith(NOTEBOOK_EXTENSION)) {
                    execNotebook(scriptPath);
                } else {
                    String sourceContents = Files.readString(scriptPath);
                    kernel.eval(sourceContents);
                }
                return null;
            }
        }

        String exts = String.join(", ", fileExtensions);
        throw new FileNotFoundException(
                "Could not find any source at '" + sourcePath + "'. Also tried with extensions: [" + exts + "].");
    }

    // This slightly verbose implementation is designed to take advantage of gson as a streaming parser
    // in which we can only take what we need on the fly and pass each cell to the handler without needing
    // to keep the entire notebook in memory.
    // This should be a big help for larger notebooks.
    private void execNotebook(Path notebookPath) throws Exception {
        try (Reader in = Files.newBufferedReader(notebookPath, StandardCharsets.UTF_8)) {
            JsonReader reader = GSON.get().newJsonReader(in);
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (!name.equals("cells")) {
                    reader.skipValue();
                    continue;
                }

                // Parsing cells
                reader.beginArray();
                while (reader.hasNext()) {
                    Boolean isCode = null;
                    String source = null;

                    reader.beginObject();
                    while (reader.hasNext()) {
                        // If the cell type was parsed and wasn't code, then don't
                        // bother doing any more work. Skip the rest.
                        if (isCode != null && !isCode) {
                            reader.skipValue();
                            continue;
                        }

                        switch (reader.nextName()) {
                            case "cell_type":
                                // We are only concerned with code cells.
                                String cellType = reader.nextString();
                                isCode = cellType.equals("code");
                                break;
                            case "source":
                                // "source" is an array of lines.
                                StringBuilder srcBuilder = new StringBuilder();
                                reader.beginArray();
                                while (reader.hasNext())
                                    srcBuilder.append(reader.nextString());
                                reader.endArray();
                                source = srcBuilder.toString();
                                break;
                            default:
                                reader.skipValue();
                                break;
                        }
                    }
                    reader.endObject();

                    // Found a code cell!
                    if (isCode != null && isCode) {
                        kernel.eval(source);
                    }
                }
                reader.endArray();
            }
            reader.endObject();
        }
    }
}
