# Changelog

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