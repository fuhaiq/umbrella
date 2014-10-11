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

import java.awt.Point;
import java.util.Stack;


/**
 * A utility class that detects matching open/close bracket pairs in Mathematica input. This class
 * is used by MathSessionPane to implement its bracket-matching (also called bracket balancing) feature,
 * but you can also use it directly in your own programs.
 * <p>
 * BracketMatcher detects matching bracket pairs of the following types: (), {}, [], (**). It ignores brackets
 * within strings and within Mathematica comments. It can acommodate nested comments. It searches in
 * the typical way--expanding the current selection left and right to find the first enclosing matching
 * brackets. If this description is not clear enough, simply create a MathSessionPane and investigate
 * how its bracket-matching feature behaves.
 * <p>
 * To use it, create an instance and supply the input text either in the constructor or by using the setText()
 * method. Then call the balance() method, supplying the character position of the left end of the current
 * selection (or the position of the caret if there is no selection) and the length of the selection in
 * characters (0 if there is no selected region). The balance() method returns a Point, which acts simply as
 * a container for two integers: the x value is the character position of the left-most matching bracket
 * and the y value is the character position just to the right of the rightmost bracket. That means that
 * these numbers mark the beginning and end of a selection region that encompasses a matched bracket pair.
 * Null is returned if there is no block larger than the current selection that is enclosed by a matched
 * pair and has no unclosed brackets within it.
 * <p>
 * A single BracketMatcher instance can be reused with different text by calling the setText() method each time.
 * 
 * @since 2.0
 * @see com.wolfram.jlink.ui.MathSessionPane
 */
public class BracketMatcher {

	private String text;
	
	public BracketMatcher() {
		this("");
	}
	
	/**
	 * @param text the string of input that will be searched for matches
	 */
	public BracketMatcher(String text) {
		this.text = text;
	}
	
	/**
	 * Sets the string of input that will be searched for matches.
	 * 
	 * @param text
	 */
	public void setText(String text) {
		this.text = text;
	}
	

	/**
	 * Performs the search for matching brackets.
	 * 
	 * @param leftEndOfSelection the character position of the left end of the current selection (or the position of the caret
	 * if there is no selection)
	 * @param selectionLength the length of the selection in characters (0 if there is no selected region)
	 * @return a Point whose x value is the character position of the left-most matching bracket
	 * and whose y value is the character position just to the right of the rightmost bracket.
	 * Null if there is no matching set that encloses the current selection.
	 */
	public Point balance(int leftEndOfSelection, int selectionLength) {

		Stack stack = new Stack();
		char c = 0;
		int i;
		int textLength = text.length();
   	
		// rightEndOfSelection is index of first char to right of right end of sel.
		int rightEndOfSelection = leftEndOfSelection + selectionLength;
	    
		if (leftEndOfSelection < textLength && isOpenBracket(text.charAt(leftEndOfSelection))) {
			for (i = leftEndOfSelection; i < textLength; i++) {
				c = text.charAt(i);
				if (isOpenBracket(c)) {
					stack.push(new Character(c));
				} else if (isCloseBracket(c)) {
					if (matchingBracket(c) != ((Character) stack.peek()).charValue())
						return null;
					stack.pop();
					if (stack.isEmpty()) {
						if (i >= rightEndOfSelection) {
							return new Point(leftEndOfSelection, i + 1);
						} else {
							// Need to grow both directions.
							break;
						}
					}
				} 
			}
			if (i == textLength)
				return null;
		} else if (leftEndOfSelection == 0) {
			return null;      	
		} else if (isCloseBracket(text.charAt(rightEndOfSelection - 1))) {
			for (i = rightEndOfSelection - 1; i >= 0; i--) {
				c = text.charAt(i);
				if (isCloseBracket(c)) {
					stack.push(new Character(c));
				} else if (isOpenBracket(c)) {
					if (matchingBracket(c) != ((Character) stack.peek()).charValue())
						return null;
					stack.pop();
					if (stack.isEmpty()) {
						if (i < leftEndOfSelection) {
							return new Point(i, rightEndOfSelection);
						} else {
							// Need to grow both directions.
							break;
						}
					}
				} 
			}
			if (i < 0)
				return null;
		} else if (rightEndOfSelection == textLength) {
			return null;
		}

		// If we fall through to here, the sel will be grown in both directions to search for a match.
	    
		int curLeft = leftEndOfSelection - 1;  	// Index of first char to left of left end of sel.
		int curRight = leftEndOfSelection;

		while (true) {
			stack.clear();
			for (i = curLeft; i >= 0; i--) {
				c = text.charAt(i);
				if (isCloseBracket(c)) {
					stack.push(new Character(c));
				} else if (isOpenBracket(c)) {
					if (stack.isEmpty()) {
						break;
					} else {
						if(matchingBracket(c) != ((Character) stack.peek()).charValue())
							return null;
						stack.pop();
					}
				}
			}
			if (i < 0)
				return null;

			// Left-moving walk has stopped at the first unmatched open bracket to the left of curRight.
			int bracketPos = i;
			stack.clear();
			stack.push(new Character(c));
			for (i = curRight; i < textLength; i++) {
				c = text.charAt(i);
				if (isOpenBracket(c)) {
					stack.push(new Character(c));
				} else if (isCloseBracket(c)) {
					if (matchingBracket(c) != ((Character) stack.peek()).charValue())
						return null;
					stack.pop();
					if (stack.isEmpty()) {
						if (i >= rightEndOfSelection) {
							return new Point(bracketPos, i + 1);
						} else {
							curLeft = bracketPos - 1;
							curRight = i + 1;
							break;
						}
					}
				}
			}
			if (i == textLength)
				return null;
		}
	}


	private static boolean isOpenBracket(char c) {
		return c == '[' || c == '(' || c == '{';
	}
	
	private static boolean isCloseBracket(char c) {
		return c == ']' || c == ')' || c == '}';
	}
	
	private static char matchingBracket(char c) {

		switch (c) {
			case '(':
				return ')';
			case ')':
				return '(';
			case '[':
				return ']';
			case ']':
				return '[';
			case '{':
				return '}';
			case '}':
				return '{';
			default:
				return 0;
		}
	}

}