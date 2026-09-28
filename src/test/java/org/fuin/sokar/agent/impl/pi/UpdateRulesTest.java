package org.fuin.sokar.agent.impl.pi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The update rules the operator decided, kept where Sokar's release tool and the update job read them.
 * <p>
 * The tool applies the rules; what can break here is the configuration that asks for them. A pin
 * the pom carries but the tool is not told about, or that the job does not follow, is never moved -
 * and nothing fails, because a pin nobody asks about is always up to date.
 */
class UpdateRulesTest {

    private static final Path WORKFLOW = Path.of(".github/workflows/update.yml");

    private static final Pattern TREE_PIN = Pattern.compile("<pin\\.([a-z0-9-]+)\\.version>");

    private static final Pattern DECLARED_PIN =
            Pattern.compile("<sokar\\.release\\.pin\\.([a-z0-9-]+)\\.property>([^<]*)</");

    private static final Pattern FOLLOWED = Pattern.compile("'\\[(\"cli\"(?:,\"[a-z0-9-]+\")*)\\]'");

    @Test
    void aReleaseIsTakenOnceItIsThreeDaysOld() throws IOException {

        assertThat(PinAgreementTest.pinned(PinAgreementTest.pom(), "sokar.release.min-age")).isEqualTo("3d");
    }

    @Test
    void findsNoAgeInAPomThatSetsNone() {

        // The other half: a pom that sets no age must not read as one that does.
        assertThat(PinAgreementTest.pinned("<properties><sokar.release.channel>x</sokar.release.channel></properties>",
                "sokar.release.min-age")).isNull();
    }

    @Test
    void everyTreePinIsMovedByTheReleaseTool() throws IOException {

        assertThat(undeclaredPins(PinAgreementTest.pom())).isEmpty();
    }

    @Test
    void refusesATreePinTheReleaseToolIsNotTold() throws IOException {

        final String pom = PinAgreementTest.pom().replace("<pin.rg.sha256>",
                "<pin.jq.version>1.8.1</pin.jq.version>\n<pin.rg.sha256>");

        assertThat(undeclaredPins(pom)).singleElement().asString().contains("pin.jq.version");
    }

    @Test
    void theUpdateJobFollowsTheCliAndEveryDeclaredPin() throws IOException {

        assertThat(unfollowedPins(PinAgreementTest.pom(), Files.readString(WORKFLOW))).isEmpty();
    }

    @Test
    void refusesAPinTheUpdateJobDoesNotFollow() throws IOException {

        final String workflow = Files.readString(WORKFLOW).replace("\"fd\",\"rg\"]'", "\"fd\"]'");

        assertThat(unfollowedPins(PinAgreementTest.pom(), workflow)).singleElement().asString().contains("rg");
    }

    /**
     * Returns every tree pin whose version the release tool is not told to move.
     *
     * @param pom Content of {@code pom.xml}.
     * @return One sentence per pin; empty when the tool moves all of them.
     */
    static List<String> undeclaredPins(final String pom) {
        final Set<String> moved = new TreeSet<>();
        final Matcher declared = DECLARED_PIN.matcher(pom);
        while (declared.find()) {
            moved.add(declared.group(2).strip());
        }
        final List<String> problems = new ArrayList<>();
        final Matcher pins = TREE_PIN.matcher(pom);
        while (pins.find()) {
            final String property = "pin." + pins.group(1) + ".version";
            if (!moved.contains(property)) {
                problems.add(property + " is pinned, but no sokar.release.pin.*.property names it");
            }
        }
        return problems;
    }

    /**
     * Returns every way what the update job follows differs from the CLI and the pins the pom declares.
     *
     * @param pom Content of {@code pom.xml}.
     * @param workflow Content of the update workflow.
     * @return One sentence per difference; empty when the job follows exactly those.
     */
    static List<String> unfollowedPins(final String pom, final String workflow) {
        final Matcher list = FOLLOWED.matcher(workflow);
        if (!list.find()) {
            return List.of(WORKFLOW + " has no matrix list starting with \"cli\"");
        }
        final Set<String> followed = new TreeSet<>(List.of(list.group(1).replace("\"", "").split(",")));
        followed.remove("cli");
        final Set<String> declared = new TreeSet<>();
        final Matcher pins = DECLARED_PIN.matcher(pom);
        while (pins.find()) {
            declared.add(pins.group(1));
        }
        return followed.equals(declared) ? List.of()
                : List.of("the update job follows " + followed + ", the pom declares the pins " + declared);
    }
}
