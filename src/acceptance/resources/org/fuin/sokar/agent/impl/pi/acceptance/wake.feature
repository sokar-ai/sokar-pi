@credential
Feature: Pi is never woken, because nothing on its screen tells an open question from rest

  Sokar types a wake line - for a message, a handed-in file or a build verdict - only into an agent that
  declares both at_rest and waiting, since a line typed into a question nobody can see is open becomes its
  answer. Pi declares no waiting: a question it puts is plain text on a resting screen. So a message and a
  file reach its task, and nothing is typed into its session; it finds them when it looks, as its guide says.
  This scenario leaves a task running while it waits, so it is in a file of its own that runs after every
  other. Without SOKAR_E2E_OPENROUTER_API_KEY and SOKAR_E2E_MODEL it is skipped.

  Background:
    Given the suite runs as an unprivileged user
    And the environment variable "SOKAR_E2E_OPENROUTER_API_KEY" is set
    And the environment variable "SOKAR_E2E_MODEL" is set

  @slow
  Scenario: a message and a handed-in file reach Pi at rest, and nothing is typed into its session
    Given a vault of this scenario's own, unlocked with the passphrase "scenario-vault-passphrase"
    And the vault holds the value of "SOKAR_E2E_OPENROUTER_API_KEY" as "openrouter" of kind "api-key"
    # A wake would come from the daemon's passes, which a leased machine's account does not run by itself.
    And a daemon of this scenario's own
    And a project called "wake" of class "guarded" with a file in it
    And a terminal on the machine
    When I run "sokar task start wake --project wake --repository wake --agent pi --provider openrouter --model ${SOKAR_E2E_MODEL} --clearance deny"
    Then the "pi" agent in task "wake" of "wake" reaches work without being asked anything
    When I type "Reply with the single word PONG."
    And I press Enter
    # The model's answer, not the typed line, which has the word in it too.
    Then within 120 seconds this script exits zero:
      """
      podman exec sokar-wake-wake tmux capture-pane -p -t sokar | grep -v 'single word PONG' | grep -qw PONG
      """
    # An answer is on the screen a moment before its turn ends. At rest is what pi.yaml's at_rest reads, so what
    # comes below comes to Pi at rest, the case in which an agent that declares waiting would be woken.
    Then within 60 seconds this script exits zero:
      """
      screen=$(podman exec sokar-wake-wake tmux capture-pane -p -t sokar)
      printf '%s\n' "$screen" | grep -qF '(auto)' || exit 1
      ! printf '%s\n' "$screen" | grep -qF 'Working ─'
      """
    When a script runs "echo 'What is 1234 plus 4321? Reply with the digits only.' | sokar talk tell sokar-wake-wake"
    Then it exits zero
    When a script runs "echo 'a specification' > /tmp/sokar-wake-spec.txt && sokar task give sokar-wake-wake /tmp/sokar-wake-spec.txt"
    Then it exits zero
    # Both reached the task, so a wake would be due from here on.
    Then within 60 seconds this script exits zero:
      """
      podman exec sokar-wake-wake sh -c 'find /run/sokar/mail/inbox/new /run/sokar/mail/inbox/cur -type f | grep -q . && test -s /sokar/files/sokar-wake-spec.txt'
      """
    # The command that delivered each would have typed its line at once, and the daemon's message watcher passes
    # every 5 seconds while idle: three of its passes go by before the screen is read.
    When a script runs "sleep 15"
    Then the terminal does not show "A message for you waits"
    And the terminal does not show "A file arrived in /sokar/files"
    When a script runs "rm -f /tmp/sokar-wake-spec.txt; sokar task remove sokar-wake-wake --force"
    And a script runs "sokar project unfollow wake --force"
    Then it exits zero
