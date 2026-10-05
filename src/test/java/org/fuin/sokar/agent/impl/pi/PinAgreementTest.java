package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    private static final List<String> TREE_PINS = List.of("pin.node.version", "pin.node.sha256", "pin.node.image.digest",
            "pin.fd.version", "pin.fd.sha256", "pin.rg.version", "pin.rg.sha256");

    private static final List<String> SCRIPT_PINS = List.of("NODE_VERSION", "NODE_SHA256", "NODE_IMAGE_DIGEST",
            "FD_VERSION", "FD_SHA256", "RG_VERSION", "RG_SHA256");

    private static final Pattern VERSION =
            Pattern.compile("^\\s*version:\\s*\"?([^\"\\n]+)\"?\\s*$", Pattern.MULTILINE);

    private static final Pattern PINNED = Pattern.compile("<agent\\.cli\\.version>([^<]+)</agent\\.cli\\.version>");

    private static final Pattern RELOCK_IMAGE =
            Pattern.compile("<sokar\\.release\\.npm\\.image>([^<]+)</sokar\\.release\\.npm\\.image>");

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
    void theTreesPinsAreAllInThePom() throws IOException {

        assertThat(runtimeDisagreements(pom())).isEmpty();
    }

    @Test
    void refusesAPomWithoutAPinTheTreeNeeds() throws IOException {

        final String pom = pom().replaceFirst("(?m)^\\s*<pin\\.node\\.sha256>.*$", "");

        assertThat(runtimeDisagreements(pom)).singleElement().asString().contains("pin.node.sha256");
    }

    @Test
    void theBuildScriptKeepsNoPinOfItsOwn() throws IOException {

        // One place for each pin, the pom, which the release tool moves: a default in the script would be
        // a second copy that quietly goes stale, and an override the environment could slip in.
        assertThat(scriptDefaults(script())).isEmpty();
        assertThat(scriptDefaults("NODE_VERSION=\"${NODE_VERSION:-22.0.0}\"\n")).containsExactly("NODE_VERSION");
    }

    @Test
    void everyInstallInTheBuildScriptRunsNoScripts() throws IOException {

        // A lifecycle script runs after the integrity check, with the shipped tree mounted writable.
        assertThat(installsRunningScripts(script())).isEmpty();
        assertThat(installsRunningScripts(script().replace(" --ignore-scripts", ""))).hasSize(2);
    }

    @Test
    void theUpdateRelocksInTheImageTheTreeIsBuiltIn() throws IOException {

        assertThat(relockImageDisagreements(pom())).isEmpty();
    }

    @Test
    void refusesABuilderImageNamedByTagAlone() throws IOException {

        // A tag is a name its owner may repoint, and npm ci, the Node download and its checksum all
        // run inside that image: whoever repoints it controls the checks that protect the package.
        final String pom = pom().replaceFirst("(?m)^\\s*<pin\\.node\\.image\\.digest>.*$", "");

        assertThat(runtimeDisagreements(pom)).singleElement().asString().contains("pin.node.image.digest");
    }

    @Test
    void refusesARelockImageThatIsNotTheBuilder() throws IOException {

        // A lockfile resolved by one npm and installed by another is how a tree drifts from its
        // lockfile. The release tool reads the pom uninterpolated, so the image is written out there
        // and this is what keeps it the builder's.
        final String pom = RELOCK_IMAGE.matcher(pom())
                .replaceFirst("<sokar.release.npm.image>docker.io/library/node:23.0.0-slim</sokar.release.npm.image>");

        assertThat(relockImageDisagreements(pom)).singleElement().asString()
                .contains("node:23.0.0-slim");
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
     * Returns every way the image the update relocks in differs from the one the tree is built in.
     *
     * @param pom Content of {@code pom.xml}.
     * @return One sentence per disagreement; empty when they are the same image.
     */
    static List<String> relockImageDisagreements(final String pom) {

        final Matcher configured = RELOCK_IMAGE.matcher(pom);
        if (!configured.find()) {
            return List.of("pom.xml declares no sokar.release.npm.image");
        }
        final String builder = "docker.io/library/node:" + pinned(pom, "pin.node.version") + "-slim@"
                + pinned(pom, "pin.node.image.digest");
        final String relock = configured.group(1).strip();
        return relock.equals(builder) ? List.of()
                : List.of("the update relocks in " + relock + ", build-pi-tree.sh builds in " + builder);
    }

    /**
     * Returns every pin of the tree the pom does not carry.
     *
     * @param pom Content of {@code pom.xml}.
     * @return One sentence per missing pin; empty when all are there.
     */
    static List<String> runtimeDisagreements(final String pom) {

        final List<String> problems = new ArrayList<>();
        for (final String name : TREE_PINS) {
            if (pinned(pom, name) == null) {
                problems.add("pom.xml pins no " + name + " - build-pi-tree.sh would refuse to run");
            }
        }
        return problems;
    }

    /**
     * Returns the names of pins the build script defaults itself, as {@code NAME="${NAME:-value}"}.
     * <p>
     * Only the pins: {@code JAVA_CMD} and the builder override fall back on purpose and are not pins.
     *
     * @param script Content of {@code build-pi-tree.sh}.
     * @return The names; empty when the script keeps none.
     */
    static List<String> scriptDefaults(final String script) {
        final List<String> names = new ArrayList<>();
        final Matcher matcher = Pattern.compile("^([A-Z0-9_]+)=\"\\$\\{\\1:-[^}]*\\}\"", Pattern.MULTILINE).matcher(script);
        while (matcher.find()) {
            if (SCRIPT_PINS.contains(matcher.group(1))) {
                names.add(matcher.group(1));
            }
        }
        return names;
    }

    /**
     * Returns every line of the build script that runs {@code npm ci} without {@code --ignore-scripts}.
     *
     * @param script Content of {@code build-pi-tree.sh}.
     * @return The lines, stripped; empty when every install disables scripts.
     */
    static List<String> installsRunningScripts(final String script) {
        return script.lines().map(String::strip)
                .filter(line -> !line.startsWith("#") && line.matches(".*\\bnpm ci\\b.*"))
                .filter(line -> !line.contains("--ignore-scripts")).toList();
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

    /**
     * Returns a pom property's value.
     *
     * @param pom Content of {@code pom.xml}.
     * @param name The property.
     * @return Its value, or {@code null} when the pom has none.
     */
    static String pinned(final String pom, final String name) {
        final Matcher matcher = Pattern.compile("<" + Pattern.quote(name) + ">([^<]+)</" + Pattern.quote(name) + ">")
                .matcher(pom);
        return matcher.find() ? matcher.group(1).strip() : null;
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

    static String pom() throws IOException {
        return Files.readString(Path.of("pom.xml"));
    }

    private static String manifest() throws IOException {
        return Files.readString(MANIFEST);
    }

    private static String lockfile() throws IOException {
        return Files.readString(LOCKFILE);
    }

    static String script() throws IOException {
        return Files.readString(BUILD_SCRIPT);
    }
}
