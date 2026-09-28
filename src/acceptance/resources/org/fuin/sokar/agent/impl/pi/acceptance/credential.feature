@credential
Feature: A task authenticates without ever holding the credential

  The half only a real credential can answer, done as a person does it: typed at a terminal, never
  on a command line. Without SOKAR_E2E_OPENROUTER_API_KEY and SOKAR_E2E_MODEL in the environment of
  the machine running this suite, every scenario here is skipped - and shows as skipped, so a green
  run says what it proved and not more.

  Background:
    Given the suite runs as an unprivileged user
    And the environment variable "SOKAR_E2E_OPENROUTER_API_KEY" is set
    And the environment variable "SOKAR_E2E_MODEL" is set

  Scenario: a person unlocks the vault and the passphrase is not echoed
    # A vault made for this scenario alone, so whatever vault the account holds is never touched.
    # Sokar refuses to unlock a vault that does not exist, so it is created first, asked twice,
    # and locked again: what is measured is the unlock a person does every day.
    Given a terminal on the machine
    When I run "export SOKAR_VAULT=$(mktemp -d)/vault.bin"
    And I run "sokar vault init"
    Then the terminal shows "New vault passphrase:"
    When I type "sokar-acceptance-passphrase"
    Then the terminal shows "New vault passphrase again:"
    When I type "sokar-acceptance-passphrase"
    Then the terminal shows "created "
    When I run "sokar vault unlock --forget"
    # Not "locked": everything seen so far is searched, and the init's "unlocked" contains it.
    Then the terminal shows "the next command asks for the passphrase again"
    When I run "sokar vault unlock"
    Then the terminal shows "Vault passphrase:"
    When I type "sokar-acceptance-passphrase"
    # Only the unlock says "kernel keyring"; the init above says "this account's keyring".
    Then the terminal shows "kernel keyring"
    And the terminal does not show "sokar-acceptance-passphrase"
    When I run "sokar vault unlock --forget && rm -r ${SOKAR_VAULT%/vault.bin}"

  Scenario: a person stores the key, and it is echoed nowhere
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
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
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    When the vault file is read as it lies on disk
    Then it exits zero
    And its output does not contain the value of "SOKAR_E2E_OPENROUTER_API_KEY"

  @slow
  Scenario: a person starts a task with the agent, and the model answers through the broker
    # The real credential stays in the vault; the task gets a token minted for it and nothing
    # else. Only a real credential makes the leak searchable, which is why this needs one.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
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

  @slow
  Scenario: a task nobody attaches to answers through the broker, and the credential is nowhere in it
    # What a script sees rather than a person: the prompt goes in as the task's own command, and
    # the environment, the logs and the broker are asked afterwards rather than looked at. Pi
    # starts without a provider named; the model argument picks OpenRouter.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    And a project called "broker" of class "guarded" with a file in it
    When a task is started in "broker" for the "pi" agent and left running
    And the task's container runs:
      """
      timeout 240 pi --print --model "${SOKAR_E2E_MODEL}" "Reply with exactly the word SOKARLIVE and nothing else."
      """
    Then its output contains "SOKARLIVE"
    And the task's container environment does not contain the value of "SOKAR_E2E_OPENROUTER_API_KEY"
    And no log the task left contains the value of "SOKAR_E2E_OPENROUTER_API_KEY"
    And the task's broker saw a request
    When a script runs "sokar project unfollow broker --force"
    Then it exits zero

  @slow
  Scenario: a person's fresh task reaches work without being asked anything
    # "Reached work, and nothing came first" rather than "the dialogs we know about are absent": a
    # release that adds a question fails this the day it arrives. The agent declares what being at
    # work looks like; the kit types nothing and waits for it.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    And a project called "ready" of class "guarded" with a file in it
    And a terminal on the machine
    When I run "sokar task start ready --project ready --repository ready --agent pi --provider openrouter --clearance deny"
    Then the "pi" agent in task "ready" of "ready" reaches work without being asked anything
    When a script runs "sokar project unfollow ready --force"
    Then it exits zero

  @slow
  Scenario: a task nobody is watching ends rather than waiting on a question
    # A question in a run nobody watches is a run that never ends. The kit stops it at the bound
    # and fails, instead of letting the suite hang with nothing to report.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    And a project called "unwatched" of class "guarded" with a file in it
    When a task nobody is watching is started in "unwatched" for the "pi" agent and ends within 300 seconds
    And a script runs "sokar project unfollow unwatched --force"
    Then it exits zero
