package org.antlr.v4.test;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.pattern.ParseTreePatternMatcher;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestParseTreeMatcher extends BaseTest {
	@Test public void testChunking() throws Exception {
		ParseTreePatternMatcher p = new ParseTreePatternMatcher();
		assertEquals("[ID, ' = ', expr, ' ;']", p.split("<ID> = <expr> ;").toString());
		assertEquals("[' ', ID, ' = ', expr]", p.split(" <ID> = <expr>").toString());
		assertEquals("[ID, ' = ', expr]", p.split("<ID> = <expr>").toString());
		assertEquals("[expr]", p.split("<expr>").toString());
		assertEquals("['<x> foo']", p.split("\\<x\\> foo").toString());
		assertEquals("['foo <x> bar ', tag]", p.split("foo \\<x\\> bar <tag>").toString());
	}

	@Test public void testDelimiters() throws Exception {
		ParseTreePatternMatcher p = new ParseTreePatternMatcher();
		p.setDelimiters("<<", ">>", "$");
		String result = p.split("<<ID>> = <<expr>> ;$<< ick $>>").toString();
		assertEquals("[ID, ' = ', expr, ' ;<< ick >>']", result);
	}

	@Test public void testInvertedTags() throws Exception {
		ParseTreePatternMatcher p = new ParseTreePatternMatcher();
		String result = null;
		try {
			p.split(">expr<");
		}
		catch (IllegalArgumentException iae) {
			result = iae.getMessage();
		}
		String expected = "tag delimiters out of order in pattern: >expr<";
		assertEquals(expected, result);
	}

	@Test public void testUnclosedTag() throws Exception {
		ParseTreePatternMatcher p = new ParseTreePatternMatcher();
		String result = null;
		try {
			p.split("<expr hi mom");
		}
		catch (IllegalArgumentException iae) {
			result = iae.getMessage();
		}
		String expected = "unterminated tag in pattern: <expr hi mom";
		assertEquals(expected, result);
	}

	@Test public void testExtraClose() throws Exception {
		ParseTreePatternMatcher p = new ParseTreePatternMatcher();
		String result = null;
		try {
			p.split("<expr> >");
		}
		catch (IllegalArgumentException iae) {
			result = iae.getMessage();
		}
		String expected = "missing start tag in pattern: <expr> >";
		assertEquals(expected, result);
	}

	@Test public void testTokenizingPattern() throws Exception {
		String grammar =
			"grammar X;\n" +
			"s : ID '=' expr ';' ;\n" +
			"expr : ID | INT ;\n" +
			"ID : [a-z]+ ;\n" +
			"INT : [0-9]+ ;\n" +
			"WS : [ \\r\\n\\t]+ -> skip ;\n";
		boolean ok =
			rawGenerateAndBuildRecognizer("X.g4", grammar, "XParser", "XLexer", false);
		assertTrue(ok);

		ParseTreePatternMatcher p =
			new ParseTreePatternMatcher(loadLexerClassFromTempDir("XLexer"),
										loadParserClassFromTempDir("XParser"));

		List<? extends Token> tokens = p.tokenizePattern("<ID> = <expr> ;");
		String results = tokens.toString();
		String expected = "[ID:3, [@-1,1:1='=',<1>,1:1], expr:1, [@-1,1:1=';',<2>,1:1]]";
		assertEquals(expected, results);
	}

	@Test
	public void testCompilingPattern() throws Exception {
		String grammar =
			"grammar X;\n" +
			"s : ID '=' expr ';' ;\n" +
			"expr : ID | INT ;\n" +
			"ID : [a-z]+ ;\n" +
			"INT : [0-9]+ ;\n" +
			"WS : [ \\r\\n\\t]+ -> skip ;\n";
		boolean ok =
			rawGenerateAndBuildRecognizer("X.g4", grammar, "XParser", "XLexer", false);
		assertTrue(ok);

		ParseTreePatternMatcher p =
			new ParseTreePatternMatcher(loadLexerClassFromTempDir("XLexer"),
										loadParserClassFromTempDir("XParser"));

		ParseTree t = p.compilePattern("s", "<ID> = <expr> ;");
		String results = t.toStringTree(p.getParser());
		String expected = "(s <ID> = expr ;)";
		assertEquals(expected, results);
	}

	@Test public void testIDNodeMatches() throws Exception {
		String grammar =
			"grammar X;\n" +
			"s : ID ';' ;\n" +
			"ID : [a-z]+ ;\n" +
			"WS : [ \\r\\n\\t]+ -> skip ;\n";

		String input = "x ;";
		String pattern = "<ID>;";
		checkPatternMatch("X.g4", grammar, "s", input, pattern, "XParser", "XLexer");
	}

	@Test public void testTokenAndRuleMatch() throws Exception {
		String grammar =
			"grammar X;\n" +
			"s : ID '=' expr ';' ;\n" +
			"expr : ID | INT ;\n" +
			"ID : [a-z]+ ;\n" +
			"INT : [0-9]+ ;\n" +
			"WS : [ \\r\\n\\t]+ -> skip ;\n";

		String input = "x = 99;";
		String pattern = "<ID> = <expr> ;";
		checkPatternMatch("X.g4", grammar, "s", input, pattern, "XParser", "XLexer");
	}

	public void checkPatternMatch(String grammarName, String grammar, String startRule,
								  String input, String pattern,
								  String parserName, String lexerName)
		throws Exception
	{
		boolean ok =
			rawGenerateAndBuildRecognizer(grammarName, grammar, parserName, lexerName, false);
		assertTrue(ok);

		ParseTree result = execParser(startRule, input, parserName, lexerName);

		ParseTreePatternMatcher p =
			new ParseTreePatternMatcher(loadLexerClassFromTempDir(lexerName),
										loadParserClassFromTempDir(parserName));
		boolean matches = p.matches(result, startRule, pattern);
		assertTrue(matches);
	}
}
