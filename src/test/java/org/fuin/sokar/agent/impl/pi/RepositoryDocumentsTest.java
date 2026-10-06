package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("documents")
class RepositoryDocumentsTest {

    @Test
    void leavesOutWhatIsNotCheckedIn() throws IOException {

        // A machine-local note is a dot file and never reaches CI, so it must not decide a guard here.
        assertThat(RepositoryDocuments.counts(RepositoryDocuments.root().resolve(".AGENTS.md"))).isFalse();
        assertThat(RepositoryDocuments.counts(RepositoryDocuments.root().resolve(".idea/notes.md"))).isFalse();
        assertThat(RepositoryDocuments.counts(RepositoryDocuments.root().resolve("doc/decisions.md"))).isTrue();
        if (Files.exists(RepositoryDocuments.root().resolve(".AGENTS.md"))) {
            assertThat(RepositoryDocuments.all()).doesNotContain(RepositoryDocuments.root().resolve(".AGENTS.md"));
        }
    }
}
