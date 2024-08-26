package org.dflib.jjava.jupyter.messages.publish;

import java.util.List;

@FunctionalInterface
public interface ErrorFormatter {
    List<String> format(Exception e);
}
