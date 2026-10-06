package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Every issue number a document names is an issue that exists.
 * <p>
 * Sound because a retired number leaves the documents with its file, so a citation is always a
 * pointer. No link breaks when one goes stale, which is why a link guard cannot see this.
 */
@Tag("documents")
class IssueCitationTest {

    /** This repository's set. Another repository's letter is not ours to check. */
    static final String LETTER = RepositoryDocuments.LETTER;

    private static final Pattern CITATION = Pattern.compile("\\b" + LETTER + "([0-9]{2,3})\\b");

    @Test
    void everyIssueNumberInADocumentIsAnIssueThatExists() throws IOException {

        final Set<String> live = RepositoryDocuments.liveIssues();
        final List<String> found = new ArrayList<>();
        for (final Path document : RepositoryDocuments.all()) {
            found.addAll(offenders(document.toString(), Files.readString(document), live));
        }

        assertThat(found).as("issues that exist: %s", live).isEmpty();
    }

    @Test
    void findsTheIssuesItChecksAgainst() throws IOException {

        // No documents passes forever. And the issues found must be every issue file there is: a scanner that
        // found none would report every citation as dangling. No open issue at all is a state it must allow.
        assertThat(RepositoryDocuments.all()).isNotEmpty();
        final long files;
        try (java.util.stream.Stream<java.nio.file.Path> listed =
                java.nio.file.Files.list(RepositoryDocuments.root().resolve("issues"))) {
            files = listed.filter(path -> !path.endsWith("README.md") && path.toString().endsWith(".md")).count();
        }
        assertThat(RepositoryDocuments.liveIssues()).hasSize((int) files);
    }

    @Test
    void refusesACitationOfADeletedIssue() {

        final String document = "The matrix was carried over from " + LETTER + "02.";

        assertThat(offenders("doc/decisions.md", document, Set.of("01", "03"))).singleElement()
                .asString().contains(LETTER + "02");
    }

    @Test
    void refusesACitationOfADeletedIssueWithThreeDigits() {

        final String document = "Carried over from " + LETTER + "102.";

        assertThat(offenders("doc/decisions.md", document, Set.of("01", "10"))).singleElement()
                .asString().contains(LETTER + "102");
    }

    @Test
    void readsACitationWrittenWithEmphasis() {

        // Bold is invisible to a reader and was fatal to the pattern this replaced.
        final String document = "The matrix was carried over from **" + LETTER + "02**.";

        assertThat(offenders("doc/decisions.md", document, Set.of("01"))).singleElement()
                .asString().contains(LETTER + "02");
    }

    @Test
    void allowsACitationOfAnIssueThatExists() {

        assertThat(offenders("AGENTS.md", "See " + LETTER + "04.", Set.of("04"))).isEmpty();
    }

    @Test
    void allowsANumberOfAnotherSet() {

        // Sokar's B set, the sluice's SL set: other repositories' numbers, checked by nobody here.
        final String document = "Requiring an entry returns with Sokar B55, and SL04 is not ours.";

        assertThat(offenders("build.md", document, Set.of("01"))).isEmpty();
    }

    @Test
    void allowsAnOrdinaryNumberThatIsNotACitation() {

        // The whole point of the letter: a byte count cannot be mistaken for an issue.
        final String document = "Answered with a 302 to cloud storage, 128 bytes, 0600 on the file.";

        assertThat(offenders("AGENTS.md", document, Set.of("01"))).isEmpty();
    }

    /**
     * Returns every citation of an issue that does not exist.
     *
     * @param path Path of the document, for naming the offender.
     * @param text Content of the document.
     * @param live Numbers of the issues that exist, without the letter.
     * @return One sentence per dangling citation; empty when every number is live.
     */
    static List<String> offenders(final String path, final String text, final Set<String> live) {

        final List<String> found = new ArrayList<>();
        final Matcher citation = CITATION.matcher(text);
        while (citation.find()) {
            if (!live.contains(citation.group(1))) {
                found.add(path + " names " + citation.group() + ", which does not exist");
            }
        }
        return found;
    }
}
