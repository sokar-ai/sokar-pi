package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.fuin.sokar.agent.api.AgentException;
import org.fuin.sokar.agent.api.ContainerFile;
import org.fuin.sokar.agent.api.SetupContext;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PiContainerSetup}.
 */
class PiContainerSetupTest {

    private static final String TOKEN = "sokar_pt_example";

    private List<ContainerFile> files(String endpoint) {
        return new PiContainerSetup().files(new org.fuin.sokar.agent.api.SetupContext(
                TOKEN, "api-key", "/workspace", endpoint, "openrouter"));
    }

    /**
     * Returns one written file by its path, so a test says which file it means rather than
     * counting on the order they happen to be written in.
     */
    private static ContainerFile at(List<ContainerFile> files, String path) {
        return files.stream().filter(file -> file.path().equals(path)).findFirst()
                .orElseThrow(() -> new AssertionError("no file at " + path + " in " + files));
    }

    private static ContainerFile routing(List<ContainerFile> files) {
        return at(files, "/home/agent/.pi/agent/extensions/sokar-route.ts");
    }

    private static ContainerFile status(List<ContainerFile> files) {
        return at(files, "/home/agent/.pi/agent/extensions/sokar-status.ts");
    }

    @Test
    void writesExtensionsPiDiscoversOnItsOwn() {

        // Auto-discovered from ~/.pi/agent/extensions, which is why these are files rather than
        // install commands.
        assertThat(files("http://127.0.0.1:9419")).extracting(ContainerFile::path)
                .containsExactlyInAnyOrder("/home/agent/.pi/agent/extensions/sokar-route.ts",
                        "/home/agent/.pi/agent/extensions/sokar-status.ts");
    }

    @Test
    void writesTheEndpointAndProviderItWasGiven() {

        // Neither is this agent's to decide any more. The dialect's path is already on the
        // endpoint - OpenRouter serves the OpenAI dialect under /api/v1, and a base without it
        // answers 404 - and the provider's name arrives with the task, so pointing Pi somewhere
        // else is no longer a change to this binary.
        assertThat(routing(files("http://127.0.0.1:9419/api/v1")).content())
                .contains("\"http://127.0.0.1:9419/api/v1\"")
                .contains("registerProvider(\"openrouter\"");
    }

    @Test
    void carriesTheTaskTokenRatherThanACredential() {

        assertThat(routing(files("http://127.0.0.1:9419")).content()).contains(TOKEN);
        assertThat(routing(files("http://127.0.0.1:9419")).ownerOnly())
                .as("it holds a token, so it is not world readable").isTrue();
    }

    @Test
    void writesNoRoutingWhenNothingWasBrokered() {

        // An agent pointed at nothing would otherwise get an extension naming an endpoint that
        // does not exist, which fails later and further away.
        assertThat(files("")).extracting(ContainerFile::path)
                .containsExactly("/home/agent/.pi/agent/extensions/sokar-status.ts");
    }

    @Test
    void survivesATokenWithCharactersThatWouldBreakTheFile() {

        final List<ContainerFile> files = new PiContainerSetup()
                .files(new org.fuin.sokar.agent.api.SetupContext("tok\"en\\with\nquotes",
                        "api-key", "/workspace", "http://127.0.0.1:9419", "openrouter"));

        assertThat(routing(files).content())
                .as("written as a JSON literal, so a stray quote cannot end the string")
                .contains("\"tok\\\"en\\\\with\\nquotes\"");
    }

    @Test
    void writesNoRoutingWhenThereIsNoTokenToPresent() {

        // The endpoint alone is half a wiring: the extension would carry an empty token, which
        // Pi rejects looking exactly like a wrong one.
        assertThat(new PiContainerSetup().files(new org.fuin.sokar.agent.api.SetupContext(
                "  ", "api-key", "/workspace", "http://127.0.0.1:9419", "openrouter")))
                .extracting(ContainerFile::path)
                .containsExactly("/home/agent/.pi/agent/extensions/sokar-status.ts");
    }

    @Test
    void reportsWhatTheAgentIsDoingWhateverServesIt() {

        // The status extension is written for a brokered task and an unbrokered one alike:
        // whether the host can tell a thinking task from one waiting on a question is not a
        // property of who serves the model.
        assertThat(status(files("http://127.0.0.1:9419")).content())
                .isEqualTo(status(files("")).content());
        assertThat(status(files("")).ownerOnly())
                .as("it carries no credential, so it needs no secrecy").isFalse();
    }

    @Test
    void subscribesToThePromptEventsThatMeanSomebodyIsBeingAsked() {

        // Pi emits these around every blocking prompt it puts to a person, and its own
        // documentation names reporting "waiting for user" as what they are for. They are the
        // only thing that separates a question from a long silence.
        assertThat(status(files("")).content())
                .contains("ui_prompt_start").contains("ui_prompt_end")
                .contains("agent_start").contains("agent_settled");
    }

    @Test
    void offersTheStateInAFileRatherThanSendingItAnywhere() {

        // The agent must not gain a way to write to the host. It writes inside its own container
        // and the host reads when it wants to; nothing is pushed.
        assertThat(status(files("")).content())
                .contains("\"/home/agent/.sokar/agent-state.json\"")
                .as("replaced atomically, so a reader never sees half a state")
                .contains("renameSync");
        assertThat(status(files("")).content())
                .as("no network of any kind")
                .doesNotContain("fetch(").doesNotContain("http");
    }

    @Test
    void refusesABrokeredTaskWithoutAProvider() {

        // Sokar never sends one: no provider chosen means no endpoint either. A file naming the
        // provider "" would override nothing and fail as a wrong credential, so a blank one is
        // Sokar's regression, and saying so is what would find it.
        assertThatThrownBy(() -> new PiContainerSetup().files(new SetupContext(
                "sokar_pt_x", "api-key", "/workspace", "http://127.0.0.1:9419", " ")))
                .isInstanceOf(AgentException.class).hasMessageContaining("empty provider");
    }
}
