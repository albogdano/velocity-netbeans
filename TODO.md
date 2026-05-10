# TODO

- [x] P0: Add unit tests (95 tests covering completion engine, parser, braces matcher, completion item, dot expression parser, HTML embedding, type resolver)
- [ ] P1: Macro name autocompletion (scan AST for `#macro` definitions, suggest them in completion)
- [ ] P3: Macro argument validation (check macro calls against definitions for argument count mismatch)
- [x] P3: Better FontAndColors differentiation (distinct colors for directive, macro, boolean, separator, operator, number)
- [ ] P3: String interpolation highlighting (variables/properties/methods inside double-quoted strings)
- [x] P4: Fix static macro registry thread safety (`ConcurrentSkipListSet` for `m_MacroNames` and `m_LibraryMacroNames`)
- [ ] P4: Add JavaCC regeneration to build (add `javacc-maven-plugin` execution to regenerate parser from `.jjt`)
- [ ] P4: Convert to NetBeans annotations (replace layer.xml registrations with `@ServiceProvider`, `@MIMEResolver.Registration`, etc.)