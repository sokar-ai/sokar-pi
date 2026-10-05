# sokar-pi

Runs [Pi](https://github.com/earendil-works/pi), the coding agent published as
`@earendil-works/pi-coding-agent` (not Oh My Pi), inside [Sokar](https://github.com/sokar-ai/sokar):
in a hardened container, reaching only its provider, with your credential kept on the machine and
its work waiting for your review.

## Install

Once Sokar's package repository is set up, as Sokar's
[getting started](https://github.com/sokar-ai/sokar/blob/main/doc/getting-started.md) describes:

```
sudo apt install sokar-agent-pi      # or: sudo dnf install sokar-agent-pi
```

How to sign in, start a task, what the task reaches and what to read when it does not work:
[doc/index.md](doc/index.md).

## More

- [build.md](build.md) - building the package, and moving the pinned Pi version.
- [doc/decisions.md](doc/decisions.md) - why it works the way it does.

## Licence

GNU General Public License v3.0 or later. See [LICENSE](LICENSE).
