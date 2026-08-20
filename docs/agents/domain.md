# Domain Documentation

Teiba-X uses a single repository-wide domain context:

- The ubiquitous-language glossary is maintained in the root `CONTEXT.md`.
- Architectural decisions that meet the ADR threshold belong in `docs/adr/`.
- Create `docs/adr/` lazily when the first qualifying decision is recorded.

Do not create a `CONTEXT-MAP.md` unless the repository later establishes genuinely independent bounded contexts that require separate glossaries.
