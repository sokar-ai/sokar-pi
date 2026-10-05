@credential
Feature: A task that comes back continues the conversation it was having

  Pi declares in its YAML where it names the session it runs. Sokar records that session, and
  starting the task again passes it back, so the agent continues instead of starting over. This proves it
  at the version this package pins: a word to remember in one run, asked for in the next.

  Background:
    Given the suite runs as an unprivileged user
    And the environment variable "SOKAR_E2E_OPENROUTER_API_KEY" is set
    And the environment variable "SOKAR_E2E_MODEL" is set

  @slow
  Scenario: a task nobody watches, started again, continues its session and remembers
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    And a project called "cont" of class "guarded" with a file in it
    When a script runs "timeout 300 sokar task start talk --project cont --repository cont --agent pi --provider openrouter --model ${SOKAR_E2E_MODEL} --prompt 'Remember the word PAPAYA. Reply with OK only.'"
    Then it exits zero
    And sokar says task "talk" of "cont" has a session to continue
    When a script runs "sokar task stop sokar-cont-talk"
    And a script runs "timeout 300 sokar task start talk --project cont --repository cont --agent pi --provider openrouter --model ${SOKAR_E2E_MODEL} --prompt 'Which word did I ask you to remember? Reply with the word only.'"
    Then it exits zero
    And its output contains "session   continuing"
    # The second prompt does not name the word: only the session it continued does.
    And its output contains "PAPAYA"
    When a script runs "sokar project unfollow cont --force"
    Then it exits zero

  @slow
  Scenario: an attached agent stopped and started again continues its conversation
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    And a project called "back" of class "guarded" with a file in it
    And a terminal on the machine
    When I run "sokar task start back --project back --repository back --agent pi --provider openrouter --model ${SOKAR_E2E_MODEL} --clearance deny"
    Then the "pi" agent in task "back" of "back" reaches work without being asked anything
    # Typed, then Enter once the text is on its screen, so it is never taken as a paste.
    When I type "Remember the word PAPAYA. Reply with OK only."
    Then the screen of task "back" of "back" shows "Remember the word PAPAYA"
    When I press Enter
    # Stopped once its answer is on the screen, not after a guessed time: the session is the agent's to
    # write during its first turn, and a stop before the turn ends leaves none to continue.
    Then within 120 seconds this script exits zero:
      """
      podman exec sokar-back-back tmux capture-pane -p -t sokar | grep -v 'Reply with OK' | grep -qw OK
      """
    When a script runs "sleep 3; sokar task stop sokar-back-back"
    Then sokar says task "back" of "back" has a session to continue
    Given a terminal on the machine
    When I run "sokar task start back --project back --repository back --agent pi --provider openrouter --model ${SOKAR_E2E_MODEL} --clearance deny"
    # A fresh session would show an empty conversation; the continued one shows what was said in it.
    Then the screen of task "back" of "back" shows "Remember the word PAPAYA"
    When a script runs "sokar project unfollow back --force"
    Then it exits zero
