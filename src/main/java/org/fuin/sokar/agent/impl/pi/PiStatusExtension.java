package org.fuin.sokar.agent.impl.pi;

import org.fuin.sokar.wire.Json;

/**
 * The extension that lets the host see whether Pi is working, idle, or waiting for a person.
 * <p>
 * <strong>Why this exists at all.</strong> Whether an agent is blocked on a question nobody
 * noticed is the one thing about a task that cannot be observed from outside: output stops either
 * way. Pi knows the answer exactly - it emits an event when it puts a prompt to somebody and
 * another when that prompt closes - but those events reach extensions only. They are not on the
 * stream its machine-readable modes serialize, so nothing outside the process can see them.
 * <p>
 * <strong>Why a file rather than a message.</strong> The agent must not gain a way to write to the
 * host: that would be a fourth path out of the container beside the vault socket, the ssh-agent
 * socket and the gate, and the first one pointing outwards. So this writes a file <em>inside</em>
 * the container, and the host reads it when it wants to know. The direction of the boundary is
 * unchanged - nothing is pushed, something is offered.
 * <p>
 * The file is replaced atomically, because a reader that catches a half-written file would see a
 * state that never existed.
 */
final class PiStatusExtension {

    /** Auto-discovered by Pi; the name only has to be unique and end in {@code .ts}. */
    static final String FILE = "/home/agent/.pi/agent/extensions/sokar-status.ts";

    /** Where the state is offered. Inside the container, and read by the host, never written. */
    static final String STATE_FILE = "/home/agent/.sokar/agent-state.json";

    private PiStatusExtension() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the extension that records what Pi is doing.
     *
     * @return File content.
     */
    static String document() {
        // The path is written as a JSON literal for the same reason the routing extension does
        // it: a path pasted into source is a quoting accident waiting to happen.
        return """
                // Written by Sokar. Records what this agent is doing so the host can tell a task
                // that is thinking from one that is waiting for somebody to answer it.
                //
                // It writes a file and sends nothing: the agent has no way to reach the host and
                // does not get one here.
                import { mkdirSync, writeFileSync, renameSync } from "node:fs";
                import { dirname } from "node:path";

                const FILE = %s;

                // What it was doing before a prompt opened, so closing one returns it to that
                // rather than guessing. Prompts nest; only the outermost span matters.
                let base = "idle";
                let waiting = 0;
                let detail = null;

                function write() {
                    const state = waiting > 0 ? "waiting" : base;
                    const body = JSON.stringify({
                        state,
                        detail: waiting > 0 ? detail : null,
                        at: new Date().toISOString(),
                    });
                    try {
                        mkdirSync(dirname(FILE), { recursive: true });
                        // Atomic: a reader never sees half a file.
                        writeFileSync(FILE + ".new", body + "\\n", { mode: 0o600 });
                        renameSync(FILE + ".new", FILE);
                    } catch {
                        // A status file that cannot be written must never take the agent with it.
                    }
                }

                export default function (pi) {
                    pi.on("session_start", () => { base = "idle"; waiting = 0; write(); });
                    pi.on("agent_start", () => { base = "working"; write(); });
                    pi.on("agent_settled", () => { base = "idle"; write(); });
                    pi.on("ui_prompt_start", (event) => {
                        waiting += 1;
                        detail = event && event.title ? String(event.title) : null;
                        write();
                    });
                    pi.on("ui_prompt_end", () => {
                        waiting = Math.max(0, waiting - 1);
                        if (waiting === 0) {
                            detail = null;
                        }
                        write();
                    });
                    pi.on("session_shutdown", () => { base = "gone"; waiting = 0; write(); });
                }
                """.formatted(Json.write(STATE_FILE));
    }
}
