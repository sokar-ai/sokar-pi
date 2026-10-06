package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Every test that reads a document carries the tag {@code documents}, so a change to documents or issues
 * runs exactly those tests with {@code -Pdocuments} and none is left out.
 */
@Tag("documents")
class DocumentTestsTaggedTest {

    /** The tag the {@code documents} profile selects. */
    static final String TAG = "@Tag(\"documents\")";

    /** A document is reached through the shared walk or named by a markdown path in a string literal. */
    private static final Pattern READS_A_DOCUMENT =
            Pattern.compile("RepositoryDocuments\\.(all|liveIssues)\\(|\"[^\"\\n]*\\.md[#\"]");

    private static final Path TESTS = Path.of("src", "test", "java");

    @Test
    void everyTestThatReadsADocumentCarriesTheTag() throws IOException {

        final List<Path> readers = readers();
        final List<String> untagged = readers.stream()
                .filter(file -> !tagged(read(file)))
                .map(file -> file.getFileName().toString())
                .toList();

        assertThat(readers).as("the scan must find the tests that read documents")
                .anySatisfy(file -> assertThat(file.getFileName()).hasToString("IssueLinkTest.java"))
                .anySatisfy(file -> assertThat(file.getFileName()).hasToString("NoIssueNumberInCodeTest.java"));
        assertThat(untagged).as("tests that read a document without %s", TAG).isEmpty();
    }

    @Test
    void seesTheSharedWalk() {

        assertThat(readsADocument("for (Path p : RepositoryDocuments.all()) {")).isTrue();
        assertThat(readsADocument("Set<String> live = RepositoryDocuments.liveIssues();")).isTrue();
    }

    @Test
    void seesAMarkdownPathWithOrWithoutAnchor() {

        assertThat(readsADocument("Path.of(\"issues\", \"README.md\")")).isTrue();
        assertThat(readsADocument("\"See [x](issues/README.md#cc14).\"")).isTrue();
    }

    @Test
    void ignoresATestThatReadsOnlyTheRoot() {

        assertThat(readsADocument("RepositoryDocuments.root().resolve(\".github/workflows\")"))
                .as("the workflows are not documents").isFalse();
        assertThat(readsADocument("contains(\"documentation: \\\"https://example.org/docs\\\"\")")).isFalse();
    }

    @Test
    void requiresTheTagOnTheClassNotInAComment() {

        assertThat(tagged("// " + TAG + "\nclass X {")).isFalse();
        assertThat(tagged(TAG + "\nclass X {")).isTrue();
    }

    private static List<Path> readers() throws IOException {
        try (Stream<Path> tree = Files.walk(RepositoryDocuments.root().resolve(TESTS))) {
            return tree.filter(file -> file.toString().endsWith("Test.java"))
                    .filter(file -> readsADocument(read(file)))
                    .sorted()
                    .toList();
        }
    }

    private static boolean readsADocument(final String source) {
        return READS_A_DOCUMENT.matcher(source).find();
    }

    private static boolean tagged(final String source) {
        return Pattern.compile("^" + Pattern.quote(TAG) + "$", Pattern.MULTILINE).matcher(source).find();
    }

    private static String read(final Path file) {
        try {
            return Files.readString(file);
        } catch (final IOException ex) {
            throw new IllegalStateException("Cannot read " + file, ex);
        }
    }
}
