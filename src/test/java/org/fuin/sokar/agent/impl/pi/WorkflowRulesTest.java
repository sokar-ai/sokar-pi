package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowRulesTest {

    private static final Path WORKFLOWS = RepositoryDocuments.root().resolve(".github/workflows");

    @Test
    void noPackageBuildSkipsTheTests() throws IOException {

        // -DskipTests also skips NativeLinkageCheck and the built-tree checks, the ones that need what was built.
        final List<String> skipping = new ArrayList<>();
        try (Stream<Path> files = Files.list(WORKFLOWS)) {
            for (final Path file : files.filter(f -> f.toString().endsWith(".yml")).sorted().toList()) {
                final StringBuilder joined = new StringBuilder();
                for (final String line : Files.readAllLines(file)) {
                    if (line.endsWith("\\")) {
                        joined.append(line, 0, line.length() - 1);
                        continue;
                    }
                    joined.append(line);
                    if (joined.indexOf("-Pnative,dist") >= 0 && joined.indexOf("-DskipTests") >= 0) {
                        skipping.add(file.getFileName() + ": " + joined.toString().strip());
                    }
                    joined.setLength(0);
                }
            }
        }
        assertThat(skipping).as("package builds with -DskipTests").isEmpty();
    }

    @Test
    void anEmptyProviderKeyStopsTheAcceptanceLegs() throws IOException {

        // Without it the credential scenarios skip, and a leg passes on less than it claims.
        assertThat(Files.readString(WORKFLOWS.resolve("build.yml")))
                .contains("-n \"${SOKAR_E2E_OPENROUTER_API_KEY:-}\"");
        assertThat(Files.readString(WORKFLOWS.resolve("update.yml")))
                .contains("-n \"$KEY\"")
                .contains("secrets.OPEN_ROUTER_API_KEY");
    }

    @Test
    void noFoldedPackageBuildSkipsItsTests() throws IOException {

        // A command folded onto more-indented lines, with or without a backslash, is still one command.
        final List<String> untested = new ArrayList<>();
        try (Stream<Path> files = Files.list(RepositoryDocuments.root().resolve(".github/workflows"))) {
            for (final Path file : files.filter(f -> f.toString().endsWith(".yml")).sorted().toList()) {
                final List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (!lines.get(i).contains("-Pnative,dist")) {
                        continue;
                    }
                    final int indent = indentOf(lines.get(i));
                    for (int j = i; j < lines.size() && (j == i || !lines.get(j).isBlank() && indentOf(lines.get(j)) > indent); j++) {
                        if (lines.get(j).contains("-DskipTests")) {
                            untested.add(file.getFileName() + ":" + (j + 1));
                        }
                    }
                }
            }
        }
        assertThat(untested).as("package builds that skip their tests on a folded line").isEmpty();
    }

    private static int indentOf(final String line) {
        return line.length() - line.stripLeading().length();
    }
}
