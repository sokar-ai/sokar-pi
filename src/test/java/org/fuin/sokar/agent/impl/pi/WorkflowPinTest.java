package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every action a workflow runs is pinned to a commit, with the release it is written beside it.
 * <p>
 * A tag is a name its owner may point anywhere, and these jobs hold the publishing token, the
 * machine credentials and a token that merges pull requests. A commit names one tree. The release
 * in the comment is what a person reads and what Dependabot moves, so a bare hash is refused too.
 */
class WorkflowPinTest {

    private static final Path WORKFLOWS = Path.of(".github/workflows");

    private static final Pattern USES = Pattern.compile("^\\s*(?:-\\s*)?uses:\\s*(.*?)\\s*$", Pattern.MULTILINE);

    private static final Pattern PINNED =
            Pattern.compile("[A-Za-z0-9_.-]+/[A-Za-z0-9_./-]+@[0-9a-f]{40} # v\\d+\\.\\d+\\.\\d+");

    // A step of this repository, or an image by digest: neither is fetched by a name.
    private static final Pattern OURS_OR_DIGEST =
            Pattern.compile("\\./\\S+|docker://\\S+@sha256:[0-9a-f]{64}");

    @Test
    void everyActionIsPinnedToACommit() throws IOException {

        final List<Path> files;
        try (Stream<Path> listed = Files.list(WORKFLOWS)) {
            files = listed.filter(file -> file.toString().endsWith(".yml")).sorted().toList();
        }
        // A walk that finds nothing passes every check below.
        assertThat(files).isNotEmpty();
        int steps = 0;
        for (final Path file : files) {
            final String text = Files.readString(file);
            steps += (int) USES.matcher(text).results().count();
            assertThat(offenders(file.getFileName().toString(), text)).isEmpty();
        }
        assertThat(steps).isPositive();
    }

    @Test
    void refusesAnActionNamedByTag() {

        assertThat(offenders("build.yml", "      - uses: actions/checkout@v7\n")).singleElement().asString()
                .contains("actions/checkout@v7").contains("# vX.Y.Z");
    }

    @Test
    void refusesAPinWithoutTheReleaseBesideIt() {

        assertThat(offenders("build.yml", "      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1\n"))
                .hasSize(1);
    }

    @Test
    void allowsAStepOfThisRepositoryAndAnImageByDigest() {

        assertThat(offenders("build.yml", "      - uses: ./.github/actions/prepare\n"
                + "      - uses: docker://alpine@sha256:" + "0".repeat(64) + "\n")).isEmpty();
    }

    /**
     * Returns every step of one workflow that runs an action by a name rather than a commit.
     *
     * @param file Name of the workflow, to name the offender.
     * @param text Content of the workflow.
     * @return One sentence per step; empty when every one is pinned.
     */
    static List<String> offenders(final String file, final String text) {
        final List<String> problems = new ArrayList<>();
        final Matcher uses = USES.matcher(text);
        while (uses.find()) {
            final String action = uses.group(1);
            if (!PINNED.matcher(action).matches() && !OURS_OR_DIGEST.matcher(action).matches()) {
                problems.add(file + " runs " + action + " - pin it as owner/repo@<full commit> # vX.Y.Z;"
                        + " Dependabot moves the pin");
            }
        }
        return problems;
    }
}
