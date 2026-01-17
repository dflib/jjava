package org.dflib.jjava.kernel.magics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.dflib.jjava.jupyter.kernel.magic.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.MagicsArgs;
import org.dflib.jjava.kernel.JavaKernel;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class LoadMagic implements LineMagic<Void, JavaKernel> {

    private static final String NOTEBOOK_EXTENSION = ".ipynb";
    private static final Gson GSON = new GsonBuilder().create();
    private static final MagicsArgs LOAD_ARGS = MagicsArgs.builder()
            .required("source")
            .onlyKnownFlags()
            .onlyKnownKeywords()
            .build();

    @Override
    public Void eval(JavaKernel kernel, List<String> args) throws Exception {

        Map<String, List<String>> vals = LOAD_ARGS.parse(args);
        Path sourcePath = Paths.get(vals.get("source").get(0)).toAbsolutePath();

        String file = sourcePath.getFileName().toString();
        Path scriptPath = sourcePath.resolveSibling(file);
        if (!Files.isRegularFile(scriptPath)) {
            throw new FileNotFoundException("Could not find any source at '" + sourcePath);
        }

        // TODO: return the eval result to the caller?
        Object result = (isNotebook(scriptPath))
                ? evalNotebook(kernel, scriptPath)
                : kernel.evalBuilder(Files.readString(scriptPath)).resolveMagics().eval();

        return null;
    }

    private boolean isNotebook(Path scriptPath) {
        // ".toString()" is important. "Path.endsWith(..)" means something entirely different
        return scriptPath.getFileName().toString().endsWith(NOTEBOOK_EXTENSION);
    }

    // This slightly verbose implementation is designed to take advantage of gson as a streaming parser
    // in which we can only take what we need on the fly and pass each cell to the handler without needing
    // to keep the entire (possibly large) notebook in memory.
    private Object evalNotebook(JavaKernel kernel, Path notebookPath) throws Exception {
        try (Reader in = Files.newBufferedReader(notebookPath, StandardCharsets.UTF_8)) {
            try (JsonReader reader = GSON.newJsonReader(in)) {
                return evalNotebook(kernel, reader);
            }
        }
    }

    private Object evalNotebook(JavaKernel kernel, JsonReader reader) throws Exception {

        Object lastResult = null;

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

                if (isCode != null && isCode) {
                    lastResult = kernel.evalBuilder(source).resolveMagics().eval();
                }
            }
            reader.endArray();
        }
        reader.endObject();

        return lastResult;
    }
}
