/*
 * $Id$
 *
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.javatest;

import com.sun.javatest.util.I18NResourceBundle;
import com.sun.javatest.util.StringArray;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

//------------------------------------------------------------------------------

/**
 * A filter for sets of keywords, as found on test descriptions.
 *
 * @see TestDescription#getKeywordTable
 */
public abstract class Keywords {
    /**
     * A constant to indicate that all of a list of keywords should be matched.
     */
    public static final String ALL_OF = "all of";
    /**
     * A constant to indicate that any of a list of keywords should be matched.
     */
    public static final String ANY_OF = "any of";
    /**
     * A constant to indicate that an expression keyword should be matched.
     */
    public static final String EXPR = "expr";
    static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Keywords.class);
    protected String text;

    /**
     * Create a keywords object.
     *
     * @param type one of ALL_OF, ANY_OF, or EXPR
     * @param text if the type is one of "all of" or "any of", text should
     *             be a white-space separated list of keywords; if type is "expr",
     *             text should be a boolean valued expression formed from
     *             keywords, '&amp;' (and), '|' (or), '!' (not) and '(' ')' (parentheses).
     * @return A Keywords object for the specified type and text.
     * @throws Keywords.Fault if there are errors in the arguments.
     */
    public static Keywords create(String type, String text) throws Fault {
        return create(type, text, null);
    }

    /**
     * Create a keywords object.
     *
     * @param type          one of ALL_OF, ANY_OF, or EXPR
     * @param text          if the type is one of "all of" or "any of", text should
     *                      be a white-space separated list of keywords; if type is "expr",
     *                      text should be a boolean valued expression formed from
     *                      keywords, '&amp;' (and), '|' (or), '!' (not) and '(' ')' (parentheses).
     * @param validKeywords a set of valid keywords for this test suite,
     *                      or null.
     *                      If not null, all the keywords in <i>text</i> must be in this set.
     * @return A Keywords object for the specified type and text.
     * @throws Keywords.Fault if there are errors in the arguments.
     */
    public static Keywords create(String type, String text, Set<String> validKeywords) throws Fault {
        Set<String> lowerCaseValidKeywords = toLowerCase(validKeywords);
        if (text == null) {
            text = "";
        }

        Keywords result = null;
        if (type == null || type.equals("ignore")) {
            return null;
        } else if (type.equals(ALL_OF)) {
            result = new AllKeywords(StringArray.split(text), lowerCaseValidKeywords);
            result.setSummary(result.toString());
            return result;
        } else if (type.equals(ANY_OF)) {
            result = new AnyKeywords(StringArray.split(text), lowerCaseValidKeywords);
            result.setSummary(result.toString());
            return result;
        } else if (type.equals(EXPR)) {
            ExprParser p = new ExprParser(text, lowerCaseValidKeywords);
            result = p.parse();
            result.setSummary(text);
            return result;
        } else {
            throw new Fault(i18n, "kw.badKeywordType", type);
        }
    }

    /**
     * Allow keywords to begin with a numeric or not.
     *
     * @param allowNumericKeywords Value to be set.
     */
    public static void setAllowNumericKeywords(boolean allowNumericKeywords) {
        ExprParser.allowNumericKeywords = allowNumericKeywords;
    }

    private static Set<String> toLowerCase(Set<String> words) {
        if (words == null) {
            return null;
        }

        boolean allLowerCase = true;
        for (Iterator<String> iter = words.iterator(); iter.hasNext() && allLowerCase; ) {
            String word = iter.next();
            allLowerCase &= word.equals(word.toLowerCase());
        }

        if (allLowerCase) {
            return words;
        }

        Set<String> s = new HashSet<>();
        for (String word : words) {
            s.add(word.toLowerCase());
        }

        return s;
    }

    private static boolean isLowerCase(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isUpperCase(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a human digestible version of the kw represented by this object.
     *
     * @return Human readable, fully descriptive rendering of current kw setting
     */
    public String getSummary() {
        return text;
    }

    /**
     * Set the descriptive representation of the kw expression provided by the user.
     *
     * @param text Useful text rendering of current kw expression
     */
    void setSummary(String text) {
        this.text = text;
    }

    /**
     * Check if this keywords object accepts, or matches, the specified
     * set of words. If the keywords type is "any of" or "all of",
     * the set must have any or of all of the words specified
     * in the keywords object; if the keywords type is "expr", the
     * given expression must evaluate to true, when the words in the
     * expression are true if they are present in the given set of words.
     *
     * @param s A set of words to compare against the keywords object.
     * @return true if the the specified set of words are compatible
     * with this keywords object.
     */
    public abstract boolean accepts(Set<String> s);

    /**
     * An exception used to report errors while using a Keywords object.
     */
    public static class Fault extends Exception {
        /**
         * Create a Fault.
         *
         * @param i18n A resource bundle in which to find the detail message.
         * @param s    The key for the detail message.
         */
        Fault(I18NResourceBundle i18n, String s) {
            super(i18n.getString(s));
        }

        /**
         * Create a Fault.
         *
         * @param i18n A resource bundle in which to find the detail message.
         * @param s    The key for the detail message.
         * @param o    An argument to be formatted with the detail message by
         *             {@link java.text.MessageFormat#format}
         */
        Fault(I18NResourceBundle i18n, String s, Object o) {
            super(i18n.getString(s, o));
        }

        /**
         * Create a Fault.
         *
         * @param i18n A resource bundle in which to find the detail message.
         * @param s    The key for the detail message.
         * @param o    An array of arguments to be formatted with the detail message by
         *             {@link java.text.MessageFormat#format}
         */
        Fault(I18NResourceBundle i18n, String s, Object... o) {
            super(i18n.getString(s, o));
        }
    }

}

//------------------------------------------------------------------------------


//------------------------------------------------------------------------------

abstract class SetKeywords extends Keywords {
    Set<String> keys;
    String allKwds = ""; // string to be used by toString()

    SetKeywords(String[] keywords, Set<String> validKeywords) throws Keywords.Fault {
        if (keywords.length == 0) {
            throw new Keywords.Fault(i18n, "kw.noKeywords");
        }

        keys = new HashSet<>();
        for (String keyword : keywords) {
            String lowerCaseKeyword = keyword.toLowerCase();
            if (validKeywords != null && !validKeywords.contains(lowerCaseKeyword)) {
                throw new Fault(i18n, "kw.invalidKeyword", keyword);
            }
            keys.add(lowerCaseKeyword);
            allKwds += lowerCaseKeyword + " ";
        }
        if (!allKwds.isEmpty()) {
            // remove last " "
            allKwds = allKwds.substring(0, allKwds.length() - 1);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SetKeywords other = (SetKeywords) obj;
        if (this.keys != other.keys && (this.keys == null || !this.keys.equals(other.keys))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + (this.keys != null ? this.keys.hashCode() : 0);
        return hash;
    }

}

class AllKeywords extends SetKeywords {
    AllKeywords(String[] keys, Set<String> validKeywords) throws Keywords.Fault {
        super(keys, validKeywords);
    }


    /**
     * Returns true, iff all keywords are in the set.
     *
     * @param s
     * @return
     */
    @Override
    public boolean accepts(Set<String> s) {
        return s.containsAll(keys);
    }

    @Override
    public String toString() {
        return "all of (" + allKwds + ")";
    }
}


class AnyKeywords extends SetKeywords {
    AnyKeywords(String[] keys, Set<String> validKeywords) throws Keywords.Fault {
        super(keys, validKeywords);
    }

    /**
     * @param s - the set
     * @return false, if none of the keywords is in the set
     */
    @Override
    public boolean accepts(Set<String> s) {
        for (String kwd : keys) {
            if (s.contains(kwd)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "any of (" + allKwds + ")";
    }
}

//------------------------------------------------------------------------------

class ExprParser {
    private static final int
            ID = 0, AND = 1, OR = 2, NOT = 3, LPAREN = 4, RPAREN = 5, END = 6, ERROR = 7;
    protected static boolean allowNumericKeywords =
            Boolean.getBoolean("javatest.allowNumericKeywords");
    private static I18NResourceBundle i18n = Keywords.i18n;
    private String text;
    private Set<String> validKeywords;
    private int index;
    private int token;
    private String idValue;
    ExprParser(String text, Set<String> validKeywords) {
        this.text = text;
        this.validKeywords = validKeywords;
        nextToken();
    }

    ExprKeywords parse() throws Keywords.Fault {
        if (text == null || text.trim().isEmpty()) {
            throw new Keywords.Fault(i18n, "kw.noExpr");
        }

        ExprKeywords e = parseExpr();
        expect(END);
        return e;
    }

    ExprKeywords parseExpr() throws Keywords.Fault {
        for (ExprKeywords e = parseTerm(); e != null; e = e.order()) {
            switch (token) {
                case AND:
                    nextToken();
                    e = new AndExprKeywords(e, parseTerm());
                    break;
                case OR:
                    nextToken();
                    e = new OrExprKeywords(e, parseTerm());
                    break;
                default:
                    return e;
            }
        }
        // bogus return to keep compiler happy
        return null;
    }

    ExprKeywords parseTerm() throws Keywords.Fault {
        switch (token) {
            case ID:
                String id = idValue;
                if (validKeywords != null && !validKeywords.contains(id)) {
                    throw new Keywords.Fault(i18n, "kw.invalidKeyword", id);
                }
                nextToken();
                return new TermExprKeywords(id);
            case NOT:
                nextToken();
                return new NotExprKeywords(parseTerm());
            case LPAREN:
                nextToken();
                ExprKeywords e = parseExpr();
                expect(RPAREN);
                return new ParenExprKeywords(e);
            default:
                throw new Keywords.Fault(i18n, "kw.badKeywordExpr");
        }
    }

    private void expect(int t) throws Keywords.Fault {
        if (t == token) {
            nextToken();
        } else {
            throw new Keywords.Fault(i18n, "kw.badKeywordExpr");
        }
    }

    private void nextToken() {
        while (index < text.length()) {
            char c = text.charAt(index++);
            switch (c) {
                case ' ':
                case '\t':
                    continue;
                case '&':
                    token = AND;
                    return;
                case '|':
                    token = OR;
                    return;
                case '!':
                    token = NOT;
                    return;
                case '(':
                    token = LPAREN;
                    return;
                case ')':
                    token = RPAREN;
                    return;
                default:
                    if (Character.isUnicodeIdentifierStart(c) ||
                            (allowNumericKeywords && Character.isDigit(c))) {
                        idValue = String.valueOf(Character.toLowerCase(c));
                        while (index < text.length()
                                && Character.isUnicodeIdentifierPart(text.charAt(index))) {
                            char ch = text.charAt(index++);
                            if (!Character.isIdentifierIgnorable(ch)) {
                                idValue += Character.toLowerCase(ch);
                            }
                        }
                        token = ID;
                        return;
                    } else {
                        token = ERROR;
                        return;
                    }
            }
        }
        token = END;
    }
}

//------------------------------------------------------------------------------

abstract class ExprKeywords extends Keywords {

    abstract int precedence();

    ExprKeywords order() {
        return this;
    }
}

//------------------------------------------------------------------------------


//------------------------------------------------------------------------------

abstract class BinaryExprKeywords extends ExprKeywords {
    protected ExprKeywords left;
    protected ExprKeywords right;

    BinaryExprKeywords(ExprKeywords left, ExprKeywords right) {
        this.left = left;
        this.right = right;
    }

    @Override
    ExprKeywords order() {
        if (precedence() > left.precedence() && left instanceof BinaryExprKeywords) {
            BinaryExprKeywords e = (BinaryExprKeywords) left;
            left = e.right;
            e.right = order();
            return e;
        } else {
            return this;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BinaryExprKeywords other = (BinaryExprKeywords) obj;
        if (this.left != other.left && (this.left == null || !this.left.equals(other.left))) {
            return false;
        }
        if (this.right != other.right && (this.right == null || !this.right.equals(other.right))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.left != null ? this.left.hashCode() : 0);
        hash = 97 * hash + (this.right != null ? this.right.hashCode() : 0);
        return hash;
    }
}

class AndExprKeywords extends BinaryExprKeywords {

    AndExprKeywords(ExprKeywords left, ExprKeywords right) {
        super(left, right);
    }

    @Override
    public boolean accepts(Set<String> s) {
        return left.accepts(s) && right.accepts(s);
    }

    @Override
    int precedence() {
        return 1;
    }

    @Override
    public String toString() {
        return "`" + left + "&" + right + "'";
    }
}

//------------------------------------------------------------------------------


//------------------------------------------------------------------------------

class NotExprKeywords extends ExprKeywords {
    private ExprKeywords expr;

    NotExprKeywords(ExprKeywords expr) {
        this.expr = expr;
    }

    @Override
    public boolean accepts(Set<String> s) {
        return !expr.accepts(s);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NotExprKeywords other = (NotExprKeywords) obj;
        if (this.expr != other.expr && (this.expr == null || !this.expr.equals(other.expr))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.expr != null ? this.expr.hashCode() : 0);
        return hash;
    }

    @Override
    int precedence() {
        return 2;
    }

    @Override
    public String toString() {
        return "!" + expr;
    }
}

class OrExprKeywords extends BinaryExprKeywords {
    OrExprKeywords(ExprKeywords left, ExprKeywords right) {
        super(left, right);
    }

    @Override
    public boolean accepts(Set<String> s) {
        return left.accepts(s) || right.accepts(s);
    }

    @Override
    int precedence() {
        return 0;
    }

    @Override
    public String toString() {
        return "`" + left + "|" + right + "'";
    }
}

//------------------------------------------------------------------------------


//------------------------------------------------------------------------------

class ParenExprKeywords extends ExprKeywords {
    private ExprKeywords expr;

    ParenExprKeywords(ExprKeywords expr) {
        this.expr = expr;
    }

    @Override
    public boolean accepts(Set<String> s) {
        return expr.accepts(s);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ParenExprKeywords other = (ParenExprKeywords) obj;
        if (this.expr != other.expr && (this.expr == null || !this.expr.equals(other.expr))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.expr != null ? this.expr.hashCode() : 0);
        return hash;
    }

    @Override
    int precedence() {
        return 2;
    }

    @Override
    public String toString() {
        return "(" + expr + ")";
    }
}

class TermExprKeywords extends ExprKeywords {
    private String key;

    TermExprKeywords(String key) {
        this.key = key;
    }

    @Override
    public boolean accepts(Set<String> s) {
        return s.contains(key);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TermExprKeywords other = (TermExprKeywords) obj;
        if ((this.key == null) ? (other.key != null) : !this.key.equals(other.key)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + (this.key != null ? this.key.hashCode() : 0);
        return hash;
    }

    @Override
    int precedence() {
        return 2;
    }

    @Override
    public String toString() {
        return key;
    }
}
