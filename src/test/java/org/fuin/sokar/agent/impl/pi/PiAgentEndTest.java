package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.sokar.agent.api.AgentEnd;
import org.junit.jupiter.api.Test;

/**
 * Tests for how {@link PiAgent} reads the end of its run.
 */
class PiAgentEndTest {

    private final PiAgent agent = new PiAgent();

    @Test
    void readsAProvidersRefusalWithItsStatus() {

        // The shape seen in a review run: OpenRouter's 403 at the very end.
        assertThat(agent.ended("{\"type\":\"agent_end\",\"messages\":[{\"role\":\"user\"},"
                + "{\"role\":\"assistant\",\"content\":[],\"stopReason\":\"error\",\"errorStatus\":403,"
                + "\"errorMessage\":\"403 Key limit exceeded (total limit).\"}]}"))
                .isEqualTo(new AgentEnd(false, "403 Key limit exceeded (total limit).", AgentEnd.PROVIDER, 403));
    }

    @Test
    void readsAStatusAtTheHeadOfTheMessageAsTheProviders() {

        // What Pi wrote on the VM for a key OpenRouter refused: no errorStatus, the status leads the text.
        assertThat(agent.ended("{\"type\":\"agent_end\",\"willRetry\":false,\"messages\":[{\"role\":"
                + "\"assistant\",\"content\":[],\"stopReason\":\"error\",\"errorMessage\":"
                + "\"401: {\\\"message\\\":\\\"Missing Authentication header\\\",\\\"code\\\":401}\"}]}"))
                .isEqualTo(new AgentEnd(false, "401: {\"message\":\"Missing Authentication header\",\"code\":401}",
                        AgentEnd.PROVIDER, 401));
    }

    @Test
    void readsAFinishedRun() {

        assertThat(agent.ended("{\"type\":\"agent_end\",\"messages\":[{\"role\":\"assistant\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"done\"}],\"stopReason\":\"stop\"}]}"))
                .isEqualTo(new AgentEnd(true, "", AgentEnd.AGENT, null));
    }

    @Test
    void readsAnAbortedRunAsTheAgentsOwnStop() {

        assertThat(agent.ended("{\"type\":\"agent_end\",\"messages\":[{\"role\":\"assistant\","
                + "\"content\":[],\"stopReason\":\"aborted\"}]}"))
                .isEqualTo(new AgentEnd(false, "request aborted", AgentEnd.AGENT, null));
    }

    @Test
    void doesNotTakeAnEndAboutToBeRetriedForTheRunsEnd() {

        // Another agent_end follows the retry; this one is not how the run ended.
        assertThat(agent.ended("{\"type\":\"agent_end\",\"willRetry\":true,\"messages\":[{\"role\":"
                + "\"assistant\",\"stopReason\":\"error\",\"errorStatus\":429,\"errorMessage\":\"429\"}]}"))
                .isNull();
    }

    @Test
    void answersNothingForALineThatIsNotTheEnd() {

        assertThat(agent.ended("{\"type\":\"turn_end\"}")).isNull();
        assertThat(agent.ended("{\"type\":\"message_update\"}")).isNull();
        assertThat(agent.ended("not json")).isNull();
        assertThat(agent.ended("{\"type\":\"agent_end\",\"messages\":\"not a list\"}")).isNull();
    }
}
