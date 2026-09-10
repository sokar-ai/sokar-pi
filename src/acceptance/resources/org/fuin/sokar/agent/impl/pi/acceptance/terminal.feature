Feature: What a person sees with this agent, before there is any credential

  Every scenario here needs no secret and no model. They are the things somebody meets in the
  first ten minutes with this agent on a machine, and each of them is a refusal that has to say
  what it is rather than fail later.

  Scenario: an agent that does not say how to log in is answered, not guessed at
    # Sokar does not know how any agent logs in; the verb comes from the agent's own manifest,
    # and this one declares none. Hardcoding one agent's verb would be wrong for every other.
    When logging in to the "pi" agent is attempted
    Then it exits non-zero

  Scenario: starting a task without a credential says what is missing, before anything is built
    Given a project called "nocred" of class "guarded" with a file in it
    And the vault is unlocked with the passphrase "sokar-acceptance-passphrase"
    And the vault holds no credential for "openrouter"
    When a task nobody is watching is started in "nocred" for the "pi" agent
    Then its output mentions one of "no credential, is locked, cannot authenticate"
    And its output contains "openrouter"

  Scenario: a script is never asked a question by this agent's task
    Given a project called "noask" of class "guarded" with a file in it
    When a script asks what a task in "noask" for the "pi" agent would do
    Then its output does not contain "[Y/n]"
    And its output contains no escape sequences
