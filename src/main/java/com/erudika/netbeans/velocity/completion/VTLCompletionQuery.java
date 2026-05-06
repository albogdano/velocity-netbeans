/*
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.completion;

import com.erudika.netbeans.velocity.completion.VTLCompletionItem.Kind;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;

/**
 * Context-aware completion query for VTL. Determines what completions
 * to show based on whether the cursor is after #, $, inside an expression,
 * or in other contexts.
 */
public class VTLCompletionQuery extends AsyncCompletionQuery
{
    private int queryCaretOffset;
    private String filterText;
    private List<VTLCompletionItem> allItems;

    @Override
    protected void prepareQuery(final JTextComponent component)
    {
        queryCaretOffset = component.getCaretPosition();
    }

    @Override
    protected void query(final CompletionResultSet resultSet, final Document doc, final int caretOffset)
    {
        try
        {
            final String textBefore = doc.getText(0, caretOffset);
            final int lineStart = findLineStart(textBefore);
            final String line = textBefore.substring(lineStart);

            filterText = extractFilterText(line);
            allItems = determineCompletions(line, filterText);

            for (final VTLCompletionItem item : allItems)
            {
                resultSet.addItem(item);
            }

            resultSet.finish();
        }
        catch (final BadLocationException ex)
        {
            resultSet.finish();
        }
    }

    @Override
    protected boolean canFilter(final JTextComponent component)
    {
        if (component == null || filterText == null || allItems == null)
        {
            return false;
        }

        try
        {
            final int currentCaret = component.getCaretPosition();
            final Document doc = component.getDocument();
            final String textBefore = doc.getText(0, currentCaret);
            final int lineStart = findLineStart(textBefore);
            final String currentLine = textBefore.substring(lineStart);
            final String newFilter = extractFilterText(currentLine);

            if (!newFilter.startsWith(filterText))
            {
                return false;
            }

            filterText = newFilter;
            return true;
        }
        catch (final BadLocationException ex)
        {
            return false;
        }
    }

    @Override
    protected void filter(final CompletionResultSet resultSet)
    {
        for (final VTLCompletionItem item : allItems)
        {
            if (item.getSortText().toString().toLowerCase().startsWith(filterText.toLowerCase()))
            {
                resultSet.addItem(item);
            }
        }
        resultSet.finish();
    }

    private int findLineStart(final String textBefore)
    {
        final int lastNewline = textBefore.lastIndexOf('\n');
        return lastNewline >= 0 ? lastNewline + 1 : 0;
    }

    private String extractFilterText(final String line)
    {
        final StringBuilder sb = new StringBuilder();
        for (int i = line.length() - 1; i >= 0; i--)
        {
            final char ch = line.charAt(i);
            if (Character.isJavaIdentifierPart(ch) || ch == '#' || ch == '$')
            {
                sb.insert(0, ch);
            }
            else if (ch == '(' || ch == ')' || ch == '!' || ch == '=' || ch == '<' || ch == '>')
            {
                break;
            }
            else
            {
                break;
            }
        }
        return sb.toString();
    }

    private List<VTLCompletionItem> determineCompletions(final String line, final String filter)
    {
        final List<VTLCompletionItem> items = new ArrayList<>();

        if (line.endsWith("#") || line.endsWith("#{") || startsWithDirectivePrefix(filter))
        {
            addDirectives(items, filter);
        }
        else if (line.endsWith("$") || startsWithReferencePrefix(filter))
        {
            addReferences(items, filter);
        }
        else if (isInExpression(line))
        {
            addReferences(items, filter);
            addKeywords(items, filter);
            addOperators(items, filter);
            addBooleanLiterals(items, filter);
        }
        else if (isInForeach(line))
        {
            addKeywordIn(items, filter);
        }
        else
        {
            addDirectives(items, filter);
        }

        return items;
    }

    private boolean startsWithDirectivePrefix(final String filter)
    {
        return filter.startsWith("#") && filter.length() > 1;
    }

    private boolean startsWithReferencePrefix(final String filter)
    {
        return filter.startsWith("$") && filter.length() > 1;
    }

    private boolean isInExpression(final String line)
    {
        final String trimmed = line.trim();
        return trimmed.contains("=") || trimmed.contains("==") || trimmed.contains("!=") ||
               trimmed.contains("<") || trimmed.contains(">") || trimmed.contains("&&") ||
               trimmed.contains("||");
    }

    private boolean isInForeach(final String line)
    {
        return line.contains("#foreach") && !line.contains("#end");
    }

    private void addDirectives(final List<VTLCompletionItem> items, final String filter)
    {
        final boolean hasHash = filter.startsWith("#");
        final String search = hasHash ? filter.substring(1) : filter;
        final int priority = 10;

        if (match("if", search))
            items.add(new VTLCompletionItem("#if", "#if()\n\n#end", "Conditional directive", Kind.DIRECTIVE, priority));
        if (match("else", search))
            items.add(new VTLCompletionItem("#else", "Else branch", Kind.DIRECTIVE, priority + 1));
        if (match("elseif", search))
            items.add(new VTLCompletionItem("#elseif", "#elseif()\n", "Else if branch", Kind.DIRECTIVE, priority + 2));
        if (match("end", search))
            items.add(new VTLCompletionItem("#end", "End block", Kind.DIRECTIVE, priority + 3));
        if (match("foreach", search))
            items.add(new VTLCompletionItem("#foreach", "#foreach($item in $list)\n\n#end", "Loop directive", Kind.DIRECTIVE, priority + 4));
        if (match("set", search))
            items.add(new VTLCompletionItem("#set", "#set($var = value)", "Variable assignment", Kind.DIRECTIVE, priority + 5));
        if (match("macro", search))
            items.add(new VTLCompletionItem("#macro", "#macro(name $arg)\n\n#end", "Macro definition", Kind.DIRECTIVE, priority + 6));
        if (match("include", search))
            items.add(new VTLCompletionItem("#include", "#include(\"template.vm\")", "Include template", Kind.DIRECTIVE, priority + 7));
        if (match("parse", search))
            items.add(new VTLCompletionItem("#parse", "#parse(\"template.vm\")", "Parse and include template", Kind.DIRECTIVE, priority + 8));
        if (match("evaluate", search))
            items.add(new VTLCompletionItem("#evaluate", "#evaluate($expr)", "Evaluate expression", Kind.DIRECTIVE, priority + 9));
        if (match("define", search))
            items.add(new VTLCompletionItem("#define", "#define($var)\n\n#end", "Define block", Kind.DIRECTIVE, priority + 10));
        if (match("stop", search))
            items.add(new VTLCompletionItem("#stop", "Stop rendering", Kind.DIRECTIVE, priority + 11));
    }

    private void addReferences(final List<VTLCompletionItem> items, final String filter)
    {
        final boolean hasDollar = filter.startsWith("$");
        final String search = hasDollar ? filter.substring(1) : filter;

        if (match("velocityCount", search))
            items.add(new VTLCompletionItem("$velocityCount", "Loop counter (deprecated)", Kind.REFERENCE, 20));
        if (match("velocityHasNext", search))
            items.add(new VTLCompletionItem("$velocityHasNext", "Loop hasNext flag (deprecated)", Kind.REFERENCE, 21));
    }

    private void addKeywords(final List<VTLCompletionItem> items, final String filter)
    {
        if (match("in", filter))
            items.add(new VTLCompletionItem("in", "Foreach keyword", Kind.KEYWORD, 50));
        if (match("and", filter))
            items.add(new VTLCompletionItem("and", "Logical AND (alternative)", Kind.KEYWORD, 51));
        if (match("or", filter))
            items.add(new VTLCompletionItem("or", "Logical OR (alternative)", Kind.KEYWORD, 52));
        if (match("not", filter))
            items.add(new VTLCompletionItem("not", "Logical NOT (alternative)", Kind.KEYWORD, 53));
        if (match("eq", filter))
            items.add(new VTLCompletionItem("eq", "Equals (alternative)", Kind.KEYWORD, 54));
        if (match("ne", filter))
            items.add(new VTLCompletionItem("ne", "Not equals (alternative)", Kind.KEYWORD, 55));
        if (match("lt", filter))
            items.add(new VTLCompletionItem("lt", "Less than (alternative)", Kind.KEYWORD, 56));
        if (match("le", filter))
            items.add(new VTLCompletionItem("le", "Less or equal (alternative)", Kind.KEYWORD, 57));
        if (match("gt", filter))
            items.add(new VTLCompletionItem("gt", "Greater than (alternative)", Kind.KEYWORD, 58));
        if (match("ge", filter))
            items.add(new VTLCompletionItem("ge", "Greater or equal (alternative)", Kind.KEYWORD, 59));
    }

    private void addKeywordIn(final List<VTLCompletionItem> items, final String filter)
    {
        if (match("in", filter))
            items.add(new VTLCompletionItem("in", "Foreach keyword", Kind.KEYWORD, 5));
    }

    private void addOperators(final List<VTLCompletionItem> items, final String filter)
    {
        if (filter.isEmpty() || filter.startsWith("!"))
        {
            items.add(new VTLCompletionItem("!", "Logical NOT", Kind.OPERATOR, 40));
        }
        if (filter.isEmpty() || filter.startsWith("&&"))
        {
            items.add(new VTLCompletionItem("&&", "Logical AND", Kind.OPERATOR, 41));
        }
        if (filter.isEmpty() || filter.startsWith("||"))
        {
            items.add(new VTLCompletionItem("||", "Logical OR", Kind.OPERATOR, 42));
        }
        if (filter.isEmpty() || filter.startsWith("=="))
        {
            items.add(new VTLCompletionItem("==", "Equals", Kind.OPERATOR, 43));
        }
        if (filter.isEmpty() || filter.startsWith("!="))
        {
            items.add(new VTLCompletionItem("!=", "Not equals", Kind.OPERATOR, 44));
        }
    }

    private void addBooleanLiterals(final List<VTLCompletionItem> items, final String filter)
    {
        if (match("true", filter))
            items.add(new VTLCompletionItem("true", "Boolean true", Kind.KEYWORD, 60));
        if (match("false", filter))
            items.add(new VTLCompletionItem("false", "Boolean false", Kind.KEYWORD, 61));
    }

    private boolean match(final String item, final String filter)
    {
        return item.toLowerCase().startsWith(filter.toLowerCase());
    }
}
