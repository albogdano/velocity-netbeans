# TODO

- [ ] P1: Macro argument validation (check macro calls against definitions for argument count mismatch)
- [ ] P1: If we know where macros are located can we allow going to the macro definition when user executes Ctrl + Click on the macro name?
- [ ] P2: Brace matching for HTML tags doesn't work

# DONE & WON'T FIX
- [x] P0: Local variable type inference from VTL literals and expressions (106 tests)
- [x] P1: Macro argument autocompletion with correct arguments and tab-stop navigation
- [x] P0: Add unit tests (95 tests covering completion engine, parser, braces matcher, completion item, dot expression parser, HTML embedding, type resolver)
- [x] P1: Macro name autocompletion (scan AST for `#macro` definitions, suggest them in completion)
- [x] P3: Better FontAndColors differentiation (distinct colors for directive, macro, boolean, separator, operator, number)
- [x] P3: String interpolation highlighting (variables/properties/methods inside double-quoted strings)
- [x] P4: Fix static macro registry thread safety (`ConcurrentSkipListSet` for `m_MacroNames` and `m_LibraryMacroNames`)
- [x] P4: Add JavaCC regeneration to build (add `javacc-maven-plugin` execution to regenerate parser from `.jjt`)
- [x] P4: (RISKY) Convert to NetBeans annotations (replace layer.xml registrations with `@ServiceProvider`, `@MIMEResolver.Registration`, etc.)