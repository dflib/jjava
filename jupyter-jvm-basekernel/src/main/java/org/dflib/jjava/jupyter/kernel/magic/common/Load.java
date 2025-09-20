package org.dflib.jjava.jupyter.kernel.magic.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.dflib.jjava.jupyter.kernel.magic.registry.LineMagic;
import org.dflib.jjava.jupyter.kernel.magic.registry.MagicsArgs;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Load {

    @FunctionalInterface
    public interface Executor {
        void execute(String code) throws Exception;
    }

    private static final ThreadLocal<Gson> GSON = ThreadLocal.withInitial(() ->
            new GsonBuilder().create());

    private static final MagicsArgs LOAD_ARGS = MagicsArgs.builder()
            .required("source")
            .onlyKnownFlags().onlyKnownKeywords()
            .build();

    // This slightly verbose implementation is designed to take advantage of gson as a streaming parser
    // in which we can only take what we need on the fly and pass each cell to the handler without needing
    // to keep the entire notebook in memory.
    // This should be a big help for larger notebooks.
    private static void forEachCell(Path notebookPath, Executor handle) throws Exception {
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
                    if (isCode != null && isCode)
                        handle.execute(source);
                }
                reader.endArray();
            }
            reader.endObject();
        }
    }

    private final List<String> fileExtensions;
    private final Executor exec;

    public Load(List<String> fileExtensions, Executor exec) {
        this.fileExtensions = fileExtensions == null
                ? Collections.emptyList()
                : fileExtensions.stream()
                .map(e -> e.startsWith(".") ? e : "." + e)
                .collect(Collectors.toList());
        this.exec = exec;
    }

    @LineMagic
    public void load(List<String> args) throws Exception {
        Map<String, List<String>> vals = LOAD_ARGS.parse(args);

        Path sourcePath = Paths.get(vals.get("source").get(0)).toAbsolutePath();

        if (Files.isRegularFile(sourcePath)) {
            if (sourcePath.getFileName().toString().endsWith(".ipynb")) {
                // Execute a notebook, run all cells in there.
                Load.forEachCell(sourcePath, this.exec);
                return;
            }

            String sourceContents = Files.readString(sourcePath);
            this.exec.execute(sourceContents);
            return;
        }

        String file = sourcePath.getFileName().toString();

        // Try and see if adding any of the supported extensions gives a file.
        for (String extension : this.fileExtensions) {
            Path scriptPath = sourcePath.resolveSibling(file + extension);
            if (Files.isRegularFile(scriptPath)) {
                String sourceContents = Files.readString(scriptPath);
                this.exec.execute(sourceContents);
                return;
            }
        }

        // Try a notebook last.
        Path scriptPath = sourcePath.resolveSibling(file + ".ipynb");
        if (Files.isRegularFile(scriptPath)) {
            // Execute a notebook, run all cells in there.
            Load.forEachCell(scriptPath, this.exec);
            return;
        }

        String exts = String.join(", ", this.fileExtensions);
        throw new FileNotFoundException(
                "Could not find any source at '" + sourcePath + "'. Also tried with extensions: [.ipynb, " + exts + "].");
    }
}
