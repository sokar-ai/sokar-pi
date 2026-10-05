package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.fuin.sokar.agent.api.SessionIds;
import org.fuin.sokar.wire.Json;
import org.junit.jupiter.api.Test;

/**
 * Where Pi names its session, read against what 0.85.0 wrote.
 * <p>
 * That the id is still where this says at the version the package pins is the acceptance suite's
 * question; this one fails when the declaration stops fitting what was measured.
 */
class SessionIdDeclarationTest {

    private static final String ID = "01a0edd8-d083-7050-b048-8f504281b118";

    /** The first record of an unattended run, as it wrote it. */
    private static final List<Object> RECORDS = List.of(
            Json.parse("{\"type\":\"session\",\"version\":3,\"id\":\"" + ID
                    + "\",\"timestamp\":\"2026-09-29T15:46:51.580Z\",\"cwd\":\"/workspace\"}"),
            Json.parse("{\"type\":\"agent_start\"}"));

    private SessionIds ids() {
        final SessionIds ids = new PiAgent().definition().sessionIds();
        assertThat(ids).as("pi.yaml declares where its session id is").isNotNull();
        return ids;
    }

    @Test
    void readsTheSessionAnUnattendedRunOpened() {

        assertThat(ids().of(RECORDS)).isEqualTo(ID);
    }

    @Test
    void findsNoSessionWhereNoRecordNamesOne() {

        // The other half: a declaration that took any record's id would pass the test above.
        assertThat(ids().of(List.of(Json.parse("{\"type\":\"message_start\",\"id\":\"not-a-session\"}")))).isNull();
    }

    @Test
    void looksForAnAttendedSessionInItsSessionFiles() {

        // Measured: ~/.pi/agent/sessions/--workspace--/<timestamp>_<id>.jsonl. Its name is not the id, but its
        // first record is the same {"type":"session"} this declares, and Sokar reads the id from inside it.
        assertThat(ids().directory()).isEqualTo(".pi/agent/sessions/--workspace--");
        assertThat(ids().suffix()).isEqualTo(".jsonl");
        assertThat(ids().of(RECORDS)).isEqualTo(ID);
    }
}
