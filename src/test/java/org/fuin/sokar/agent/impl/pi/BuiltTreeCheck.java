package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.fuin.sokar.wire.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What only the built tree can answer: the runtime and the tools the package ships, asked themselves.
 * <p>
 * <strong>Run by failsafe in the {@code dist} profile, after the tree is built</strong> at
 * {@code prepare-package}. As a unit test it ran at {@code test}, a phase earlier, so it skipped in every
 * run the pipeline made and read as covered. Here a missing tree fails the run: this profile exists to
 * build one, and a run that did not is not a run that passed.
 */
class BuiltTreeCheck {

    private static final Path TREE = Path.of("target/tree");

    private static final Path NODE = TREE.resolve("node/bin/node");

    @TempDir
    Path dir;

    @Test
    void theBuiltRuntimeReportsThePinnedVersion() throws IOException, InterruptedException {

        // The runtime's digest is checked when it is fetched; this asks the one in the package what it is.
        assertThat(PinAgreementTest.reportDisagreements(version(NODE),
                PinAgreementTest.pinned(PinAgreementTest.pom(), "pin.node.version"))).isEmpty();
    }

    @Test
    void theShippedSearchToolsReportThePinnedVersions() throws IOException, InterruptedException {

        // Pi downloads them when they are missing, which a task cannot; these are the ones it finds.
        final String pom = PinAgreementTest.pom();
        assertThat(version(TREE.resolve("tools/bin/fd")))
                .isEqualTo("fd " + PinAgreementTest.pinned(pom, "pin.fd.version"));
        assertThat(version(TREE.resolve("tools/bin/rg")))
                .startsWith("ripgrep " + PinAgreementTest.pinned(pom, "pin.rg.version") + " ");
    }

    @Test
    void theStatusExtensionNamesThePromptThatIsStillOpen() throws IOException, InterruptedException {

        // A opens, B opens over it, B closes: the person is now answering A, and the file must say A.
        assertThat(runStatusExtension("""
                on.session_start();
                on.ui_prompt_start({ title: "A" });
                on.ui_prompt_start({ title: "B" });
                on.ui_prompt_end();
                """)).contains("\"state\":\"waiting\"").contains("\"detail\":\"A\"");
    }

    @Test
    void theStatusExtensionReturnsToWhatItWasDoingWhenTheLastPromptCloses()
            throws IOException, InterruptedException {

        assertThat(runStatusExtension("""
                on.session_start();
                on.agent_start();
                on.ui_prompt_start({ title: "A" });
                on.ui_prompt_end();
                """)).contains("\"state\":\"working\"").contains("\"detail\":null");
    }

    /** The first line a binary of the tree prints for {@code --version}. */
    private String version(final Path binary) throws IOException, InterruptedException {
        assertThat(Files.isExecutable(binary)).as("%s in the built tree - build-pi-tree.sh ran?", binary)
                .isTrue();
        // To a file rather than a pipe: reading a pipe to its end would wait out a hang, not the timeout.
        final Path out = dir.resolve(binary.getFileName() + ".version");
        final Process process = new ProcessBuilder(binary.toAbsolutePath().toString(), "--version")
                .redirectErrorStream(true).redirectOutput(out.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError(binary + " --version did not finish within 30 seconds");
        }
        return Files.readString(out, StandardCharsets.UTF_8).strip().lines().findFirst().orElse("");
    }

    private String runStatusExtension(final String events) throws IOException, InterruptedException {
        assertThat(Files.isExecutable(NODE)).as("%s in the built tree - build-pi-tree.sh ran?", NODE).isTrue();
        final Path state = dir.resolve("status.json");
        final Path extension = dir.resolve("extension.mjs");
        Files.writeString(extension, PiStatusExtension.document()
                .replace(Json.write(PiStatusExtension.STATE_FILE), Json.write(state.toString())));
        final Path harness = dir.resolve("harness.mjs");
        Files.writeString(harness, """
                import extension from %s;
                const on = {};
                extension({ on: (name, handler) => { on[name] = handler; } });
                %s
                """.formatted(Json.write(extension.toUri().toString()), events));
        final Process node = new ProcessBuilder(NODE.toAbsolutePath().toString(), harness.toString())
                .redirectErrorStream(true).redirectOutput(dir.resolve("node.log").toFile()).start();
        if (!node.waitFor(30, TimeUnit.SECONDS)) {
            node.destroyForcibly();
            throw new AssertionError("node did not finish within 30 seconds");
        }
        assertThat(node.exitValue()).as(Files.readString(dir.resolve("node.log"))).isZero();
        return Files.readString(state, StandardCharsets.UTF_8);
    }
}
