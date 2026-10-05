package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pi declares nothing to read waiting by, and that is a measurement, not an omission.
 * <p>
 * On 0.85.0, captured at a terminal, a question it puts is plain text on its idle screen.
 * A rule written without measuring would match nothing and make Sokar say "not waiting" where the true
 * answer is "cannot say". This fails when a declaration appears, so whoever adds one also replaces the
 * acceptance scenario that proves "cannot say" with one that drives Pi to a question.
 */
class WaitingDeclarationTest {

    @Test
    void declaresNothingToReadWaitingBy() {

        assertThat(new PiAgent().definition().waiting()).isNull();
    }
}
