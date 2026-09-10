package org.fuin.sokar.agent.impl.pi;

import java.util.List;
import org.fuin.sokar.agent.api.ContainerFile;
import org.fuin.sokar.agent.api.ContainerSetup;
import org.fuin.sokar.agent.api.SetupContext;

/**
 * What Pi needs in a fresh container before it will run unattended.
 * <p>
 * One file, and it carries both halves of the wiring: where to send requests, and what to present.
 * Neither can be an environment variable here - Pi has no variable for an endpoint, and this
 * agent's broker starts after the container exists, so its environment is already fixed.
 */
public class PiContainerSetup implements ContainerSetup {

    @Override
    public List<ContainerFile> files(SetupContext context) {

        if (!context.brokered() || !credentialed(context)) {
            // This file carries both halves, so with either missing it names an endpoint or a
            // token that is not there, and Pi fails looking like a wrong credential.
            return List.of();
        }
        return List.of(ContainerFile.secret(PiRoutingExtension.FILE,
                PiRoutingExtension.document(context.provider(), context.endpoint(),
                        context.token())));
    }

    /**
     * Returns whether this task was given a token to present.
     *
     * @param context What the agent was told about the task.
     * @return {@code true} when there is a token to write.
     */
    private static boolean credentialed(final SetupContext context) {
        return context.token() != null && !context.token().isBlank();
    }
}
