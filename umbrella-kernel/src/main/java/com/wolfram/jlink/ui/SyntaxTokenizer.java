//////////////////////////////////////////////////////////////////////////////////////
//
//   J/Link source code (c) 1999-2002, Wolfram Research, Inc. All rights reserved.
//
//   Use is governed by the terms of the J/Link license agreement, which can be found at
//   www.wolfram.com/solutions/mathlink/jlink.
//
//   Author: Todd Gayley
//
//////////////////////////////////////////////////////////////////////////////////////

package com.wolfram.jlink.ui;


/**
 * A utility class that can break up Mathematica code into 4 syntax classes: strings, comments, symbols,
 * and normal (meaning everything else). This class is used by MathSessionPane to implement its syntax coloring
 * feature, but you can use it directly in your own programs.
 * <p>
 * To use a SyntaxTokenizer, construct one and then call its setText() method, supplying the Mathematica
 * input you want tokenized. You then call getNextRecord() repeatedly to get SyntaxRecords, which tell
 * you the type of syntax element and the length in characters.
 * <p>
 * This process is very fast. You can iterate through 100,000 characters of Mathematica code in a
 * small fraction of a second
 * <p>
 * Here is some sample code that demonstrates how to use a SyntaxTokenizer:
 * <pre>
 * 	String input = "some Mathematica code here";
 * 	SyntaxTokenizer tok = new SyntaxTokenizer();
 * 	tok.setText(input);
 * 	while(tok.hasMoreRecords()) {
 * 		SyntaxTokenizer.SyntaxRecord rec = tok.getNextRecord();
 * 		System.out.println("type: " + rec.type);
 * 		System.out.println("text: " + input.substring(rec.start, rec.start + rec.length));
 * 	}</pre>
 * 
 * @since 2.0
 * @see MathSessionPane
 */
public class SyntaxTokenizer {

	/**
	 * A syntax type that consists of everything other than STRING, COMMENT, or SYMBOL.
	 */
	public static final int NORMAL    = 0;

	/**
	 * A syntax type that corresponds to a literal string.
	 */
	public static final int STRING    = 1;

	/**
	 * A syntax type that corresponds to a Mathematica comment.
	 */
	public static final int COMMENT   = 2;
	
	/**
	 * A syntax type that corresponds to a Mathematica symbol.
	 */
	public static final int SYMBOL    = 3;
	
	private static final int UNKNOWN  = 4;
	
	private String text;
	private int state = UNKNOWN;
	private int commentLevel = 0;
	private int tokenStart = 0;
	private int charIndex = -1;
	private int len = 0;
	
	
	/**
	 * Sets the Mathematica input text to tokenize.
	 * 
	 * @param text
	 */
	public void setText(String text) {
		
		this.text = text;
		reset();
	}
	
	/**
	 * Resets the state of the tokenizer so that the next call to getNextRecord() will retrieve
	 * the first record in the text.
	 */
	public void reset() {
		
		len = text.length();
		commentLevel = 0;
		tokenStart = 0;
		state = UNKNOWN;
		charIndex = -1;  // Start at -1 because it is preincremented.
	}
	
	
	/**
	 * Gets the next SyntaxRecord specifying the type of the element (SYMBOL, STRING, COMMENT or NORMAL),
	 * its start position, and length.
	 */
	public SyntaxRecord getNextRecord() {
		
		SyntaxRecord result = null;
		
		while (++charIndex < len) {
			
			char c = text.charAt(charIndex);
				
			switch (getState()) {
				case COMMENT:
					if (c == ')' && text.charAt(charIndex - 1) == '*' && text.charAt(charIndex - 2) != '(') {
						decrementComment();
						if (getCommentLevel() == 0)
							return switchState(COMMENT, UNKNOWN);
					} else if (c == '(' && charIndex < len - 1 && text.charAt(charIndex + 1) == '*') {
						setState(COMMENT);
					}
					break;
				case STRING:
					if (c == '"') {
						int numBackslashes = 0;
						for (int i = charIndex - 1; i >= 0 && text.charAt(i) == '\\'; i--)
							numBackslashes++;
						if ((numBackslashes % 2) == 0)
							return switchState(STRING, UNKNOWN);
					}
					break;
				case SYMBOL:
					if (c == '"')
						return switchState(SYMBOL, STRING);
					else if (c == '(' && charIndex < len - 1 && text.charAt(charIndex + 1) == '*')
						return switchState(SYMBOL, COMMENT);
					else if (!isSymbolChar(c))
						return switchState(SYMBOL, NORMAL);
					break;
				case NORMAL:
					if (c == '"')
						return switchState(NORMAL, STRING);
					else if (c == '(' && charIndex < len - 1 && text.charAt(charIndex + 1) == '*')
						return switchState(NORMAL, COMMENT);
					else if (isSymbolStart(c))
						return switchState(NORMAL, SYMBOL);
					break;
				case UNKNOWN:
					if (c == '"') {
						setState(STRING);
					} else if (c == '(' && charIndex < len - 1 && text.charAt(charIndex + 1) == '*') {
						setState(COMMENT);
					} else if (isSymbolStart(c)) {
						setState(SYMBOL);
					} else {
						setState(NORMAL);
					}
					break;
			}		
		}
		// If we get to here, we have come to the end of input and no special token detected. Note that
		// charIndex has already been incremented to next char (which is beyond the end of input).
		if (getState() == SYMBOL) {
			return switchState(SYMBOL, UNKNOWN);
		} else if (getState() != UNKNOWN) {
			return switchState(NORMAL, UNKNOWN);
		} else {
			setState(UNKNOWN);
			return null;
		}
	}
	

	/**
	 * Returns true or false to indicate whether there are any more records left in the text
	 * (i.e., whether we have come to the end of the input).
	 */
	public boolean hasMoreRecords() {
		// 2nd part of || is for case where we have a one-char length token at end of text. We have walked past the
		// end, but not consumed the last char.
		return charIndex < len - 1 || charIndex == len - 1 && getState() != UNKNOWN;
	}
	
	
	/**
	 * A simple class the encapsulates information about a syntax element.
	 */
	public class SyntaxRecord {
		
		/**
		 * The type of the syntax element. Will be one of: SyntaxTokenizer.NORMAL, SyntaxTokenizer.SYMBOL, 
		 * SyntaxTokenizer.STRING, SyntaxTokenizer.COMMENT.
		 */
		public int type;
		/**
		 * The character position at which this syntax element begins.
		 */
		public int start;
		/**
		 * The length in characters of this syntax element.
		 */
		public int length;
		
		SyntaxRecord(int type, int start, int length) {
			this.type = type;
			this.start = start;
			this.length = length;
		}
	}


	//////////////////////////////////  Private  //////////////////////////////////////
	
	private SyntaxRecord switchState(int from, int to) {
	
		int length = (from == COMMENT || from == STRING) ? charIndex - tokenStart + 1 : charIndex - tokenStart;
		SyntaxRecord result = new SyntaxRecord(from, tokenStart, length);
		setState(to);
		return result;
	}
	
	private void setState(int type) {
		
		state = type;
		if (type == COMMENT) {
			if (++commentLevel == 1)
				tokenStart = charIndex;
		} else {
			tokenStart = charIndex;
		}
	}
	
	private int getState() {
		return state;
	}
		
	private void decrementComment() {
		commentLevel--;
	}
	
	private int getCommentLevel() {
		return commentLevel;
	}
		
	private static boolean isSymbolStart(char c) {
		return Character.isLetter(c) || c == '$' || c == '`';
	}

	private static boolean isSymbolChar(char c) {
		return Character.isLetterOrDigit(c) || c == '$' || c == '`';
	}
	
}

