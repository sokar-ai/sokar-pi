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
import org.junit.jupiter.api.Test;

/**
 * No document but the index links an issue by its file.
 * <p>
 * An issue file is deleted when the issue is finished, so a pointer at one breaks on the day it
 * succeeds - and it breaks the same way when an issue is closed unbuilt, which nobody expects.
 * Three repositories found that defect by accident in one morning on 2026-09-20; this is the guard
 * that keeps it out of this one, because the fourth finding would not have been lucky.
 * <p>
 * The index is the exception: it links files by design and is the thing everything else points at.
 */
class IssueLinkTest {

    /** The one file allowed to link issue files, relative to the repository root. */
    private static final Path INDEX = Path.of("issues", "README.md");

    private static final Pattern LINK = Pattern.compile("\\]\\(([^)\\s]+)");

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

        // Built from a file that is really there, so renaming an issue cannot leave this passing
        // against a sample nothing matches any more.
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
        final Matcher link = LINK.matcher(text);
        while (link.find()) {
            final String target = link.group(1);
            if (ISSUE_FILE.matcher(target).matches() && !target.endsWith("README.md")) {
                found.add(path + " links an issue file rather than the index: " + target);
            }
        }
        return found;
    }

    private static Path anIssueFile() throws IOException {
        try (Stream<Path> tree = Files.list(RepositoryDocuments.root().resolve(INDEX.getParent()))) {
            final Path issue = tree.filter(path -> !path.endsWith("README.md"))
                    .min(Comparator.comparing(Path::toString)).orElse(null);
            assertThat(issue).as("an issue file to build the refusal from").isNotNull();
            return issue;
        }
    }
}
