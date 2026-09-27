package org.fuin.sokar.agent.impl.pi;

import java.util.List;
import org.fuin.sokar.agent.api.AgentException;
import org.fuin.sokar.agent.api.ContainerFile;
import org.fuin.sokar.agent.api.ContainerSetup;
import org.fuin.sokar.agent.api.SetupContext;

/**
 * What Pi needs in a fresh container before it will run unattended.
 * <p>
 * Two files. One carries both halves of the wiring - where to send requests, and what to present -
 * and neither can be an environment variable here: Pi has no variable for an endpoint, and this
 * agent's broker starts after the container exists, so its environment is already fixed. The other
 * records what the agent is doing, for a host that cannot otherwise tell thinking from waiting.
 */
public class PiContainerSetup implements ContainerSetup {

    @Override
    public List<ContainerFile> files(SetupContext context) {

        final List<ContainerFile> files = new java.util.ArrayList<>();

        // Always, and not a secret: it carries no credential, and whether the host can tell a
        // task that is thinking from one waiting on a question must not depend on how this task
        // happens to be served.
        files.add(ContainerFile.of(PiStatusExtension.FILE, PiStatusExtension.document()));

        if (!context.brokered() || !credentialed(context)) {
            // The routing file carries both halves, so with either missing it names an endpoint
            // or a token that is not there, and Pi fails looking like a wrong credential.
            return List.copyOf(files);
        }
        if (context.provider().isBlank()) {
            // Sokar sends a provider with every endpoint; a blank one is a regression on that
            // side, and a file naming "" would override nothing and fail as a wrong credential.
            throw new AgentException("Sokar sent an empty provider for a brokered task - a bug in"
                    + " Sokar, not in the operator's setup: no routing file can be written");
        }
        files.add(ContainerFile.secret(PiRoutingExtension.FILE,
                PiRoutingExtension.document(context.provider(), context.endpoint(),
                        context.token())));
        return List.copyOf(files);
    }

    /**
     * Returns whether this task was given a token to present.
     *
     * @param context What the agent was told about the task.
     * @return {@code true} when there is a token to write.
     */
    private static boolean credentialed(final SetupContext context) {
        return !context.token().isBlank();
    }
}
