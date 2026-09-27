package org.fuin.sokar.agent.impl.pi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The repository's own markdown files and issue numbers, for the guards that read them.
 * <p>
 * One walk rather than one per guard, because two copies of "which files count" drift.
 */
final class RepositoryDocuments {

    private static final Pattern NUMBER =
            Pattern.compile("^" + IssueCitationTest.LETTER + "([0-9]{2})-.*\\.md$");

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
     * Returns every markdown file in the repository, build output aside.
     *
     * @return Absolute paths, sorted.
     * @throws IOException When the tree cannot be walked.
     */
    static List<Path> all() throws IOException {
        try (Stream<Path> tree = Files.walk(root())) {
            return tree.filter(path -> path.toString().endsWith(".md"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> !path.toString().contains("/.git/"))
                    .sorted()
                    .toList();
        }
    }

    /**
     * Returns the numbers of the issues that exist, without the letter.
     *
     * @return Two-digit numbers; empty when the directory is gone.
     * @throws IOException When the directory cannot be read.
     */
    static Set<String> liveIssues() throws IOException {
        try (Stream<Path> files = Files.list(root().resolve("issues"))) {
            return files.map(path -> NUMBER.matcher(path.getFileName().toString()))
                    .filter(Matcher::matches)
                    .map(matcher -> matcher.group(1))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
        }
    }
}
