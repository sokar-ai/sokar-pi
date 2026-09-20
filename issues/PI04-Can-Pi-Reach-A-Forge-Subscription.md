# PI04 — Can Pi reach a forge subscription, and does brokering hold beyond one dialect

**Priority:** 3
**Opened:** 2026-09-12
**Source:** handed over from Sokar requirement **A04**, 2026-09-12. Everything else in that
requirement is built and met; these are the questions it left open.
**Depends on:** Sokar requirement **A01** (Pi Forge Subscription), which stayed in that repository
because what remains of it is the vault, the host-side browser sign-in and the short-lived token.
This issue is the half that can only be answered here.

## What is open

- **Does Pi reach GitHub Copilot at all?** A01 assumes it does, and nobody checked. Oh My Pi
  advertises Copilot explicitly; this agent does not. If the answer is no, A01's premise moves to
  another agent and this repository is not the vehicle for it.
- **Does redirection hold for a provider that does not speak the common dialect?** Verified for
  OpenRouter, which is the whole of what is proven. A forge subscription may authenticate
  differently, and the routing extension sets a base URL - which only helps a provider that can be
  addressed by one.
- **Do its sign-in flows refresh on their own?** They do, which is what makes them awkward: a token
  that refreshes inside the container is a credential the container holds. That is Sokar
  requirement **B01**, and this repository's part is only to say what Pi does.

## What would close it

A measurement against a real subscription, not a reading of documentation - the same standard the
OpenRouter answer was held to. Either it routes through the broker, or it does not and this file
says which providers of Pi's list can be brokered at all.

## Why it is priority 3

Nothing is blocked on it. The agent is built, shipped and verified against a provider that works,
and the question is about extending it rather than about it being right. It moves up the moment
somebody wants a subscription-backed agent.
