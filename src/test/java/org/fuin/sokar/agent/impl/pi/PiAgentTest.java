package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.fuin.sokar.agent.api.Agent;
import org.fuin.sokar.agent.api.AgentRegistry;
import org.fuin.sokar.agent.api.PackagedTree;
import org.fuin.sokar.agent.api.RunRequest;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Pi agent module.
 */
class PiAgentTest {

    private final Agent agent = new PiAgent();

    @Test
    void declaresWhatItShowsOnceAtWork() {

        // The kit's check waits for this text and types nothing; a dialog before the prompt hides it.
        assertThat(agent.definition().ready()).isNotNull();
        assertThat(agent.definition().ready().text()).isEqualTo("Pi can explain its own features");
    }

    @Test
    void isDiscoveredThroughTheServiceLoader() {

        // Found through META-INF/services, like claude and omp; the directory was here and empty.
        final AgentRegistry registry = AgentRegistry.discover();

        assertThat(registry.names()).contains("pi");
        assertThat(registry.require("pi").definition().binary()).isEqualTo("pi");
    }

    @Test
    void shipsItsTreeBesideItsBinary() throws IOException {

        // Relative to the binary, the tree follows the agent into an account's own directory; an
        // absolute path would copy the machine's tree whichever copy of the agent ran.
        assertThat(agent.definition().packaged()).extracting(PackagedTree::source)
                .containsExactly("pi/pi-tree.tar.gz");
        // And both packages put it there, beside the binary they install.
        final String pom = Files.readString(Path.of("pom.xml"));
        assertThat(pom).contains("<prefix>${agent.install.dir}/pi</prefix>")
                .contains("<name>${agent.install.dir}/pi/pi-tree.tar.gz</name>")
                .doesNotContain("/usr/share/sokar/agents/pi");
    }

    @Test
    void ignoresWhatTheRepositoryWouldConfigure() {

        // The workspace is untrusted input; its .pi extensions would load after sokar-route.ts.
        assertThat(agent.definition().sandboxedArguments()).containsExactly("--no-approve");
    }

    @Test
    void buildsAHeadlessCommandWithThePromptLast() {

        // The prompt is positional, so it has to come after the flags rather than behind one.
        assertThat(agent.headlessCommand(
                new RunRequest("fix the bug", "z-ai/glm-4.6", null, null, false, true)))
                .containsExactly("pi", "--no-approve", "--model", "z-ai/glm-4.6", "--print", "--mode",
                        "json", "fix the bug");
    }

    @Test
    void resumesWithTheFlagPiItselfUses() {

        assertThat(agent.headlessCommand(
                new RunRequest("carry on", null, null, "01a07a41", false, false)))
                .containsExactly("pi", "--no-approve", "--session", "01a07a41", "carry on");
    }

    @Test
    void startsPiWithoutAVersionCheckOrTelemetry() throws IOException {

        // Pi asks pi.dev for its latest version at start and shows a notice; it installs nothing.
        // A task cannot reach pi.dev, so the request could only fail. Set in the launcher, before
        // the exec, because Pi has no setting for it - only the variable. PI_TELEMETRY=0 stops the
        // install ping there too, and the attribution headers on provider requests.
        final String definition;
        try (InputStream in = PiAgentTest.class.getClassLoader().getResourceAsStream("agent/pi.yaml")) {
            assertThat(in).isNotNull();
            definition = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(definition).contains(
                "#!/bin/sh\\\\nexport PI_SKIP_VERSION_CHECK=1\\\\nexport PI_TELEMETRY=0\\\\nexec /opt/node/bin/node ");
    }

    @Test
    void refusesPiDev() {

        // Where the install ping and the update check go; refused, so the policy is stated, not implied.
        assertThat(agent.definition().refusedDomains()).contains("pi.dev");
    }

    @Test
    void rendersItsLogRatherThanShowingRawEvents() {

        // Without the override the default formatter passes every event through as raw JSON.
        assertThat(new PiAgent().logFormatter().format("{\"type\":\"agent_end\",\"messages\":["
                + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"stop\"}],\"willRetry\":false}"))
                .isEqualTo("-- done, 1 turn");
    }

    @Test
    void isToldHowItsMailboxWorksThroughItsSystemPrompt() {

        // Sokar puts these behind the sandboxed arguments in a task with a mailbox, {file} standing
        // for the instructions' path; the CLI reads the file, so the text never rides on the command line.
        assertThat(agent.definition().instructionArguments()).containsExactly("--append-system-prompt", "{file}");
    }

    @Test
    void saysWhenItIsAtRestAsMeasuredAtATerminal() {

        // Sokar types its wake line only where this matches the last lines of the screen. The screens are
        // cut from ones measured on the VM: after an answer, and during a 'sleep 90' tool call.
        final var atRest = agent.definition().atRest();
        assertThat(atRest).isNotNull();
        assertThat(atRest.shows()).containsExactly("(auto)");
        assertThat(atRest.lacks()).containsExactly("Working ─");
        assertThat(atRest.matches("PONG\n────\n↑1.9k ↓19 $0.000 0.2%/1.0M (auto)      z-ai/glm-5.3-flash • high")).isTrue();
        assertThat(atRest.matches("── ⠹ Working ────────────\n0.0%/1.0M (auto)      z-ai/glm-5.3-flash • high")).isFalse();
    }
}
