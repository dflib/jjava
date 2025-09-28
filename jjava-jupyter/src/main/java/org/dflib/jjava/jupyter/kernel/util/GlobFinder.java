package org.dflib.jjava.jupyter.kernel.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simplified glob implementation designed for finding files. The current implementation supports
 * {@code "*"} to match 0 or more characters <strong>between {@code "/"}</strong> and {@code "?"} to
 * match a single character. A glob ending in {@code "/"} will match all files in a directories matching
 * the glob.
 * <p>
 * Important note for Windows file systems: Globs should use {@code "/"} to separate the glob despite it not being the
 * platform separator.
 */
public class GlobFinder {
    private static class GlobSegment {
        public enum FilterRestriction {
            ONLY_FILES(true, false),
            ONLY_DIRECTORIES(false, true),
            ANYTHING(true, true);

            private final boolean acceptsFiles;
            private final boolean acceptsDirectories;

            FilterRestriction(boolean acceptsFiles, boolean acceptsDirectories) {
                this.acceptsFiles = acceptsFiles;
                this.acceptsDirectories = acceptsDirectories;
            }

            public boolean acceptsFiles() {
                return acceptsFiles;
            }

            public boolean acceptsDirectories() {
                return acceptsDirectories;
            }
        }

        public static final GlobSegment ANY = new GlobSegment(Pattern.compile("^.*$"));

        private final String literal;
        private final Pattern regex;

        public GlobSegment(String literal) {
            this.literal = literal;
            this.regex = null;
        }

        public GlobSegment(Pattern regex) {
            this.literal = null;
            this.regex = regex;
        }

        public boolean isLiteral() {
            return this.literal != null;
        }

        public DirectoryStream.Filter<Path> filter(FilterRestriction restriction) {
            return s -> {
                BasicFileAttributes attributes = Files.readAttributes(s, BasicFileAttributes.class);

                if ((attributes.isRegularFile() && !restriction.acceptsFiles()) || (attributes.isDirectory() && !restriction.acceptsDirectories())) {
                    return false;
                }

                Path pathName = s.getFileName();

                if (pathName == null) {
                    return false;
                }

                String name = pathName.toString();
                return this.literal != null
                        ? this.literal.equals(name)
                        : this.regex.matcher(name).matches();
            };
        }

        @Override
        public String toString() {
            return this.isLiteral() ? this.literal : this.regex.pattern();
        }
    }

    private static final Pattern GLOB_SEGMENT_COMPONENT = Pattern.compile(
            "" +
                    "(?<literal>[^*?]+)" +
                    "|(?<wildcard>\\*)" +
                    "|(?<singleWildcard>\\?)" +
                    "|(?:\\\\(?<escaped>[*?]))"
    );

    private static final Pattern SPLITTER = Pattern.compile("/+");

    private final Path base;
    private final List<GlobSegment> segments;
    private final boolean isExplicitDirectory;

    public GlobFinder(FileSystem fs, String glob) {
        // Split with "/" but match with the actual separator
        String[] segments = SPLITTER.split(glob);
        this.isExplicitDirectory = glob.endsWith("/");

        List<GlobSegment> matchers = new ArrayList<>(segments.length);
        int lastBaseSegmentIdx = 0;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            StringBuilder pattern = new StringBuilder();
            StringBuilder lit = new StringBuilder();
            int wildcards = 0;
            int singleWildcards = 0;

            Matcher m = GLOB_SEGMENT_COMPONENT.matcher(segment);
            while (m.find()) {
                String literal = m.group("literal");
                if (literal == null) literal = m.group("escaped");
                if (literal != null) {
                    pattern.append(Pattern.quote(literal));
                    lit.append(literal);
                    continue;
                }

                String wildcard = m.group("wildcard");
                if (wildcard != null) {
                    pattern.append(".*");
                    wildcards++;
                    continue;
                }

                String singleWildcard = m.group("singleWildcard");
                // There are only 4 groups, 3 of which have been checked and are null so this
                // on must be non-null.
                assert singleWildcard != null : "Glob construction pattern incomplete.";
                pattern.append(".");
                singleWildcards++;
            }

            assert m.hitEnd() : "Glob construction missed some characters.";

            if (wildcards == 0 && singleWildcards == 0) {
                matchers.add(new GlobSegment(lit.toString()));
                if (lastBaseSegmentIdx == i) lastBaseSegmentIdx++;
            } else {
                matchers.add(new GlobSegment(Pattern.compile("^" + pattern.toString() + "$")));
            }
        }

        // Cannot use the very nice `new File(glob).isAbsolute()` solution as this is restricted to the default file
        // system and doesn't use the `fs`. Additionally `Paths.get(glob).isAbsolute()` will fail with an illegal path
        // exception when trying to parse a windows path with a * in it for example. Therefor we need a clean segment.
        boolean isAbsolute = lastBaseSegmentIdx > 0 && fs.getPath(segments[0] + fs.getSeparator()).isAbsolute();
        String firstSeg = isAbsolute ? segments[0] + fs.getSeparator() : "." + fs.getSeparator();

        this.base = fs.getPath(firstSeg, Arrays.copyOfRange(segments, isAbsolute ? 1 : 0, lastBaseSegmentIdx));
        this.segments = matchers.subList(lastBaseSegmentIdx, matchers.size());
    }

    public GlobFinder(String glob) {
        this(FileSystems.getDefault(), glob);
    }

    public Iterable<Path> computeMatchingPaths() throws IOException {
        if (this.segments.isEmpty()) {
            if (Files.exists(this.base))
                return Collections.singletonList(this.base);
            else
                return Collections.emptyList();
        }

        List<Path> paths = new ArrayList<>();
        GlobSegment head = this.segments.get(0);
        List<GlobSegment> tail = this.segments.subList(1, this.segments.size());

        collectExplicit(GlobSegment.FilterRestriction.ANYTHING, this.base, head, tail, paths);

        return paths;
    }

    private void collectExplicit(GlobSegment.FilterRestriction finalFilterRestriction, Path dir, GlobSegment segment, List<GlobSegment> segments, Collection<Path> into) throws IOException {
        boolean isMoreSegments = !segments.isEmpty();
        // Should match files if there are more segments in which case this must be a directory so
        // we can continue. Otherwise we let the search determine if a file is acceptable.
        GlobSegment.FilterRestriction filterRestriction = isMoreSegments ? GlobSegment.FilterRestriction.ONLY_DIRECTORIES : finalFilterRestriction;

        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, segment.filter(filterRestriction))) {
            GlobSegment head = isMoreSegments ? segments.get(0) : null;
            List<GlobSegment> tail = isMoreSegments ? segments.subList(1, segments.size()) : Collections.emptyList();

            for (Path p : files) {
                if (isMoreSegments)
                    collectExplicit(finalFilterRestriction, p, head, tail, into);
                else
                    into.add(p);
            }
        }
    }

    public Iterable<Path> computeMatchingFiles() throws IOException {
        if (this.segments.isEmpty()) {
            if (Files.isDirectory(this.base) && this.isExplicitDirectory)
                return Files.newDirectoryStream(this.base, Files::isRegularFile);
            if (Files.isRegularFile(this.base))
                return Collections.singleton(this.base);
            return Collections.emptyList();
        }

        List<Path> paths = new ArrayList<>();
        GlobSegment head = this.segments.get(0);
        List<GlobSegment> tail;

        // If explicitly ends with a "/" then the pattern means match all files in this directory
        // otherwise we assume the last pattern is a file matcher.
        if (this.isExplicitDirectory) {
            tail = new ArrayList<>(this.segments.size() + 1);
            Collections.copy(tail, this.segments.subList(1, this.segments.size()));
            tail.add(GlobSegment.ANY);
        } else {
            tail = this.segments.subList(1, this.segments.size());
        }

        collectExplicit(GlobSegment.FilterRestriction.ONLY_FILES, this.base, head, tail, paths);

        return paths;
    }
}

