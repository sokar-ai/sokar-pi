@credential
Feature: An agent at rest is woken by a message for it

  Sokar types one line into a task's session when a message for it arrives and its agent's declared
  at_rest matches the screen. These scenarios leave tasks running while they wait for the agent, so they
  are in a file of their own that runs after every other: a wake that fails, and skips its clean-up, takes
  no other scenario down with it. Without SOKAR_E2E_OPENROUTER_API_KEY and SOKAR_E2E_MODEL every scenario
  here is skipped.

  Background:
    Given the suite runs as an unprivileged user
    And the environment variable "SOKAR_E2E_OPENROUTER_API_KEY" is set
    And the environment variable "SOKAR_E2E_MODEL" is set

  @slow
  Scenario: an agent at rest is woken by a message for it, and answers it
    # Sokar types its one wake line into the session when a message for the task arrives and the agent's
    # declared at_rest matches its screen. The message asks for a sum, so the answer is the agent's own, not the
    # message read back.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    # The wake comes from the daemon's passes, which a leased machine's account does not run by itself.
    And a daemon of this scenario's own
    And a project called "wake" of class "guarded" with a file in it
    And a terminal on the machine
    When I run "sokar task start wake --project wake --repository wake --agent pi --provider openrouter --model ${SOKAR_E2E_MODEL} --clearance deny"
    Then the "pi" agent in task "wake" of "wake" reaches work without being asked anything
    When I type "Reply with the single word PONG."
    And I press Enter
    Then within 120 seconds the terminal shows "PONG"
    # An answer is on the screen a moment before its turn ends; the pause makes this the case of an agent at rest.
    When a script runs "sleep 10"
    And a script runs "echo 'What is 1234 plus 4321? Reply with the digits only.' | sokar talk tell sokar-wake-wake"
    Then it exits zero
    And within 120 seconds the terminal shows "A message for you waits"
    # The guide has a message answered with a message, so the answer counts either way the agent gives it:
    # as its reply, held because this project cannot address the person, or on its screen. The kit stops
    # the wait at once when the agent loops.
    Then within 240 seconds this script exits zero:
      """
      held=$(sokar talk held sokar-wake-wake)
      for id in $(printf '%s\n' "$held" | awk '$1 ~ /\.json$/ && $2 == "held" {print $1}'); do
        sokar talk read sokar-wake-wake "$id" | grep -qE '5,?555' && exit 0
      done
      screen=$(podman exec sokar-wake-wake tmux capture-pane -p -S - -t sokar)
      printf '%s\n' "$screen" | grep -qE '5,?555' && exit 0
      printf 'held: %s\nscreen, last lines:\n' "$held"; printf '%s\n' "$screen" | grep -v '^ *$' | tail -15
      exit 1
      """
    When a script runs "sokar task remove sokar-wake-wake --force"
    And a script runs "sokar project unfollow wake --force"
    Then it exits zero

  @slow
  Scenario: a message that arrives while the agent works wakes it once it is at rest
    # The case a single look at the screen misses: told while it works, the agent is announced the message when
    # its turn has ended, not never.
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    # The wake comes from the daemon's passes, which a leased machine's account does not run by itself.
    And a daemon of this scenario's own
    And a project called "busy" of class "guarded" with a file in it
    And a terminal on the machine
    When I run "sokar task start busy --project busy --repository busy --agent pi --provider openrouter --model ${SOKAR_E2E_MODEL} --clearance deny"
    Then the "pi" agent in task "busy" of "busy" reaches work without being asked anything
    When I type "Run the shell command 'sleep 10' with your shell tool, then reply with the single word DONE."
    And I press Enter
    And a script runs "sleep 3"
    And a script runs "echo 'What is 1234 plus 4321? Reply with the digits only.' | sokar talk tell sokar-busy-busy"
    Then it exits zero
    And within 240 seconds the terminal shows "A message for you waits"
    # The guide has a message answered with a message, so the answer counts either way the agent gives it:
    # as its reply, held because this project cannot address the person, or on its screen. The kit stops
    # the wait at once when the agent loops.
    Then within 240 seconds this script exits zero:
      """
      held=$(sokar talk held sokar-busy-busy)
      for id in $(printf '%s\n' "$held" | awk '$1 ~ /\.json$/ && $2 == "held" {print $1}'); do
        sokar talk read sokar-busy-busy "$id" | grep -qE '5,?555' && exit 0
      done
      screen=$(podman exec sokar-busy-busy tmux capture-pane -p -S - -t sokar)
      printf '%s\n' "$screen" | grep -qE '5,?555' && exit 0
      printf 'held: %s\nscreen, last lines:\n' "$held"; printf '%s\n' "$screen" | grep -v '^ *$' | tail -15
      exit 1
      """
    When a script runs "sokar task remove sokar-busy-busy --force"
    And a script runs "sokar project unfollow busy --force"
    Then it exits zero
