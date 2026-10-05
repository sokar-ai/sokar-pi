package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Code, comments, test names and anything that ships - the pages under {@code doc/} are published - never
 * cite an issue number, this repository's or another's: an issue is deleted once it is finished, and the
 * citation then points at nothing.
 */
class NoIssueNumberInCodeTest {

    /** The shape every repository's numbers share, so no list of prefixes has to be kept in step. */
    private static final Pattern ISSUE = Pattern.compile("\\b[A-Z]{1,2}[0-9]{2,3}\\b");

    /** Tests whose subject is citations, so their fixtures name numbers on purpose. */
    private static final Set<String> FIXTURES =
            Set.of("IssueCitationTest.java", "IssueLinkTest.java", "NoIssueNumberInCodeTest.java");

    /** Generated lockfiles, whose digests can spell an issue number by chance. */
    private static final Set<String> LOCKFILES = Set.of("package-lock.json");

    @Test
    void nothingThatShipsNamesAnIssueNumber() throws IOException {

        final List<Path> files = code();
        final List<String> found = new ArrayList<>();
        for (final Path file : files) {
            found.addAll(offenders(RepositoryDocuments.root().relativize(file).toString(), read(file)));
        }

        assertThat(files).as("the scan must reach the code it guards").isNotEmpty()
                .anySatisfy(file -> assertThat(file.toString()).endsWith(".yaml"))
                .anySatisfy(file -> assertThat(file.toString()).endsWith("decisions.md"));
        assertThat(found).as("name the thing instead of its issue").isEmpty();
    }

    @Test
    void refusesAThreeDigitNumberOfAnotherRepository() {

        assertThat(offenders("agent.yaml", "# Sokar B114 tells a task its prefixes.")).singleElement()
                .asString().contains("B114");
    }

    @Test
    void refusesAChannelQuestionsNumber() {

        assertThat(offenders("control", "# settled in the channel (QF19).")).hasSize(1);
    }

    @Test
    void refusesANumberOfThisRepository() {

        assertThat(offenders("Test.java", "// Measured for " + RepositoryDocuments.LETTER + "07.")).hasSize(1);
    }

    @Test
    void allowsWordsAndNumbersThatAreNotIssues() {

        assertThat(offenders("pom.xml", "UTF8, SHA256, X509TrustManager, v1.19, port 9419, x86-64, a 302 answer"))
                .isEmpty();
    }

    /**
     * Returns every issue number a file names.
     *
     * @param path Path of the file, for naming the offender.
     * @param text Content of the file.
     * @return One sentence per number found.
     */
    static List<String> offenders(final String path, final String text) {

        final List<String> found = new ArrayList<>();
        final Matcher issue = ISSUE.matcher(text);
        while (issue.find()) {
            found.add(path + " names " + issue.group());
        }
        return found;
    }

    private static String read(final Path file) throws IOException {
        try {
            return Files.readString(file);
        } catch (final MalformedInputException binary) {
            return "";
        }
    }

    private static List<Path> code() throws IOException {
        // Markdown outside doc/ is the issues' and the rules', which cite on purpose; build output and dot
        // directories are not checked in, except .github, which runs.
        final Path root = RepositoryDocuments.root();
        final List<Path> found = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                final String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                final boolean skipped = name.equals("target") || name.equals("node_modules")
                        || name.startsWith(".") && !name.equals(".github");
                return !dir.equals(root) && skipped ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                final String name = file.getFileName().toString();
                final boolean page = name.endsWith(".md") && root.relativize(file).startsWith("doc");
                if ((page || !name.endsWith(".md")) && !name.startsWith(".") && !FIXTURES.contains(name)
                        && !LOCKFILES.contains(name)) {
                    found.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return found;
    }
}
