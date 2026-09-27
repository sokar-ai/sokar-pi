package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.fuin.sokar.wire.Json;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The status extension, run as JavaScript on the Node the package ships.
 * <p>
 * Only where a tree has been built, as {@code PinAgreementTest}'s runtime check: the unit phase runs
 * before the tree exists, so on a clean checkout this is skipped - reported as skipped, not passed.
 */
class PiStatusExtensionTest {

    private static final Path NODE = Path.of("target/tree/node/bin/node");

    @TempDir
    Path dir;

    @Test
    void namesThePromptThatIsStillOpen() throws IOException, InterruptedException {

        // A opens, B opens over it, B closes: the person is now answering A, and the file must say A.
        assertThat(run("""
                on.session_start();
                on.ui_prompt_start({ title: "A" });
                on.ui_prompt_start({ title: "B" });
                on.ui_prompt_end();
                """)).contains("\"state\":\"waiting\"").contains("\"detail\":\"A\"");
    }

    @Test
    void returnsToWhatItWasDoingWhenTheLastPromptCloses() throws IOException, InterruptedException {

        assertThat(run("""
                on.session_start();
                on.agent_start();
                on.ui_prompt_start({ title: "A" });
                on.ui_prompt_end();
                """)).contains("\"state\":\"working\"").contains("\"detail\":null");
    }

    private String run(final String events) throws IOException, InterruptedException {
        assumeTrue(Files.isExecutable(NODE), "no built tree at " + NODE);
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
