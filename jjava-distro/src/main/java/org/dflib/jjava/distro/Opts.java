package org.dflib.jjava.distro;

import java.util.ArrayList;
import java.util.List;

class Opts {

    // TODO: see 7205d03766c8 and c91ade8fec0cdc. A similar split method in MagicParser got altered in a few subtle
    //  ways... Do those changes alloy here, and should we have a single splitter?
    //  7205d03766c8
    //  - if (current.length() > 0 && inQuotes) {
    //  + if (inQuotes) {
    //  c91ade8fec0cdc
    //   - current.append("\\\\");
    //   + current.append("\\");

    static List<String> splitOpts(String opts) {
        opts = opts.trim();

        List<String> split = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escape = false;
        for (char c : opts.toCharArray()) {
            switch (c) {
                case ' ':
                case '\t':
                    if (inQuotes) {
                        current.append(c);
                    } else if (current.length() > 0) {
                        // If whitespace is closing the string the add the current and reset
                        split.add(current.toString());
                        current.setLength(0);
                    }
                    break;
                case '\\':
                    if (escape) {
                        current.append("\\\\");
                        escape = false;
                    } else {
                        escape = true;
                    }
                    break;
                case '\"':
                    if (escape) {
                        current.append('"');
                        escape = false;
                    } else {
                        if (current.length() > 0 && inQuotes) {
                            split.add(current.toString());
                            current.setLength(0);
                            inQuotes = false;
                        } else {
                            inQuotes = true;
                        }
                    }
                    break;
                default:
                    current.append(c);
            }
        }

        if (current.length() > 0) {
            split.add(current.toString());
        }

        return split;
    }
}
