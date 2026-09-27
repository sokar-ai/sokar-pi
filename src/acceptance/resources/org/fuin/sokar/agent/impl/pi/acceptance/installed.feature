Feature: The published package on a clean machine

  Everything before this proved the code was right; this proves that what an operator gets is.
  The packages were installed from the repository by the run that rented this machine, the way an
  operator installs them - and this agent's binary was built in another repository against a
  published contract, so sokar has never heard of it.

  Background:
    Given the suite runs as an unprivileged user

  Scenario: sokar is installed and discovers the agent it was never linked against
    When a script runs "sokar --version"
    Then it exits zero
    And a script running "sokar agents" mentions "pi"

  Scenario: a person sees the agent by its name and its label
    Given a terminal on the machine
    When I run "sokar agents"
    Then the terminal shows "pi"
    And the terminal shows "Pi"

  Scenario: the hooks register for this user
    When a script runs "sokar setup"
    Then it exits zero

  Scenario: doctor says what this machine can do
    Given a terminal on the machine
    When I run "sokar doctor"
    Then the terminal shows "hooks registered"

  Scenario: the installed version is a snapshot the next build supersedes
    # A flat snapshot is the same version every build, so 'apt upgrade' has nothing to do and
    # whoever installed yesterday stays there. Asked of dpkg or rpm, not reasoned about.
    Then the installed package "sokar-agent-pi" is a snapshot that the next build supersedes

  Scenario: the package carries a bill naming what it installs
    # Checked on a machine that installed the package rather than in the build that made it: an
    # update gate diffs this against the published one, and a missing bill breaks it silently.
    Then the bill at "/usr/share/sokar/sbom/sokar-agent-pi.cdx.json" names "sokar-agent-pi-tree"

  Scenario: the adapter ships the Pi its bill names
    # Two facts written by two different steps of the build. The scope is stripped in a bill, so
    # the package appears under its bare name, nested in the tree.
    Then the "pi" agent installs the version the bill at "/usr/share/sokar/sbom/sokar-agent-pi.cdx.json" names for "pi-coding-agent"

  Scenario: an update run installs the version it was asked to test
    # Set only by the update job, which tests its candidate packages rather than the published ones.
    Given the environment variable "SOKAR_E2E_EXPECT_CLI" is set
    Then the "pi" agent installs the version in the environment variable "SOKAR_E2E_EXPECT_CLI"
