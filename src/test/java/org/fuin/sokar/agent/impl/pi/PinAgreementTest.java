package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fuin.sokar.wire.Json;
import org.junit.jupiter.api.Test;

/**
 * The shipped Pi version, and the Node runtime it runs on, agree everywhere they are written.
 * <p>
 * This agent's pin is not a URL and a digest: the tree is built here and shipped inside the
 * package, so the question is whether the definition, the pom, the manifest the tree is built from
 * and the lockfile that resolves it all name one version - and whether the runtime is the reviewed
 * one. Nothing here needs the network, because everything the package installs is in the
 * repository.
 * <p>
 * Each refusal below is reproduced from the real files rather than from a hand-written sample, so a
 * change to their shape breaks the test instead of passing it.
 */
class PinAgreementTest {

    private static final String DEFINITION = "agent/pi.yaml";

    private static final String PACKAGE = "@earendil-works/pi-coding-agent";

    private static final Path MANIFEST = Path.of("src/main/npm/package.json");

    private static final Path LOCKFILE = Path.of("src/main/npm/package-lock.json");

    private static final Path BUILD_SCRIPT = Path.of("buildtools/build-pi-tree.sh");

    private static final Path BUILT_NODE = Path.of("target/tree/node/bin/node");

    private static final List<String> RUNTIME = List.of("NODE_VERSION", "NODE_SHA256");

    private static final Pattern VERSION =
            Pattern.compile("^\\s*version:\\s*\"?([^\"\\n]+)\"?\\s*$", Pattern.MULTILINE);

    private static final Pattern PINNED = Pattern.compile("<agent\\.cli\\.version>([^<]+)</agent\\.cli\\.version>");

    @Test
    void theBuiltDefinitionAgreesWithThePomTheManifestAndTheLockfile() throws IOException {

        assertThat(disagreements(filtered(), pom(), manifest(), lockfile())).isEmpty();
    }

    @Test
    void refusesADefinitionThatWasNeverFiltered() throws IOException {

        // The source template is exactly what a build that skipped filtering would ship.
        final String template = Files.readString(Path.of("src/main/resources", DEFINITION));

        assertThat(disagreements(template, pom(), manifest(), lockfile())).singleElement().asString()
                .contains("filtering did not run");
    }

    @Test
    void refusesAPomThatPinsAnotherVersion() throws IOException {

        final String pom = PINNED.matcher(pom()).replaceFirst("<agent.cli.version>0.0.1</agent.cli.version>");

        assertThat(disagreements(filtered(), pom, manifest(), lockfile())).singleElement().asString()
                .contains("pom.xml pins 0.0.1");
    }

    @Test
    void refusesAManifestThatAsksForAnotherVersion() throws IOException {

        final String manifest = manifest().replaceFirst(
                "(\"" + Pattern.quote(PACKAGE) + "\"\\s*:\\s*\")[^\"]+", "$10.0.1");

        assertThat(disagreements(filtered(), pom(), manifest, lockfile())).singleElement().asString()
                .contains("package.json asks for 0.0.1");
    }

    @Test
    void refusesALockfileThatResolvesAnotherVersion() throws IOException {

        // The one that decides what an operator gets: npm ci installs the lockfile and ignores what
        // package.json asked for.
        final String lockfile = lockfile().replaceFirst(
                "(\"node_modules/" + Pattern.quote(PACKAGE) + "\"\\s*:\\s*\\{\\s*\"version\"\\s*:\\s*\")[^\"]+",
                "$10.0.1");

        assertThat(disagreements(filtered(), pom(), manifest(), lockfile)).singleElement().asString()
                .contains("the lockfile resolves 0.0.1");
    }

    @Test
    void theRuntimeIsTheReviewedOne() throws IOException {

        assertThat(runtimeDisagreements(script(), System.getenv())).isEmpty();
    }

    @Test
    void refusesARuntimeOverriddenFromTheEnvironment() throws IOException {

        // An override is not forbidden for a developer trying something; it is forbidden silently,
        // because the package would then carry a runtime nobody reviewed while every pin stayed green.
        assertThat(runtimeDisagreements(script(), Map.of("NODE_VERSION", "23.0.0"))).singleElement().asString()
                .contains("NODE_VERSION is overridden");
    }

    @Test
    void refusesABuildScriptWithoutAReviewedDefault() throws IOException {

        final String script = script().replaceFirst("(?m)^NODE_SHA256=.*$", "");

        assertThat(runtimeDisagreements(script, Map.of())).singleElement().asString()
                .contains("no default for NODE_SHA256");
    }

    @Test
    void theBuiltRuntimeReportsThePinnedVersion() throws IOException, InterruptedException {

        // Only where a tree has been built. The unit phase runs before the tree is built, so on a
        // clean checkout this is skipped - reported as skipped, not passed.
        assumeTrue(Files.isExecutable(BUILT_NODE), "no built tree at " + BUILT_NODE);

        final Process node = new ProcessBuilder(BUILT_NODE.toString(), "--version").redirectErrorStream(true).start();
        final String reported = new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        assertThat(node.waitFor(30, TimeUnit.SECONDS)).isTrue();

        assertThat(reportDisagreements(reported, reviewedDefault(script(), "NODE_VERSION"))).isEmpty();
    }

    @Test
    void refusesABuiltRuntimeThatReportsAnotherVersion() {

        assertThat(reportDisagreements("v23.0.0", "22.20.0")).singleElement().asString()
                .contains("reports v23.0.0");
    }

    /**
     * Returns every way the definition, the pom, the manifest and the lockfile disagree.
     *
     * @param definition Definition text.
     * @param pom Content of {@code pom.xml}.
     * @param manifest Content of {@code package.json}.
     * @param lockfile Content of {@code package-lock.json}.
     * @return One sentence per disagreement; empty when they agree.
     */
    static List<String> disagreements(final String definition, final String pom, final String manifest,
            final String lockfile) {

        final Matcher found = VERSION.matcher(definition);
        if (!found.find()) {
            return List.of("the definition names no version");
        }
        final String shipped = found.group(1).strip();
        if (shipped.contains("${")) {
            return List.of("resource filtering did not run - the definition still reads " + shipped);
        }

        final List<String> problems = new ArrayList<>();
        final Matcher pinned = PINNED.matcher(pom);
        if (!pinned.find()) {
            problems.add("pom.xml declares no agent.cli.version");
        } else if (!pinned.group(1).strip().equals(shipped)) {
            problems.add("pom.xml pins " + pinned.group(1).strip() + ", the definition ships " + shipped);
        }
        final Object asked = map(map(Json.parse(manifest)).get("dependencies")).get(PACKAGE);
        if (!shipped.equals(asked)) {
            problems.add("package.json asks for " + asked + ", the definition ships " + shipped);
        }
        final Object resolved =
                map(map(map(Json.parse(lockfile)).get("packages")).get("node_modules/" + PACKAGE)).get("version");
        if (!shipped.equals(resolved)) {
            problems.add("the lockfile resolves " + resolved + ", and npm ci installs the lockfile");
        }
        return problems;
    }

    /**
     * Returns every way the Node runtime would not be the reviewed one.
     *
     * @param script Content of {@code build-pi-tree.sh}.
     * @param environment The environment the build would run with.
     * @return One sentence per disagreement; empty when the runtime is the reviewed one.
     */
    static List<String> runtimeDisagreements(final String script, final Map<String, String> environment) {

        final List<String> problems = new ArrayList<>();
        for (final String name : RUNTIME) {
            final String reviewed = reviewedDefault(script, name);
            if (reviewed == null) {
                problems.add("build-pi-tree.sh declares no default for " + name);
                continue;
            }
            final String given = environment.get(name);
            if (given != null && !given.equals(reviewed)) {
                problems.add(name + " is overridden in the environment - the package would not carry the reviewed "
                        + "runtime");
            }
        }
        return problems;
    }

    /**
     * Returns what the built runtime reports, compared with the reviewed version.
     *
     * @param reported Output of {@code node --version}.
     * @param version The reviewed version, without the leading 'v'.
     * @return One sentence when they differ; empty when they agree.
     */
    static List<String> reportDisagreements(final String reported, final String version) {
        return reported.equals("v" + version)
                ? List.of()
                : List.of("the built runtime reports " + reported + ", the build script pins v" + version);
    }

    private static String reviewedDefault(final String script, final String name) {
        final Matcher matcher = Pattern.compile("^" + name + "=\"\\$\\{" + name + ":-([^}]+)\\}\"", Pattern.MULTILINE)
                .matcher(script);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Map<?, ?> map(final Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String filtered() throws IOException {
        try (InputStream in = PinAgreementTest.class.getClassLoader().getResourceAsStream(DEFINITION)) {
            assertThat(in).as("%s on the classpath - build first", DEFINITION).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String pom() throws IOException {
        return Files.readString(Path.of("pom.xml"));
    }

    private static String manifest() throws IOException {
        return Files.readString(MANIFEST);
    }

    private static String lockfile() throws IOException {
        return Files.readString(LOCKFILE);
    }

    private static String script() throws IOException {
        return Files.readString(BUILD_SCRIPT);
    }
}
