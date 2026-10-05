# PI21 — A login inside a brokered Pi task fails

**Status:** later.

**What must be true.** A person in an attended, brokered Pi task cannot put a provider key into the
task with `/login`, so nothing in the task can use a provider past the broker.

## Why

Pi has no switch to turn `/login` off, and an entry in `~/.pi/agent/auth.json` wins over the provider
key the task is given. Egress blocks every subscription sign-in, because their auth hosts are not
declared. One path remains: a person, in an attended task, types `/login openrouter` and approves it in
a browser; Pi then stores a non-expiring OpenRouter key in the task, which the agent can read and use
past the broker. The model cannot do this alone.

## Acceptance

- `auth.json` cannot be written in a brokered task.
- An attended acceptance scenario shows `/login` fails and no `auth.json` appears; with the file
  writable, the same scenario is seen to fail.

## To be checked

- Whether a root-owned, read-only `auth.json` does it; that needs image-layout work, because
  `~/.pi/agent` is the agent's.
