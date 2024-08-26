package org.dflib.jjava.jupyter.kernel.magic.common;

import org.dflib.jjava.jupyter.kernel.magic.registry.CellMagic;
import org.dflib.jjava.jupyter.kernel.magic.registry.MagicsArgs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.Map;

public class WriteFile {
    private static final MagicsArgs WRITEFILE_ARGS = MagicsArgs.builder()
            .required("filename")
            .flag("append", 'a')
            .onlyKnownFlags().onlyKnownKeywords()
            .build();

    @CellMagic
    public static Void writefile(List<String> args, String body) throws Exception {
        Map<String, List<String>> vals = WRITEFILE_ARGS.parse(args);

        String fileName = vals.get("filename").get(0);
        boolean append = !vals.get("append").isEmpty();

        File file = new File(fileName);

        if (file.isDirectory())
            throw new FileAlreadyExistsException("Cannot write to file " + fileName + ". It is a directory.");

        try (OutputStreamWriter fileOut = new OutputStreamWriter(new FileOutputStream(file, append), Charset.forName("utf8"))) {
            fileOut.write(body);
        }

        return null;
    }
}
