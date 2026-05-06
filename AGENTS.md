# AGENTS.md — velocity-netbeans

## Project Overview

**velocity-netbeans** is an Apache NetBeans module plugin that provides editor support for Apache Velocity Template Language (VTL) files (`.vm` and `.vsl` extensions). It is built with Maven using the `nbm-maven-plugin`, targets NetBeans RELEASE280, and requires Java 21.

**Maintainer:** com.erudika
**Original Author:** Werner Jaeger (T-Systems International GmbH)
**License:** CDDL 1.0
**Module Version:** 2.2.5
**Maven Version:** 1.0.0-SNAPSHOT

---

## Architecture

```
src/main/java/com/erudika/netbeans/velocity/
├── VTLDataObject.java                    # Data object for .vm/.vsl files
├── completion/
│   ├── VTLCompletionProvider.java        # Completion SPI entry point
│   ├── VTLCompletionQuery.java           # Context-aware completion logic
│   └── VTLCompletionItem.java            # Completion item rendering
├── embedding/
│   └── HTMLEmbeddingProvider.java        # Embeds HTML language in TEXT tokens
├── editor/
│   ├── VTLEditorKit.java                 # Editor kit (NbEditorKit subclass)
│   ├── bracesmatching/
│   │   └── VTLBracesMatcher.java         # Matches #if/#end, #foreach/#end, #macro/#end
│   └── fold/
│       ├── VTLCodeFoldingSideBarFactory.java
│       ├── VTLFoldAnalyser.java           # AST visitor for foldable regions
│       ├── VTLFoldInfo.java               # Fold metadata
│       ├── VTLFoldManager.java            # Fold hierarchy management
│       └── VTLFoldManagerFactory.java
├── jcclexer/                              # JavaCC 5.0 generated parser + AST
│   ├── VelocityParser.jjt                 # JJTree grammar source (KEY FILE)
│   ├── VelocityParser.java                # Generated parser
│   ├── VelocityParserTokenManager.java    # Generated lexer
│   ├── VelocityParserConstants.java       # Token constants
│   └── node/                              # 45 AST node classes (JJTree-generated)
│       ├── SimpleNode.java                # Base AST node
│       ├── VelocityAnalyser.java           # Abstract visitor base class
│       └── [AST*.java]                    # One class per AST node type
├── lexer/                                 # NetBeans Lexer SPI integration
│   ├── VTLLanguageHierarchy.java           # 67 token types + categories + HTML embedding
│   ├── VTLLexer.java                       # Lexer adapter
│   ├── VTLTokenId.java                     # TokenId wrapper
│   └── VelocityCharStream.java             # CharStream adapter for JavaCC
└── parser/                                # NetBeans Parsing SPI integration
    ├── VTLParser.java                      # Parser wrapper
    ├── VTLParserFactory.java
    ├── VTLParserResult.java
    ├── VTLSyntaxErrorsHighlightingTask.java
    ├── VTLSyntaxErrorsHighlightingTaskFactory.java
    ├── VTLUpToDateStatusProvider.java
    └── VTLUpToDateStatusProviderFactory.java

src/main/resources/com/erudika/netbeans/velocity/
├── layer.xml                               # NetBeans layer (registry)
├── Bundle.properties                       # i18n strings
├── FontAndColors.xml                       # Syntax coloring config
├── VTLResolver.xml                         # MIME resolver (.vm, .vsl → text/x-velocity)
├── VelocityExample.vm                      # Example template
├── VTLTemplate.vsl                         # New file template
└── VelocityFiles16.png                     # File type icon
```

---

## Current Features

1. **Syntax coloring** — 67 token types across 10 categories (keyword, directive, comment, string, operator, number, identifier, boolean, separator, unparsedcontent)
2. **Syntax error highlighting** — Parser collects errors, scheduler task highlights them in editor
3. **Code folding** — Folds for `#foreach`, `#if`, `#elseif`, `#else`, `#macro` blocks
4. **Braces matching** — Matches directive pairs (#if/#end, #foreach/#end, #macro/#end, #elseif/#end, #else/#end)
5. **Error stripe** — Annotations show in the editor sidebar
6. **MIME resolution** — `.vm` and `.vsl` files recognized as `text/x-velocity`
7. **File templates** — New VTL template available in File > New
8. **HTML mixing** — HTML content between VTL directives receives syntax highlighting, code completion, and validation via embedded `text/html` language
9. **Autocompletion** — Context-aware completions for directives (`#`), references (`$`), keywords, operators, and boolean literals

---

## Supported Directives

| Directive | Status |
|-----------|--------|
| `#set` | Supported |
| `#if` / `#elseif` / `#else` / `#end` | Supported |
| `#foreach` / `#end` | Supported |
| `#macro` / `#end` | Supported |
| `#include` | Supported |
| `#stop` | Supported |
| `#parse` | Recognized (LINE), no coloring |
| `#evaluate` | Recognized (LINE), no coloring |
| `#define` | Recognized (BLOCK), no coloring |

---

## Key Findings

### Strengths

- **Solid foundation**: The JavaCC-based grammar is based on Velocity 1.6.2 spec and handles most VTL constructs
- **Good NetBeans SPI integration**: Properly uses Lexer SPI, Parsing SPI, Editor Fold SPI, Braces Matching SPI, and Error Stripe API
- **AST visitor pattern**: The `VelocityAnalyser` abstract class makes it easy to add new AST-traversing features
- **Parser reusability**: The `VelocityParser` is designed to be reused across parse invocations
- **Error recovery**: The parser has error recovery logic that collects multiple syntax errors rather than stopping at the first one
- **Clean layer.xml**: Well-organized NetBeans layer registration

### Issues & Limitations

#### Existing Known Issues (from README.md)
1. **No macro argument validation** — Macro calls are not checked against their definitions for argument count mismatch
2. **Missing directive coloring** — `#parse`, `#evaluate`, `#define` are recognized but not color-coded (they use generic directive token)
3. **Escaped directive coloring** — Escaped variables/properties/methods should display as text color, not identifier color
4. **String interpolation** — Variables/properties/methods inside double-quoted strings are not recognized or highlighted

#### Code Quality Issues
5. **Hungarian notation** — Code uses Hungarian notation (e.g., `m_Operation`, `strDesc`, `fBackward`) which is non-standard for Java
6. **Raw types** — Several places use raw types instead of generics (e.g., `Map<VTLFoldInfo, Fold>` without proper generic imports in older code)
7. **No unit tests** — There are no test files (`src/test/` directory does not exist). This is critical before adding new features
8. **JavaCC grammar not regenerated** — The `.jjt` file exists but there is no Maven plugin or build step to regenerate the parser from it. The `.java` files in `jcclexer/` are pre-generated and checked in
9. **Static macro registry** — `VelocityParser.m_MacroNames` is static, which means macro definitions persist across different file parses. This could cause false positives when macros defined in one file appear as "recognized" in another file
10. **Hardcoded font/coloring** — Most token categories map to `keyword` in FontAndColors.xml, reducing visual differentiation
11. **No NetBeans API annotations** — Missing `@ServiceProvider`, `@MIMEResolver`, and other declarative annotations that modern NetBeans modules use
12. **`#end` folding description** — The fold description concatenates the first 3 tokens (`firstToken.next.next`) which could produce unreadable descriptions for complex directives
13. **Thread safety concerns** — The `ANALYSERS` static map in `VTLParser` is a `HashMap` (not thread-safe) and could cause issues with concurrent parsing

---

## Autocompletion (COMPLETED)

Autocompletion is implemented using the NetBeans Completion SPI:

### How It Works

The completion system uses three classes:

1. **VTLCompletionProvider** — Registers for `text/x-velocity` MIME type, creates completion tasks, and defines auto-trigger characters (`#` and `$`)
2. **VTLCompletionQuery** — Analyzes cursor context to determine what completions to show:
   - After `#` → Directives (#if, #foreach, #set, etc.)
   - After `$` → References ($velocityCount, $velocityHasNext)
   - Inside expressions → Keywords, operators, boolean literals
   - Inside `#foreach` → `in` keyword
3. **VTLCompletionItem** — Renders individual completion items with type indicators (d=directive, r=reference, k=keyword, o=operator)

### Completion Categories

| Category | Trigger | Completions |
|----------|---------|-------------|
| Directives | `#` | `#if`, `#else`, `#elseif`, `#end`, `#foreach`, `#macro`, `#set`, `#include`, `#parse`, `#evaluate`, `#define`, `#stop` |
| References | `$` | `$velocityCount`, `$velocityHasNext` |
| Keywords | In expressions | `in`, `and`, `or`, `not`, `eq`, `ne`, `lt`, `le`, `gt`, `ge` |
| Operators | In expressions | `!`, `&&`, `||`, `==`, `!=` |
| Booleans | In expressions | `true`, `false` |

### Snippet Support
Directives include useful code snippets:
- `#if` → `#if()\n\n#end`
- `#foreach` → `#foreach($item in $list)\n\n#end`
- `#set` → `#set($var = value)`
- `#macro` → `#macro(name $arg)\n\n#end`

### Key Files
- `completion/VTLCompletionProvider.java` — Entry point, registered in layer.xml
- `completion/VTLCompletionQuery.java` — Context analysis and completion logic
- `completion/VTLCompletionItem.java` — Item rendering and insertion behavior
- `layer.xml` — Registered under `Editors/text/x-velocity/Completion`

### Known Limitations
- Does not yet suggest macro names defined in the current file
- Does not suggest context variables passed from the application
- Does not suggest methods/properties on referenced objects

---

## ~~Suggestions for Adding Autocompletion~~ (See above - implemented)

~~To add autocompletion to this module, you'll need to implement the **NetBeans Completion SPI**. Here's the recommended approach:~~

~~### 1. Add Completion Dependencies to `pom.xml`~~
~~```xml~~
~~<dependency>~~
~~    <groupId>org.netbeans.api</groupId>~~
~~    <artifactId>org-netbeans-modules-editor-completion</artifactId>~~
~~    <version>${nbVer}</version>~~
~~</dependency>~~
~~```~~

~~### 2. Create Completion Classes~~
~~Create a new package: `com.erudika.netbeans.velocity.completion`~~

~~- **VTLCompletionProvider.java** — Implements `CompletionProvider`. Registers for `text/x-velocity` MIME type.~~
~~- **VTLCompletionItem.java** — Implements `CompletionItem`. Represents individual completion entries.~~
~~- **VTLCompletionQuery.java** — The logic that determines what completions to show based on cursor context.~~

~~### 3. Register in `layer.xml`~~
~~```xml~~
~~<folder name="Editors">~~
~~   <folder name="text">~~
~~      <folder name="x-velocity">~~
~~         <folder name="Completion">~~
~~            <file name="com-erudika-netbeans-velocity-completion-VTLCompletionProvider.instance"/>~~
~~         </folder>~~
~~      </folder>~~
~~   </folder>~~
~~</folder>~~
~~```~~

~~### 4. Suggested Completion Categories~~

~~| Category | Trigger | Completions |~~
~~|----------|---------|-------------|~~
~~| Directives | `#` | `#set`, `#if`, `#foreach`, `#macro`, `#include`, `#parse`, `#evaluate`, `#define`, `#stop` |~~
~~| References | `$` | Context variables (requires context analysis), `$velocityCount`, `$velocityHasNext` |~~
~~| Operators | Inside expressions | `==`, `!=`, `<`, `<=`, `>`, `>=`, `&&`, `\|\|`, `!`, `and`, `or`, `not`, `eq`, `ne`, `lt`, `le`, `gt`, `ge` |~~
~~| Macro calls | `#` (known macros) | Scan file for `#macro` definitions, suggest matching calls |~~
~~| Keywords | Inside `#foreach` | `in` |~~
~~| Built-in objects | `$` | `$request`, `$response`, `$session`, `$cookie`, `$context` (configurable) |~~

~~### 5. Implementation Strategy~~
~~- Parse the AST to extract macro definitions for macro-name completion~~
~~- Use the existing lexer to determine current token context (are we after `#`, `$`, inside an expression?)~~
~~- Consider using `VTLParserResult` to get the AST during completion queries~~

---

## HTML + Velocity Mixing (COMPLETED)

HTML mixing is implemented using a two-layer embedding approach:

### How It Works

1. **Lexer-level embedding** (`VTLLanguageHierarchy.embedding()`): When the VTL lexer encounters a `TEXT` token (content between VTL directives), it delegates lexing to the HTML language. This gives you:
   - HTML syntax highlighting (tags, attributes, text)
   - HTML tag auto-completion
   - HTML structure validation

2. **Parsing-level embedding** (`HTMLEmbeddingProvider`): Creates `Embedding` objects for TEXT tokens that contain HTML-like content (`<`, `>`, `&`, quotes). This enables:
   - HTML code completion via NetBeans' built-in HTML completion
   - HTML parsing and semantic analysis
   - Works alongside VTL parsing infrastructure

### Architecture

```
VTL File (.vm/.vsl)
├── #if($user.loggedIn)          → VTL directive (VTL lexer)
├── <nav>                        → TEXT token → embedded HTML lexer
│   ├── <a href="/home">Home</a> → HTML tokens from embedded lexer
│   └── $user.name               → Inside HTML text, VTL not re-tokenized
├── #end                         → VTL directive (VTL lexer)
└── <footer>...</footer>         → TEXT token → embedded HTML lexer
```

### Key Files
- `lexer/VTLLanguageHierarchy.java` — `embedding()` method delegates TEXT tokens to `text/html`
- `embedding/HTMLEmbeddingProvider.java` — Parsing API embedding with HTML content heuristic
- `pom.xml` — Added `org-netbeans-modules-html-lexer` dependency

### Limitations
- VTL constructs inside HTML attributes (e.g., `<div class="$css">`) are treated as HTML text
- The HTML content heuristic (`containsHtmlContent()`) only triggers on HTML-like characters (`<`, `>`, `&`, `"`, `'`)

---

## ~~Suggestions for HTML + Velocity Mixing~~ (See above - implemented)

~~Currently, `.vm` files are treated as pure `text/x-velocity`. To support HTML mixing:~~

~~### Option A: Embedded Language (Recommended)~~

~~Use NetBeans' **embedded language** support to treat HTML portions as embedded `text/html` within `text/x-velocity`.~~

~~1. **Create an HTML embedding provider** that detects HTML content between VTL directives~~
~~2. **Lexer enhancement** — Add HTML token recognition in the DEFAULT lexical state~~
~~3. **Use GSF (Generic Scripting Framework)** or the newer embedded language APIs~~

~~### Option B: Multi-Language Lexer (Simpler)~~

~~Modify the lexer to recognize HTML tokens as a separate category and use a secondary HTML lexer for HTML portions.~~

~~#### Steps:~~
~~1. **Add HTML token category** to `VTLLanguageHierarchy.java`:~~
~~   - `HTML_TAG`, `HTML_ATTRIBUTE`, `HTML_TEXT`, etc.~~

~~2. **Modify `VelocityParser.jjt`** — In the DEFAULT state, add HTML-aware tokenization:~~
~~   - Recognize `<tag>`, `</tag>`, `<tag attr="value">`, HTML comments~~
~~   - Keep the existing TEXT token as fallback~~

~~3. **Add HTML autocompletion** — Register an HTML completion provider that activates when cursor is in HTML context~~

~~4. **Update `FontAndColors.xml`** — Add HTML category with appropriate coloring~~

~~5. **Consider using `@LanguageRegistration`** — Modern NetBeans supports declarative language registration with embedded languages~~

~~### Option C: MIME Type Association (Simplest)~~

~~Associate `.vm` files with HTML as the base language and overlay VTL highlighting:~~
~~1. Change the base editor kit to HTML editor kit~~
~~2. Register VTL as a secondary language/overlay~~
~~3. This gives you HTML features (completion, validation) "for free"~~

---

## Priority Feature Roadmap

| Priority | Feature | Effort | Impact | Status |
|----------|---------|--------|--------|--------|
| P0 | Add unit tests | High | Critical foundation | Pending |
| P1 | ~~Directive autocompletion~~ | ~~Medium~~ | ~~High user value~~ | **DONE** |
| P1 | ~~Reference autocompletion~~ | ~~Medium-High~~ | ~~High user value~~ | **DONE** |
| P1 | Macro name autocompletion | Medium | High user value | Pending |
| P2 | ~~HTML mixing (embedded)~~ | ~~High~~ | ~~Critical for real-world use~~ | **DONE** |
| P2 | ~~HTML autocompletion in mixed files~~ | ~~Medium~~ | ~~High user value~~ | **DONE** |
| P3 | Macro argument validation | Medium | Improves code quality | Pending |
| P3 | Better FontAndColors differentiation | Low | Improves readability | Pending |
| P3 | String interpolation highlighting | Medium | Addresses known issue | Pending |
| P4 | Fix static macro registry (cross-file pollution) | Low | Bug fix | Pending |
| P4 | Add JavaCC regeneration to build | Low | Improves maintainability | Pending |
| P4 | Convert to NetBeans annotations | Low | Modernization | Pending |

---

## Build Commands

```bash
# Build the module
mvn clean install

# Run in NetBeans IDE
mvn -Dnetbeans.run.ide=<path-to-netbeans> nbm:run-ide

# Build NBM installer
mvn nbm:cluster
```

## Key Files to Modify

| Feature | Files to Modify |
|---------|-----------------|
| ~~Autocompletion~~ | ~~NEW: `completion/*.java`, `layer.xml`, `pom.xml`~~ |
| Macro completion | `completion/VTLCompletionQuery.java` — scan AST for `#macro` definitions |
| ~~HTML Mixing~~ | ~~`VTLLanguageHierarchy.java`, `VTLLexer.java`, `VelocityParser.jjt`, `FontAndColors.xml`~~ |
| Macro validation | `VTLParser.java`, `VelocityParser.jjt`, NEW: validation task |
| Font/colors | `FontAndColors.xml`, `Bundle.properties` |
| Tests | NEW: `src/test/` |
