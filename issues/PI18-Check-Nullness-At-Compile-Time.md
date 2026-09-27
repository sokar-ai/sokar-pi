# PI18 — Check nullness at compile time with NullAway

**Priority:** 2
**Opened:** 2026-09-27
**Source:** the operator's instruction, after the skills review found a nullness contract nothing checked

## What

The package is `@NullMarked` (JSpecify), so a plain type means non-null - but **nothing enforces
it**. Measured 2026-09-27: neither this `pom.xml` nor the effective pom with the parent
`org.fuin:pom:2.0.2` configures Error Prone or NullAway. The annotations are promises no build checks.

The package was only made `@NullMarked` on 2026-09-27, by the skills review, so no annotation here
has ever been checked.


NullAway runs as an Error Prone check inside `maven-compiler-plugin`, scoped to the annotated
package, and turns a broken nullness contract into a compile error. The installed skill
`jspecify-nullaway-migration` is the playbook: the order of steps, and the dependency-analysis and
annotation-processor fallout such a migration causes.

## What would close it

- Error Prone with NullAway runs in the compile of `src/main`, restricted to
  `org.fuin.sokar.agent.impl.pi`, and a violation fails the build.
- **Proven against the failure it exists for:** a method returning `null` from a non-`@Nullable`
  type is written once, the build goes red naming it, and it is taken out again.
- Every report it makes on the current code is fixed, or annotated with the reason where the code is
  right and the checker cannot see it.
- The native build and its checks still pass - the compiler plugin also feeds native-image.

## Open question

**Where the configuration lives:** this pom, or the shared parent `org.fuin:pom`, which would make
it true in every repository at once but is not this repository's to change. If the parent, this
issue is blocked on that change and names it.
