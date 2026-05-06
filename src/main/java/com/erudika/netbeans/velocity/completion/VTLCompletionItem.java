/*
 * Copyright (c) 2009 T-Systems International GmbH.
 * All rights reserved.
 * This software is the confidential and proprietary information
 * of T-Systems International GmbH.
 *
 */
package com.erudika.netbeans.velocity.completion;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionTask;

/**
 * Completion item for VTL (Velocity Template Language) directives, references,
 * keywords, and operators.
 */
public class VTLCompletionItem implements CompletionItem
{
    public enum Kind
    {
        DIRECTIVE("d"),
        REFERENCE("r"),
        KEYWORD("k"),
        OPERATOR("o"),
        MACRO("m");

        private final String abbreviation;

        Kind(final String abbreviation)
        {
            this.abbreviation = abbreviation;
        }

        public String getAbbreviation()
        {
            return abbreviation;
        }
    }

    private final String text;
    private final String insertText;
    private final String description;
    private final Kind kind;
    private final int priority;

    public VTLCompletionItem(final String text, final String insertText, final String description, final Kind kind, final int priority)
    {
        this.text = text;
        this.insertText = insertText != null ? insertText : text;
        this.description = description;
        this.kind = kind;
        this.priority = priority;
    }

    public VTLCompletionItem(final String text, final String description, final Kind kind)
    {
        this(text, text, description, kind, 100);
    }

    public VTLCompletionItem(final String text, final String description, final Kind kind, final int priority)
    {
        this(text, text, description, kind, priority);
    }

    public VTLCompletionItem(final String text, final String insertText, final String description, final Kind kind)
    {
        this(text, insertText, description, kind, 100);
    }

    @Override
    public void defaultAction(final JTextComponent component)
    {
        try
        {
            final Document doc = component.getDocument();
            final int caretPos = component.getCaretPosition();
            final int start = findInsertionStart(doc, caretPos);

            doc.remove(start, caretPos - start);
            doc.insertString(start, insertText, null);

            positionCursor(component, start, insertText);
        }
        catch (final BadLocationException ex)
        {
            SwingUtilities.invokeLater(() -> component.setText(component.getText() + insertText));
        }
    }

    private int findInsertionStart(final Document doc, final int caretPos) throws BadLocationException
    {
        int pos = caretPos - 1;
        while (pos >= 0)
        {
            final char ch = doc.getText(pos, 1).charAt(0);
            if (Character.isJavaIdentifierPart(ch) || ch == '#' || ch == '$')
            {
                pos--;
            }
            else
            {
                break;
            }
        }
        return pos + 1;
    }

    private void positionCursor(final JTextComponent component, final int start, final String inserted)
    {
        final int cursorPos;
        if (inserted.endsWith("()"))
        {
            cursorPos = start + inserted.length() - 1;
        }
        else if (inserted.endsWith("($"))
        {
            cursorPos = start + inserted.length();
        }
        else
        {
            cursorPos = start + inserted.length();
        }
        component.setCaretPosition(cursorPos);
    }

    @Override
    public CharSequence getInsertPrefix()
    {
        return text;
    }

    @Override
    public CharSequence getSortText()
    {
        return text;
    }

    @Override
    public int getSortPriority()
    {
        return priority;
    }

    @Override
    public void render(final Graphics g, final Font defaultFont, final Color defaultColor,
                       final Color backgroundColor, final int width, final int height, final boolean selected)
    {
        g.setFont(defaultFont);
        g.setColor(selected ? Color.WHITE : defaultColor);

        int x = 2;
        final int y = height - 4;

        final String kindLabel = kind.getAbbreviation();
        g.setColor(selected ? new Color(100, 180, 255) : new Color(0, 100, 200));
        g.drawString(kindLabel, x, y);
        x += g.getFontMetrics().stringWidth(kindLabel) + 6;

        g.setColor(selected ? Color.WHITE : defaultColor);
        g.drawString(text, x, y);

        if (description != null && !description.isEmpty())
        {
            x = width - g.getFontMetrics().stringWidth(description) - 5;
            g.setColor(selected ? new Color(200, 200, 200) : new Color(140, 140, 140));
            g.drawString(description, x, y);
        }

        if (selected)
        {
            g.setColor(new Color(0, 120, 215));
            g.fillRect(0, 0, 2, height);
        }
    }

    @Override
    public int getPreferredWidth(final Graphics g, final Font defaultFont)
    {
        g.setFont(defaultFont);
        int width = g.getFontMetrics().stringWidth(kind.getAbbreviation());
        width += 6 + g.getFontMetrics().stringWidth(text);
        if (description != null && !description.isEmpty())
        {
            width += 10 + g.getFontMetrics().stringWidth(description);
        }
        return width + 10;
    }

    @Override
    public void processKeyEvent(final KeyEvent evt)
    {
    }

    @Override
    public CompletionTask createDocumentationTask()
    {
        return null;
    }

    @Override
    public CompletionTask createToolTipTask()
    {
        return null;
    }

    @Override
    public boolean instantSubstitution(final JTextComponent component)
    {
        return false;
    }
}
