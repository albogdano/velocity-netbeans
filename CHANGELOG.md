# Changelog

**For version 3.0.0**

- Many improvements and fixes, including unnecessary errors for valid VTL syntax.
- HTML embedding - allows us to mix HTML + VTL in the same file
- Advanced autocompletion for everything in the context and built-in directives
- Context scanning for Spring and Velocity API methods for adding data to VelocityContext
- Configurable library file and global variables inside Netbeans Options > Editor > Velocity Context
- New autocomplete icons
- Various fixes and improvements:
		-  Macro argument validation (check macro calls against definitions for argument count mismatch)
		-  Brace matching for HTML tags in mixed VTL+HTML content (`VTLBracesMatcher` handles HTML tags cross-boundary; `VTLHtmlBracesMatcher` registered for `text/x-velocity/text/html` MimePath)
		-  If we know where macros are located can we allow going to the macro definition when user executes Ctrl + Click on the macro name?
		-  Ctrl+Click "Go To Definition" for VTL macros (`VTLMacroHyperlinkProvider` implementing `HyperlinkProviderExt`)
		-  Local variable type inference from VTL literals and expressions (106 tests)
		-  Macro argument autocompletion with correct arguments and tab-stop navigation
		-  Add unit tests (95 tests covering completion engine, parser, braces matcher, completion item, dot expression parser, HTML embedding, type resolver)
		-  Macro name autocompletion (scan AST for `#macro` definitions, suggest them in completion)
		-  Better FontAndColors differentiation (distinct colors for directive, macro, boolean, separator, operator, number)
		-  String interpolation highlighting (variables/properties/methods inside double-quoted strings)
		-  Fix static macro registry thread safety (`ConcurrentSkipListSet` for `m_MacroNames` and `m_LibraryMacroNames`)
		-  fixed folding uses deprecated APIs

**For version 2.2.5**

- Bugfix: locking rule violation in VTLBracesMatcher

**For version 2.2.4**

- Upgraded to run under NetBeans 8.0

**For version 2.2.3**

- unparsed content (#[[ ]]#) is now recognized
- Upgraded to run under NetBeans 7.0

**For version 2.2.2**

- Upgraded to run under NetBeans 6.9

**For version 2.2.1**

- Bugfix: adding already existing code folds
- Bugfix: adding multi line comment at the end causes IDE to hang
- Bugfix: syntax color lost when editing references, single line comments and string literals
- Bugfix: brace match at the end of file causes illegal position exception

**For version 2.2.0**

- support for braces matching (#ForEach .. #end, #if .. #end, #elsif .. #end, #else .. #end and #macro .. #end ) added

**For version 2.1.0**

- supports code folding for #ForEach, #if, #elsif, #else and #macro directives
- syntax coloring for include directive added (but no processing of the included template)
- macro calls a now recognized
- does now correctly color escaped directive in text color instead of directive color
- foreach directive now supports object array and vector as argument after the 'in' keyword
- error stripe status indicator works now

**For version 2.0.0**

- completely revised version, no longer based on deprecated NBS (NetBeans Schlieman), instead it is now based on a javacc 5.0 defined grammar (*.jj) definition.