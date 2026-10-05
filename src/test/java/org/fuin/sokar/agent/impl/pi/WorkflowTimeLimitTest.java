package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class WorkflowTimeLimitTest {

    private static final Pattern JOB = Pattern.compile("^  ([a-z][a-z0-9_-]*):$");

    @Test
    void everyJobStopsAfterALimit() throws IOException {

        // GitHub's default is six hours, and a leg on a rented machine pays for every minute of a hang.
        final List<String> unlimited = new ArrayList<>();
        try (Stream<Path> files = Files.list(RepositoryDocuments.root().resolve(".github/workflows"))) {
            for (final Path file : files.filter(f -> f.toString().endsWith(".yml")).sorted().toList()) {
                boolean inJobs = false;
                String job = null;
                boolean limited = false;
                for (final String line : Files.readAllLines(file)) {
                    if (line.equals("jobs:")) {
                        inJobs = true;
                        continue;
                    }
                    final Matcher m = JOB.matcher(line);
                    if (inJobs && m.matches()) {
                        if (job != null && !limited) {
                            unlimited.add(file.getFileName() + " " + job);
                        }
                        job = m.group(1);
                        limited = false;
                    } else if (line.matches("^    timeout-minutes: [1-9][0-9]*$")) {
                        limited = true;
                    }
                }
                if (job != null && !limited) {
                    unlimited.add(file.getFileName() + " " + job);
                }
            }
        }
        assertThat(unlimited).as("jobs without timeout-minutes").isEmpty();
    }
}
