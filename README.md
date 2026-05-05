# VTL (Velocity Template Language) plugin for Apache NetBeans

Provides basic support to Velocity's `*.vm` and `*.vsl` files. 
Syntax coloring, basic error highlighting and braces matching is achieved with a lexer and parser based on 
Apache Velocity's 1.6.2 specification and compiled with JavaCC 5.0.

Supports syntax- error highlighting, code folding and braces matching.


Known issues:

- It should be checked, that the number of arguments in a macro call is the same as in the corresponding macro definition.
- No syntax coloring for directives #parse, #evaluate, #define
- Escaped variables, properties and methods should be in text color instead of identity color
- Variable, property or method in double quotes are not recognized.
- Feedback would be very much appreciated.
