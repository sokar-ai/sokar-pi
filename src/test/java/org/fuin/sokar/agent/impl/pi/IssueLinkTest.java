package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * No document but the index links an issue by its file.
 * <p>
 * An issue file is deleted when the issue is finished or closed unbuilt, so a link to one breaks
 * then. The index is the exception: linking files is what an index is.
 */
@Tag("documents")
class IssueLinkTest {

    /** The one file allowed to link issue files, relative to the repository root. */
    private static final Path INDEX = Path.of("issues", "README.md");

    private static final Pattern LINK = Pattern.compile("\\]\\(([^)\\s]+)");

    // A reference-style link names its target on a line of its own: [label]: target
    private static final Pattern REFERENCE = Pattern.compile("(?m)^\\s*\\[[^\\]]+\\]:\\s*(\\S+)");

    // Any depth under issues/, because another repository's requirements sit one level deeper.
    private static final Pattern ISSUE_FILE = Pattern.compile(".*issues/.+\\.md(#.*)?");

    @Test
    void noDocumentButTheIndexLinksAnIssueFile() throws IOException {

        final List<String> found = new ArrayList<>();
        for (final Path document : RepositoryDocuments.all()) {
            found.addAll(offenders(document.toString(), Files.readString(document)));
        }

        assertThat(found).isEmpty();
    }

    @Test
    void refusesALinkToAnIssueFile() throws IOException {

        // From a file that is really there, so a rename cannot leave this passing on a sample.
        final Path issue = anIssueFile();
        final String document = "Waiting on [the pin](" + INDEX.getParent() + "/" + issue.getFileName() + ").";

        assertThat(offenders("doc/decisions.md", document)).singleElement().asString()
                .contains(issue.getFileName().toString());
    }

    @Test
    void refusesALinkToAnIssueFileOfAnotherRepository() {

        final String document = "See [B55](../sokar/issues/base/B55-The-Changelog-Check.md).";

        assertThat(offenders("build.md", document)).singleElement().asString()
                .contains("B55-The-Changelog-Check.md");
    }

    @Test
    void allowsTheIndexToLinkTheFilesItIndexes() throws IOException {

        final Path index = RepositoryDocuments.root().resolve(INDEX);

        assertThat(offenders(INDEX.toString(), Files.readString(index))).isEmpty();
    }

    @Test
    void allowsALinkToTheIndexItself() {

        assertThat(offenders("build.md", "Open work is in [the index](issues/README.md).")).isEmpty();
    }

    @Test
    void allowsALinkToARowOfTheIndex() {

        // The fragment names a row; the target is still the index.
        assertThat(offenders("build.md", "Next is [the top row](issues/README.md#pi12).")).isEmpty();
    }

    @Test
    void refusesAReferenceStyleLinkToAnIssueFile() {

        final String document = "Waiting on [the pin][pin].\n\n[pin]: issues/PI03-Automated-Agent-Updates.md\n";

        assertThat(offenders("doc/decisions.md", document)).singleElement().asString()
                .contains("PI03-Automated-Agent-Updates.md");
    }

    /**
     * Returns every link to an issue file in one document.
     *
     * @param path Path of the document, used to let the index through and to name the offender.
     * @param text Content of the document.
     * @return One sentence per offending link; empty when the document keeps the rule.
     */
    static List<String> offenders(final String path, final String text) {

        if (Path.of(path).endsWith(INDEX)) {
            return List.of();
        }
        final List<String> found = new ArrayList<>();
        for (final Pattern kind : List.of(LINK, REFERENCE)) {
            final Matcher link = kind.matcher(text);
            while (link.find()) {
                final String target = link.group(1);
                // A fragment names a row of the index; it does not make the index an issue file.
                final String file = target.replaceFirst("#.*$", "");
                if (ISSUE_FILE.matcher(file).matches() && !file.endsWith("README.md")) {
                    found.add(path + " links an issue file rather than the index: " + target);
                }
            }
        }
        return found;
    }

    private static Path anIssueFile() throws IOException {
        try (Stream<Path> tree = Files.list(RepositoryDocuments.root().resolve(INDEX.getParent()))) {
            // With no issue open, a name of the same shape: what is refused is the shape, not the file.
            return tree.filter(path -> !path.endsWith("README.md"))
                    .min(Comparator.comparing(Path::toString)).orElse(Path.of("PI99-An-Issue.md"));
        }
    }
}
