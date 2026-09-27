package org.fuin.sokar.agent.impl.pi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The repository's own markdown files and issue numbers, for the guards that read them.
 * <p>
 * One walk rather than one per guard, because two copies of "which files count" drift.
 */
final class RepositoryDocuments {

    /** The letter this repository's issue numbers carry. */
    static final String LETTER = "PI";

    private static final Pattern NUMBER = Pattern.compile("^" + LETTER + "([0-9]{2})-.*\\.md$");

    private RepositoryDocuments() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the repository root, which is where a test runs.
     *
     * @return Absolute path.
     */
    static Path root() {
        return Path.of("").toAbsolutePath();
    }

    /**
     * Returns every checked-in markdown file, build output aside.
     *
     * @return Absolute paths, sorted.
     * @throws IOException When the tree cannot be walked.
     */
    static List<Path> all() throws IOException {
        try (Stream<Path> tree = Files.walk(root())) {
            return tree.filter(RepositoryDocuments::counts).sorted().toList();
        }
    }

    /**
     * Says whether a file is one of the documents the guards read.
     * <p>
     * A dot file or directory is not checked in, so it never reaches CI: a note in {@code .AGENTS.md}
     * must not turn a guard red on one machine and nowhere else.
     *
     * @param path Absolute path under the repository root.
     * @return True for a markdown file outside build output and dot paths.
     */
    static boolean counts(final Path path) {
        if (!path.toString().endsWith(".md")) {
            return false;
        }
        for (final Path segment : root().relativize(path)) {
            if (segment.toString().startsWith(".") || segment.toString().equals("target")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the numbers of the issues that exist, without the letter.
     *
     * @return Two-digit numbers.
     * @throws IOException When the directory cannot be read, or is gone.
     */
    static Set<String> liveIssues() throws IOException {
        try (Stream<Path> files = Files.list(root().resolve("issues"))) {
            return files.map(path -> NUMBER.matcher(path.getFileName().toString()))
                    .filter(Matcher::matches)
                    .map(matcher -> matcher.group(1))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
