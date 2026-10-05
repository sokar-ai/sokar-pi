@credential
Feature: Sokar says what it can tell about the agent waiting for a person, and no more

  Pi declares nothing to read waiting by, because nothing on its screen tells it apart: asked to put a
  question, it writes the question as plain text on what is otherwise its idle screen. So the honest
  answer is "cannot say", and this proves Sokar gives it - not "not waiting", which would tell a person
  a question is not there when it may be. A release that starts drawing something to read is the day
  to declare it, measured, and to replace this with the kit's "waiting for a person".

  Background:
    Given the suite runs as an unprivileged user
    And the environment variable "SOKAR_E2E_OPENROUTER_API_KEY" is set
    And the environment variable "SOKAR_E2E_MODEL" is set

  @slow
  Scenario: an attached Pi at work is shown as one Sokar cannot say is waiting
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    And a project called "asks" of class "guarded" with a file in it
    And a terminal on the machine
    When I run "sokar task start asks --project asks --repository asks --agent pi --clearance deny"
    Then the "pi" agent in task "asks" of "asks" reaches work without being asked anything
    When a script runs "sokar task status sokar-asks-asks"
    Then it exits zero
    And its output has a line matching "screen +cannot say - .*"
    When a script runs "sokar project unfollow asks --force"
    Then it exits zero
