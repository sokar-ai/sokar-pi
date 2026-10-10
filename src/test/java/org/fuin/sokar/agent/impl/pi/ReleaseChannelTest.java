package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs both "Which channel" steps of the build workflow as a tag's run would, against a pom that names a Sokar
 * snapshot and one that names a release. The step itself is what refuses a release built on a snapshot, so the step
 * is run, not looked for.
 */
class ReleaseChannelTest {

    private static final String REFUSAL = "is a snapshot; a release is built against a released Sokar";

    @Test
    void aTagIsRefusedWhileSokarIsASnapshot(@TempDir final Path dir) throws Exception {

        final List<String> steps = whichChannel();
        assertThat(steps).as("\"Which channel\" steps in build.yml, the build job's and the release job's").hasSize(2);
        for (final String step : steps) {
            final Run run = run(step, dir, "0.4.2-SNAPSHOT");
            assertThat(run.exit()).as("exit of a tag on a Sokar snapshot").isNotZero();
            assertThat(run.output()).contains(REFUSAL);
        }
    }

    @Test
    void aTagOnAReleasedSokarIsAReleaseChannel(@TempDir final Path dir) throws Exception {

        // The same steps let a released Sokar through, so the refusal above is the snapshot's and nothing else's.
        for (final String step : whichChannel()) {
            final Run run = run(step, dir, "0.4.2");
            assertThat(run.exit()).as("exit of a tag on a released Sokar:%n%s", run.output()).isZero();
            assertThat(Files.readString(dir.resolve("output"))).contains("channel=releases");
        }
    }

    private record Run(int exit, String output) {
    }

    /** Runs a step's script in a directory holding a pom with version 0.5.0 and the given sokar.version, as tag v0.5.0. */
    private static Run run(final String script, final Path dir, final String sokar) throws IOException, InterruptedException {
        Files.writeString(dir.resolve("pom.xml"), "<project>\n    <version>0.5.0</version>\n    <properties>\n"
                + "        <sokar.version>" + sokar + "</sokar.version>\n    </properties>\n</project>\n");
        Files.writeString(dir.resolve("output"), "");
        final Path file = dir.resolve("step.sh");
        Files.writeString(file, script);
        final ProcessBuilder builder = new ProcessBuilder("bash", file.toString()).directory(dir.toFile())
                .redirectErrorStream(true);
        builder.environment().put("GITHUB_REF_TYPE", "tag");
        builder.environment().put("GITHUB_REF_NAME", "v0.5.0");
        builder.environment().put("GITHUB_OUTPUT", dir.resolve("output").toString());
        final Process process = builder.start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("the step ends").isTrue();
        return new Run(process.exitValue(), output);
    }

    /** The run block of each step named "Which channel" in build.yml, its indentation removed. */
    private static List<String> whichChannel() throws IOException {
        final List<String> lines = Files.readAllLines(RepositoryDocuments.root().resolve(".github/workflows/build.yml"));
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).strip().equals("- name: Which channel")) {
                continue;
            }
            int j = i + 1;
            while (!lines.get(j).strip().equals("run: |")) {
                j++;
            }
            final int key = indentOf(lines.get(j));
            final StringBuilder script = new StringBuilder();
            int body = -1;
            for (int k = j + 1; k < lines.size() && (lines.get(k).isBlank() || indentOf(lines.get(k)) > key); k++) {
                if (lines.get(k).isBlank()) {
                    script.append('\n');
                    continue;
                }
                if (body < 0) {
                    body = indentOf(lines.get(k));
                }
                script.append(lines.get(k).substring(body)).append('\n');
            }
            result.add(script.toString());
        }
        return result;
    }

    private static int indentOf(final String line) {
        return line.length() - line.stripLeading().length();
    }
}
