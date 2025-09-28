package org.dflib.jjava.jupyter.kernel.display;

import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A smarter set of {@link MIMEType}s.
 */
public class RenderRequestTypes {
    public static class Builder {
        private final MIMESuffixAssociation suffixAssociation;

        private boolean requestsWildcard;
        private final Set<String> entireGroupRequests;
        private final Set<MIMEType> requestedTypes;

        public Builder(MIMESuffixAssociation suffixAssociation) {
            this.suffixAssociation = suffixAssociation;

            this.requestsWildcard = false;
            this.entireGroupRequests = new LinkedHashSet<>();
            this.requestedTypes = new LinkedHashSet<>();
        }

        public Builder withType(String type) {
            MIMEType mimeType = MIMEType.parse(type);
            return this.withType(mimeType);
        }

        public Builder withType(MIMEType type) {
            if (type.isWildcard())
                this.requestsWildcard = true;
            else if (!type.hasSubtype() || type.subtypeIsWildcard())
                this.entireGroupRequests.add(type.getGroup());
            else
                this.requestedTypes.add(type);
            return this;
        }

        public RenderRequestTypes build() {
            return new RenderRequestTypes(
                    this.suffixAssociation,
                    this.requestsWildcard,
                    this.requestsWildcard || this.entireGroupRequests.isEmpty()
                            ? Collections.emptySet()
                            : this.entireGroupRequests,
                    this.requestsWildcard || this.requestedTypes.isEmpty()
                            ? Collections.emptySet()
                            : this.requestedTypes
            );
        }
    }

    private final MIMESuffixAssociation suffixAssociation;

    private final boolean requestsWildcard;
    private final Set<String> entireGroupRequests;
    private final Set<MIMEType> requestedTypes;
    private final Map<String, List<MIMEType>> requestedTypesByGroup;

    private RenderRequestTypes(MIMESuffixAssociation suffixAssociation, boolean requestsWildcard, Set<String> entireGroupRequests, Set<MIMEType> requestedTypes) {
        this.suffixAssociation = suffixAssociation;
        this.requestsWildcard = requestsWildcard;
        this.entireGroupRequests = entireGroupRequests;
        this.requestedTypes = requestedTypes;

        this.requestedTypesByGroup = new LinkedHashMap<>();
        requestedTypes.forEach(t ->
                this.requestedTypesByGroup.compute(t.getGroup(), (k, v) -> {
                    List<MIMEType> l = v == null ? new ArrayList<>() : v;
                    l.add(t);
                    return l;
                })
        );
    }

    /**
     * Resolve the requested {@link MIMEType} from a supported type. This query usually returns
     * {@code null} or the original {@code supportedType} except in special cases with the suffix.
     * <p>
     * If the {@code supportedType} with the {@link MIMEType#getSuffix() suffix} dropped is requested
     * then the resolved type is the {@code supportedType} with the {@link MIMEType#getSuffix() suffix} dropped.
     * <p>
     * If the {@code supportedType}'s {@link MIMEType#getSuffix() suffix} has a {@link MIMESuffixAssociation#resolveSuffix(String) resolved suffix type}
     * (like {@code +json} being compatible with {@code application/json}) and the {@link MIMESuffixAssociation#resolveSuffix(String) resolved suffix type}
     * is requested, then the {@link MIMESuffixAssociation#resolveSuffix(String) resolved suffix type} is the resolved type.
     *
     * @param supportedType the type to resolve to one of the requested types.
     *
     * @return the requested type or {@code null} if the type is not requested.
     */
    public MIMEType resolveSupportedType(MIMEType supportedType) {
        if (supportedType.isWildcard() || supportedType.subtypeIsWildcard())
            throw new IllegalArgumentException("Cannot resolve type of wildcard MIME type: '" + supportedType.toString() + "'");

        // Everything is supported
        if (this.requestsWildcard)
            return supportedType;

        // If the exact type is supported or the group is supported then the exact type
        // is supported.
        if (this.requestedTypes.contains(supportedType)
                || this.entireGroupRequests.contains(supportedType.getGroup()))
            return supportedType;

        if (supportedType.hasSuffix()) {
            // If dropping the supported type without the suffix is supported then that
            // is compatible and is the resolved type.
            MIMEType withoutSuffix = supportedType.withoutSuffix();
            if (this.requestedTypes.contains(withoutSuffix))
                return withoutSuffix;

            // If the type association of the suffix is supported then use the association.
            MIMEType suffixDelegate = this.suffixAssociation.resolveSuffix(supportedType.getSuffix());
            if (suffixDelegate != null && (
                    this.requestedTypes.contains(suffixDelegate)
                            || this.entireGroupRequests.contains(suffixDelegate.getGroup())))
                return suffixDelegate;
        }

        // The type is not supported
        return null;
    }

    public boolean isRequestedExactly(MIMEType type) {
        return this.requestsWildcard
                || this.entireGroupRequests.contains(type.getGroup())
                || this.requestedTypes.contains(type);
    }

    public void removeFulfilledRequests(DisplayData out) {
        this.requestedTypes.removeIf(t -> {
            if (out.hasDataForType(t)) {
                this.requestedTypesByGroup.compute(t.getGroup(), (k, v) -> {
                    if (v == null) return null;
                    v.remove(t);
                    return v.isEmpty() ? null : v;
                });
                return true;
            }
            return false;
        });
    }

    /**
     * Check if the request wants something rendered as any of the supported types.
     *
     * @param supported a set of supported types
     *
     * @return true if any of the supported types is requested
     */
    public boolean anyRequestedIsSupported(Set<MIMEType> supported) {
        // The request wants everything. As long as something is supported, it is requested.
        if (this.requestsWildcard)
            return !supported.isEmpty();

        for (MIMEType t : supported) {
            // If any request is supported then as long as the request is not empty, something
            // is requested.
            if (t.isWildcard() && !this.isEmpty())
                return true;

            // If an entire group is supported then as long as that group is requested or
            // something requested has the same group, something is requested.
            if (t.subtypeIsWildcard() && (
                    this.entireGroupRequests.contains(t.getGroup())
                            || this.requestedTypesByGroup.containsKey(t.getGroup())))
                return true;

            // If the supported type can be resolved then it must be requested.
            if (this.resolveSupportedType(t) != null)
                return true;
        }

        // Nothing supported is requested.
        return false;
    }

    public boolean isEmpty() {
        return !this.requestsWildcard
                && this.entireGroupRequests.isEmpty()
                && this.requestedTypes.isEmpty();
    }
}
