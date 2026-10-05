package org.fuin.sokar.agent.impl.pi;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.fuin.sokar.agent.api.AgentEnd;
import org.fuin.sokar.agent.api.LogFormatter;
import org.fuin.sokar.wire.Json;
import org.jspecify.annotations.Nullable;

/**
 * Renders the events of {@code pi --print --mode json} as readable lines.
 * <p>
 * Most of what Pi writes is streaming: every token arrives as a {@code message_update} and every
 * chunk of tool output as a {@code tool_execution_update}, and a single line can hold a whole file.
 * The finished message is repeated in {@code message_end}, so the deltas are dropped and the
 * assistant's text is taken from there.
 * <p>
 * A line this formatter does not recognise is shown, cut to a readable length, because for an
 * unattended run the log is the only record there is, and a new event Pi starts writing is exactly
 * what nobody has seen before.
 */
public final class PiJsonFormatter implements LogFormatter {

    /** Lifecycle and streaming events: what they carry arrives again, complete, in a later event. */
    private static final Set<String> SILENT = Set.of("session", "agent_start", "agent_settled",
            "turn_start", "turn_end", "message_start", "message_update", "tool_execution_update");

    /** Prefixes a line the formatter recognised but could not read, so it is visibly not prose. */
    private static final String UNREADABLE = "[unreadable] ";

    /** An HTTP status a provider's refusal starts its message with: {@code 401: ...}, {@code 403 Key ...}. */
    private static final java.util.regex.Pattern LEADING_STATUS =
            java.util.regex.Pattern.compile("^([45]\\d{2})\\b");

    /** Long enough for a sentence or a provider's refusal, short enough to stay one line. */
    private static final int MAX = 300;

    @Override
    public @Nullable String format(String line) {

        final String trimmed = line.strip();
        if (!trimmed.startsWith("{")) {
            return cut(line);
        }

        final Object parsed;
        try {
            parsed = Json.parse(trimmed);
        } catch (RuntimeException ex) {
            // Not JSON after all, or a partial line. Show it.
            return cut(line);
        }
        if (!(parsed instanceof Map<?, ?> event)) {
            return cut(line);
        }

        final String type = String.valueOf(event.get("type"));
        if (SILENT.contains(type)) {
            return null;
        }
        return switch (type) {
            case "message_end" -> message(event, line);
            case "tool_execution_start" -> event.get("toolName") instanceof String tool
                    ? "[" + tool + "]" : UNREADABLE + cut(line);
            case "tool_execution_end" -> !(event.get("toolName") instanceof String tool)
                    ? UNREADABLE + cut(line)
                    : Boolean.TRUE.equals(event.get("isError")) ? "[" + tool + " failed]" : null;
            case "agent_end" -> end(event, line);
            default -> cut(line);
        };
    }

    private static @Nullable String message(Map<?, ?> event, String line) {
        if (!(event.get("message") instanceof Map<?, ?> message)) {
            return UNREADABLE + cut(line);
        }
        // The prompt and every tool result end as a message too; the tool call already showed.
        if (!"assistant".equals(message.get("role"))) {
            return null;
        }
        final String failure = failure(message);
        if (failure != null) {
            return "-- error: " + failure;
        }
        if (!(message.get("content") instanceof List<?> content)) {
            return UNREADABLE + cut(line);
        }
        final StringBuilder out = new StringBuilder();
        for (final Object block : content) {
            // Thinking is skipped, and a tool call is shown by its own event.
            if (block instanceof Map<?, ?> map && "text".equals(map.get("type"))
                    && map.get("text") instanceof String text) {
                out.append(text).append(' ');
            }
        }
        final String text = collapse(out.toString());
        return text.isEmpty() ? null : text;
    }

    private static String end(Map<?, ?> event, String line) {
        if (!(event.get("messages") instanceof List<?> messages)) {
            return UNREADABLE + cut(line);
        }
        int turns = 0;
        Map<?, ?> last = null;
        for (final Object item : messages) {
            if (item instanceof Map<?, ?> message && "assistant".equals(message.get("role"))) {
                turns++;
                last = message;
            }
        }
        final String count = turns + (turns == 1 ? " turn" : " turns");
        final String failure = last == null ? null : failure(last);
        if (failure == null) {
            return "-- done, " + count;
        }
        // Pi retries a refusal it judges transient and ends the run again afterwards, so this end is
        // not the last one and must not read like it.
        if (Boolean.TRUE.equals(event.get("willRetry"))) {
            return "-- error, retrying: " + failure + ", " + count;
        }
        return "-- ended with an error: " + failure + ", " + count;
    }

    /**
     * Reads one line for how the run ended: an {@code agent_end} whose last assistant message decides it.
     * An end Pi is about to retry is not the run's end, because another follows it.
     *
     * @param line One raw line of output.
     * @return How the run ended, or {@code null} for a line that is not its end.
     */
    static @Nullable AgentEnd ended(String line) {
        final String trimmed = line.strip();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        final Object parsed;
        try {
            parsed = Json.parse(trimmed);
        } catch (RuntimeException ex) {
            return null;
        }
        if (!(parsed instanceof Map<?, ?> event) || !"agent_end".equals(event.get("type"))
                || Boolean.TRUE.equals(event.get("willRetry"))
                || !(event.get("messages") instanceof List<?> messages)) {
            return null;
        }
        Map<?, ?> last = null;
        for (final Object item : messages) {
            if (item instanceof Map<?, ?> message && "assistant".equals(message.get("role"))) {
                last = message;
            }
        }
        final Map<?, ?> found = last;
        final String failure = found == null ? null : failure(found);
        if (found == null || failure == null) {
            return new AgentEnd(true, "", AgentEnd.AGENT, null);
        }
        // The provider's refusal carries its HTTP status, as a field or at the head of the message
        // ("401: {...}"); without one it is the agent's own stop.
        if (found.get("errorStatus") instanceof Number status) {
            return new AgentEnd(false, failure, AgentEnd.PROVIDER, status.intValue());
        }
        final java.util.regex.Matcher leading = LEADING_STATUS.matcher(failure);
        return leading.find()
                ? new AgentEnd(false, failure, AgentEnd.PROVIDER, Integer.valueOf(leading.group(1)))
                : new AgentEnd(false, failure, AgentEnd.AGENT, null);
    }

    /**
     * Says why an assistant message failed, the way Pi's own text mode does: its error message, or
     * the stop reason when there is none. An aborted run is a failure too; read as done, it looked
     * like a success with nothing in it.
     */
    private static @Nullable String failure(Map<?, ?> message) {
        final Object reason = message.get("stopReason");
        if (!"error".equals(reason) && !"aborted".equals(reason)) {
            return null;
        }
        if (message.get("errorMessage") instanceof String said && !said.isBlank()) {
            return collapse(said);
        }
        return "request " + reason;
    }

    private static String collapse(String text) {
        return cut(text.strip().replaceAll("\\s+", " "));
    }

    private static String cut(String text) {
        return text.length() > MAX ? text.substring(0, MAX) + "…" : text;
    }
}
