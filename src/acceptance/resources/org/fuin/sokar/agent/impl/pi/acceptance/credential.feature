@credential
Feature: A task authenticates without ever holding the credential

  The half only a real credential can answer, done as a person does it: typed at a terminal, never
  on a command line. Without SOKAR_E2E_OPENROUTER_API_KEY and SOKAR_E2E_MODEL in the environment of
  the machine running this suite, every scenario here is skipped - and shows as skipped, so a green
  run says what it proved and not more.

  Background:
    Given the environment variable "SOKAR_E2E_OPENROUTER_API_KEY" is set
    And the environment variable "SOKAR_E2E_MODEL" is set

  Scenario: a person unlocks the vault and the passphrase is not echoed
    # On a machine that has no vault yet, this is the run that sets the passphrase.
    Given a terminal on the machine
    When I run "sokar vault unlock"
    Then the terminal shows "Vault passphrase:"
    When I type "sokar-acceptance-passphrase"
    Then the terminal shows "ready$"
    And the terminal does not show "sokar-acceptance-passphrase"

  Scenario: a person stores the key, and it is echoed nowhere
    Given the vault is unlocked with the passphrase "sokar-acceptance-passphrase"
    And a terminal on the machine
    When I run "sokar vault put openrouter --type api-key"
    Then the terminal shows "Value for 'openrouter':"
    When I type the value of "SOKAR_E2E_OPENROUTER_API_KEY"
    Then the terminal shows "stored"
    And the terminal does not show the value of "SOKAR_E2E_OPENROUTER_API_KEY"
    When I run "sokar vault list"
    Then the terminal shows "openrouter"
    And the terminal does not show the value of "SOKAR_E2E_OPENROUTER_API_KEY"

  Scenario: the credential is not recoverable from the vault file
    Given the vault is unlocked with the passphrase "sokar-acceptance-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    When the vault file is read as it lies on disk
    Then it exits zero
    And its output does not contain the value of "SOKAR_E2E_OPENROUTER_API_KEY"

  @slow
  Scenario: a person starts a task with the agent, and the model answers through the broker
    # The real credential stays in the vault; the task gets a token minted for it and nothing
    # else. Only a real credential makes the leak searchable, which is why this needs one.
    Given the vault is unlocked with the passphrase "sokar-acceptance-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    And a project called "live" of class "guarded" with a file in it
    And a terminal on the machine
    When I run "sokar task start shell --project live --repository live --agent pi --provider openrouter --attach shell --clearance deny"
    And I wait for the shell inside the container
    Then the terminal shows "token"
    And the terminal does not show the value of "SOKAR_E2E_OPENROUTER_API_KEY"
    When I run "pi --print --model ${SOKAR_E2E_MODEL} 'Reply with exactly the word SOKARLIVE and nothing else.'"
    Then within 240 seconds the terminal shows "SOKARLIVE"
    When I run "env"
    Then the terminal shows "OPENROUTER_API_KEY="
    And the terminal does not show the value of "SOKAR_E2E_OPENROUTER_API_KEY"
    When I run "exit"
    Then the terminal shows "ready$"
    # The task is named so its container is known: sokar-<project>-<task>. Checked before it is removed,
    # so a change to that rule fails here with the name shown rather than later with none.
    And the terminal shows "container sokar-live-shell"
    When a script runs "sokar task remove sokar-live-shell --force"
    Then it exits zero
