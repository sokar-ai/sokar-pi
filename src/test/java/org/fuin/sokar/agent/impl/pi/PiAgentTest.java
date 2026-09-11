package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.sokar.agent.api.Agent;
import org.fuin.sokar.agent.api.RunRequest;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Pi agent module.
 */
class PiAgentTest {

    private final Agent agent = new PiAgent();

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
}
