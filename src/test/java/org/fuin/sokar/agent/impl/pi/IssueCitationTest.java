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
import org.junit.jupiter.api.Test;

/**
 * Every issue number a document names is an issue that exists.
 * <p>
 * A link guard cannot see this one: a number written in prose points at nothing once the issue is
 * deleted, and no link breaks.
 * <p>
 * <strong>The letter is what makes this cheap.</strong> While the numbers were three digits the
 * pattern had to find the word "issue" in front of one, and both holes it had were in that word
 * rather than in the number - a capital at the start of a sentence, and emphasis around either.
 * {@code PI07} needs no word, so there is no anchor left to get wrong, and a byte count or an HTTP
 * status can no longer look like a citation.
 * <p>
 * It is sound because this repository takes the number out of the documents in the change that
 * deletes the issue, so a citation that remains is always a pointer and never a record of a moment.
 */
class IssueCitationTest {

    /** This repository's set. Another repository's letter is not ours to check. */
    static final String LETTER = "PI";

    private static final Pattern CITATION = Pattern.compile("\\b" + LETTER + "([0-9]{2})\\b");

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

        // Both sets, because the two ways of being empty fail in opposite directions: no
        // documents and everything passes forever, no live issues and every citation is reported
        // as dangling - a tidy table somebody else acts on. The second was nearly acted on in
        // another repository on 2026-09-20, from a check keyed on filenames during a rename.
        assertThat(RepositoryDocuments.all()).isNotEmpty();
        assertThat(RepositoryDocuments.liveIssues()).isNotEmpty();
    }

    @Test
    void refusesACitationOfADeletedIssue() {

        final String document = "The matrix was carried over from " + LETTER + "02.";

        assertThat(offenders("doc/decisions.md", document, Set.of("01", "03"))).singleElement()
                .asString().contains(LETTER + "02");
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
