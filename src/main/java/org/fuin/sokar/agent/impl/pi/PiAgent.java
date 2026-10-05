package org.fuin.sokar.agent.impl.pi;

import org.fuin.sokar.agent.api.AgentEnd;
import org.fuin.sokar.agent.api.ContainerSetup;
import org.fuin.sokar.agent.api.LogFormatter;
import org.fuin.sokar.agent.api.YamlAgent;
import org.jspecify.annotations.Nullable;

/**
 * Pi.
 * <p>
 * Two overrides: the endpoint and the token go into an extension Pi discovers, because it has no
 * variable for either; and its JSON events are rendered for the log, because a raw one can hold a
 * whole file. Everything else is {@code pi.yaml}.
 */
public final class PiAgent extends YamlAgent {

    /**
     * Constructor.
     */
    public PiAgent() {
        super("pi");
    }

    @Override
    public ContainerSetup containerSetup() {
        return new PiContainerSetup();
    }

    @Override
    public LogFormatter logFormatter() {
        return new PiJsonFormatter();
    }

    @Override
    public @Nullable AgentEnd ended(String line) {
        return PiJsonFormatter.ended(line);
    }

    /**
     * Entry point of the {@code sokar-agent-pi} binary.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        org.fuin.sokar.agent.api.AgentMain.run(new PiAgent(), args);
    }
}
