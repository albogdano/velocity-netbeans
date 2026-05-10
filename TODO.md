# TODO

- [ ] P1: When a Velocity macro library is configured in the Netbeans options menu, some macros are not highlighted properly and not clickable (i.e. don't have hyper links) - we need to rescan the current file again

# DONE

- [x] P1: folding uses deprecated APIs
- [x] P1: WARNING: Ineffective registration of resolver Services/MIMEResolver/VTLResolver.xml use @MIMEResolver.Registration! See bug #191777.
- [x] P3: Macro argument validation (check macro calls against definitions for argument count mismatch)
- [x] P2: Brace matching for HTML tags in mixed VTL+HTML content (`VTLBracesMatcher` handles HTML tags cross-boundary; `VTLHtmlBracesMatcher` registered for `text/x-velocity/text/html` MimePath)
- [x] P1: If we know where macros are located can we allow going to the macro definition when user executes Ctrl + Click on the macro name?
- [x] P1: Ctrl+Click "Go To Definition" for VTL macros (`VTLMacroHyperlinkProvider` implementing `HyperlinkProviderExt`)
- [x] P0: Local variable type inference from VTL literals and expressions (106 tests)
- [x] P1: Macro argument autocompletion with correct arguments and tab-stop navigation
- [x] P0: Add unit tests (95 tests covering completion engine, parser, braces matcher, completion item, dot expression parser, HTML embedding, type resolver)
- [x] P1: Macro name autocompletion (scan AST for `#macro` definitions, suggest them in completion)
- [x] P3: Better FontAndColors differentiation (distinct colors for directive, macro, boolean, separator, operator, number)
- [x] P3: String interpolation highlighting (variables/properties/methods inside double-quoted strings)
- [x] P4: Fix static macro registry thread safety (`ConcurrentSkipListSet` for `m_MacroNames` and `m_LibraryMacroNames`)

# WON'T FIX
- [-] P4: Add JavaCC regeneration to build (add `javacc-maven-plugin` execution to regenerate parser from `.jjt`)
- [-] P4: (RISKY) Convert to NetBeans annotations (replace layer.xml registrations with `@ServiceProvider`, `@MIMEResolver.Registration`, etc.)