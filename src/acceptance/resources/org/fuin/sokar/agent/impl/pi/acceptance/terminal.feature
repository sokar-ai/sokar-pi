Feature: What a person sees with this agent, before there is any credential

  Every scenario here needs no secret and no model. They are the things somebody meets in the
  first ten minutes with this agent on a machine, and each of them is a refusal that has to say
  what it is rather than fail later.

  Scenario: an agent that does not say how to log in is answered, not guessed at
    # Sokar does not know how any agent logs in; the verb comes from the agent's own manifest,
    # and this one declares none. Hardcoding one agent's verb would be wrong for every other.
    When a script runs "sokar vault login pi --dry-run"
    Then it exits non-zero

  # The removal states the premise instead of assuming it. credential.feature stores an
  # 'openrouter' credential on this machine, so whether a task can authenticate here depends
  # on what another feature did earlier - this scenario passed for the agent whose provider is
  # anthropic and failed for the two whose provider is openrouter, which was luck either way.
  # Removing is idempotent: 'nothing named x was in the vault' is a success.
  #
  # A prompt, because that is what makes the run unattended, and only an unattended run is
  # refused for a missing credential: nobody is watching one, so a warning would go into an
  # empty room. An attached shell deliberately needs no credential - working inside the
  # container by hand is what it is for - and --dry-run stops before the check entirely,
  # which is what this scenario used to ask and why it never saw the message.
  Scenario: starting a task without a credential says what is missing, before anything is built
    Given a project called "nocred" of class "guarded" with a file in it
    And the vault is unlocked with the passphrase "sokar-acceptance-passphrase"
    And a script runs "sokar vault remove openrouter"
    When a script runs "cd ~/nocred && sokar task run --agent pi --prompt hello"
    Then its output mentions one of "no credential, is locked, cannot authenticate"
    And its output contains "openrouter"

  Scenario: a script is never asked a question by this agent's task
    Given a project called "noask" of class "guarded" with a file in it
    When a script runs "cd ~/noask && sokar task run --agent pi --dry-run --no-attach"
    Then its output does not contain "[Y/n]"
    And its output contains no escape sequences
