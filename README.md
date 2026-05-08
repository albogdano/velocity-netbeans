<h1 align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/c/cb/Apache_Velocity_logo.svg">
  <br>Velocity Plugin for Apache NetBeans</br>
</h1>

> This project is a fork of the original [Velocity Editor](https://sourceforge.net/projects/velocity-editor/) plugin by Werner Jäger (T-Systems International GmbH), updated and extended with modern features for current NetBeans releases.

<p align="center">
  <a href="#features">Features</a> •
  <a href="#requirements">Requirements</a> •
  <a href="#how-to-build">How To Build</a> •
  <a href="#how-to-use">How To Use</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#known-issues">Known Issues</a>
</p>

<p align="center">
This is an <a href="https://netbeans.apache.org/">Apache NetBeans</a> plugin that provides editor support for <a href="https://velocity.apache.org/">Apache Velocity Template Language</a> (VTL) files (<code>.vm</code> and <code>.vsl</code> extensions).
</p>

### Features

- **Syntax highlighting** — 67 token types across 10 categories (keyword, directive, comment, string, operator, number, identifier, boolean, separator, unparsed content)
- **Error highlighting** — real-time syntax error detection with error stripe annotations
- **Code folding** — collapsible regions for `#foreach`, `#if`, `#elseif`, `#else`, `#macro` blocks
- **Braces matching** — matches directive pairs (`#if`/`#end`, `#foreach`/`#end`, `#macro`/`#end`)
- **HTML embedding** — full HTML support (highlighting, completion, validation) for HTML content between VTL directives
- **Autocompletion** — context-aware completions for:
  - Directives (`#if`, `#foreach`, `#set`, `#macro`, `#include`, `#parse`, `#define`, `#evaluate`, `#stop`, `#break`)
  - References (`$var` variables declared in the template via `#set`, `#foreach`, `#macro`)
  - Built-in references (`$foreach.count`, `$foreach.index`, `$foreach.first`, `$foreach.last`, `$foreach.hasNext`)
  - Keywords (`in`, `and`, `or`, `not`, `eq`, `ne`, `lt`, `le`, `gt`, `ge`)
  - Operators (`!`, `&&`, `||`, `==`, `!=`, `<`, `<=`, `>`, `>=`)
  - Boolean literals (`true`, `false`)
- **Macro library support** — reads macro definitions from configurable library files (e.g., `VM_global_library.vm`) and suggests `#macroName()` completions
- **Java context scanning** — automatically detects Velocity context variables from Java source code:
  - `context.put("key", value)` — Velocity API
  - `model.addAttribute("key", value)` — Spring MVC Model
- **Method/property completion** — typing `$var.` shows available methods and Velocity property shortcuts (e.g., `getName()` → `name`) with full type resolution and multi-level chaining support
- **Configurable type mappings** — define variable types in the options panel for projects where automatic scanning isn't available
- **File templates** — New file templates for Velocity Template (.vsl) and Velocity Macro (.vm)

### Requirements

- **Apache NetBeans 22+** (built against RELEASE280)
- **Java 21+**

### How to Build

```bash
# Build the module (produces .nbm file)
mvn clean install

# The installable NBM file will be at:
# target/nbm/velocity-netbeans-1.0.0-SNAPSHOT.nbm
```

### How to Use

#### Installation

1. Build the NBM file: `mvn clean install`
2. In NetBeans, go to **Tools > Plugins > Downloaded**
3. Click **Add Plugins...** and select `target/nbm/velocity-netbeans-1.0.0-SNAPSHOT.nbm`
4. Click **Install** and restart NetBeans

#### Basic Editing

Open any `.vm` or `.vsl` file. You get syntax highlighting, error detection, code folding, and braces matching out of the box.

#### Autocompletion

- Type `#` to get directive completions
- Type `$` to get reference/variable completions
- Type `.` after a typed variable (e.g., `$user.`) to get method and property completions

#### Method Completion (Dot-Completion)

For method/property suggestions to work, the plugin needs to know the Java type of each variable. This is resolved from:

1. **Automatic Java scanning** — The plugin scans your project's Java files for `context.put("key", value)` and `model.addAttribute("key", value)` calls, extracting variable names and their types.
2. **Manual configuration** — Define type mappings in **Options > Editor > Velocity Context** using the format `$varName:com.example.Type`.

Once types are known, typing `$user.` will show:
- **Properties** — Velocity-style shortcuts derived from getters (e.g., `name` from `getName()`)
- **Methods** — Full Java method signatures (e.g., `getName()`, `setName(String)`)

Multi-level chaining is supported: `$user.getAddress().getCity().` resolves each return type in the chain.

### Configuration

Go to **Options > Editor > Velocity Context** to configure:

- **Macro Library Files** — comma-separated list of macro library filenames (default: `VM_global_library.vm`). The plugin searches upward from the current file's directory.
- **Context Variables** — one per line, in the format:
  - `$varName` — declares a variable (no type info, basic completion only)
  - `$varName:com.example.Type` — declares a variable with type (enables method/property completion)

#### Supported Context Detection

The Java scanner automatically detects variables from these patterns:

```java
// Velocity API
VelocityContext context = new VelocityContext();
context.put("user", userObj);         // → $user typed as User

// Spring MVC
model.addAttribute("title", "Hello"); // → $title typed as String
model.addAttribute("items", list);    // → $items typed as List
```

Supported receiver types (and subclasses/implementations):
- `org.apache.velocity.VelocityContext`
- `org.apache.velocity.context.Context`
- `org.springframework.ui.Model`
- `org.springframework.ui.ModelMap`
- `org.springframework.web.servlet.ModelAndView`

### Known Issues

- Macro argument count validation is not yet implemented (macro calls are not checked against their definitions)
- Escaped variables/properties (`\$var`) may still show identifier coloring instead of text coloring
- The first time a `.vm` file is opened in a project, Java context scanning runs asynchronously — completions from Java appear on the second invocation

### License

[CDDL 1.0](https://opensource.org/license/CDDL-1.0)
