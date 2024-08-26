package org.dflib.jjava.jupyter.kernel.display;

import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

@FunctionalInterface
public interface MIMESuffixAssociation {
    static final MIMESuffixAssociation NONE = s -> null;

    /**
     * Returns the delegate MIME type associated with a suffix. For example the
     * suffix {@code json} is associated with the {@code application/json} type.
     *
     * @param suffix the suffix to resolve
     *
     * @return the delegate {@link MIMEType}
     */
    MIMEType resolveSuffix(String suffix);
}
