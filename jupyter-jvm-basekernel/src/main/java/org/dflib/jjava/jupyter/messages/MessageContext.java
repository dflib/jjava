package org.dflib.jjava.jupyter.messages;

import java.util.List;

public interface MessageContext {
    public List<byte[]> getIdentities();

    public Header getHeader();
}
