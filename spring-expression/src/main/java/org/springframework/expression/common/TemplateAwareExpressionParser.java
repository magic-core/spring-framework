/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.common;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.lang.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * An expression parser that understands templates. It can be subclassed by expression
 * parsers that do not offer first class support for templating.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 */
public abstract class TemplateAwareExpressionParser implements ExpressionParser {

	@Override
	public Expression parseExpression(String expressionString) throws ParseException {
		return parseExpression(expressionString, null);
	}

	/**
	 * 返回LiteralExpression实例（Pojo对象）（封装"expressionString"字符串）
	 *
	 * @param expressionString 要解析的字符串
	 * @param context 用于包含${表达式}的复杂情况，本Demo执行源码用不到
	 * @return
	 * @throws ParseException
	 */
	@Override
	public Expression parseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
		if (context != null && context.isTemplate()) {
			// 返回LiteralExpression实例（Pojo对象）（封装"expressionString"字符串）
			return parseTemplate(expressionString, context);
		}
		else {
			return doParseExpression(expressionString, context);
		}
	}


	/**
	 * 返回LiteralExpression实例（Pojo对象）（封装"expressionString"字符串）
	 *
	 * @param expressionString 要解析的字符串
	 * @param context 用于包含${表达式}的复杂情况，本Demo执行源码用不到
	 * @return
	 * @throws ParseException
	 */
	private Expression parseTemplate(String expressionString, ParserContext context) throws ParseException {
		if (expressionString.isEmpty()) {
			return new LiteralExpression("");
		}
		// 返回LiteralExpression实例（Pojo对象）（封装"expressionString"字符串）
		Expression[] expressions = parseExpressions(expressionString, context);
		// 本Demo，走本分支
		if (expressions.length == 1) {
			return expressions[0];
		}
		// Demo不涉及，暂不深解
		// 只针对"expressionString"包含${表达式}的复杂情况
		else {
			return new CompositeStringExpression(expressionString, expressions);
		}
	}

	/**
	 * 返回LiteralExpression实例（封装expressionString字符串）
	 *
	 * Helper that parses given expression string using the configured parser. The
	 * expression string can contain any number of expressions all contained in "${...}"
	 * markers. For instance: "foo${expr0}bar${expr1}". The static pieces of text will
	 * also be returned as Expressions that just return that static piece of text. As a
	 * result, evaluating all returned expressions and concatenating the results produces
	 * the complete evaluated string. Unwrapping is only done of the outermost delimiters
	 * found, so the string 'hello ${foo${abc}}' would break into the pieces 'hello ' and
	 * 'foo${abc}'. This means that expression languages that used ${..} as part of their
	 * functionality are supported without any problem. The parsing is aware of the
	 * structure of an embedded expression. It assumes that parentheses '(', square
	 * brackets '[' and curly brackets '}' must be in pairs within the expression unless
	 * they are within a string literal and a string literal starts and terminates with a
	 * single quote '.
	 * @param expressionString the expression string
	 * @return the parsed expressions
	 * @throws ParseException when the expressions cannot be parsed
	 */
	private Expression[] parseExpressions(String expressionString, ParserContext context) throws ParseException {
		List<Expression> expressions = new ArrayList<>();
		String prefix = context.getExpressionPrefix();// "#{"
		String suffix = context.getExpressionSuffix();// "}"
		int startIdx = 0;

		while (startIdx < expressionString.length()) {
			// Demo不涉及，暂不深解
			// 以下逻辑是 expressionString 包含"#{表达式}的处理逻辑
			int prefixIndex = expressionString.indexOf(prefix, startIdx);
			if (prefixIndex >= startIdx) {
				// an inner expression was found - this is a composite
				if (prefixIndex > startIdx) {
					expressions.add(new LiteralExpression(expressionString.substring(startIdx, prefixIndex)));
				}
				int afterPrefixIndex = prefixIndex + prefix.length();
				int suffixIndex = skipToCorrectEndSuffix(suffix, expressionString, afterPrefixIndex);
				if (suffixIndex == -1) {
					throw new ParseException(expressionString, prefixIndex,
							"No ending suffix '" + suffix + "' for expression starting at character " +
							prefixIndex + ": " + expressionString.substring(prefixIndex));
				}
				if (suffixIndex == afterPrefixIndex) {
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix +
							"' at character " + prefixIndex);
				}
				String expr = expressionString.substring(prefixIndex + prefix.length(), suffixIndex);
				expr = expr.trim();
				if (expr.isEmpty()) {
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix +
							"' at character " + prefixIndex);
				}
				expressions.add(doParseExpression(expr, context));
				startIdx = suffixIndex + suffix.length();
			}
			else {
				// expressionString 字符串中不存在${表达式}，直接添加静态文本
				expressions.add(new LiteralExpression(expressionString.substring(startIdx)));
				startIdx = expressionString.length();
			}
		}

		return expressions.toArray(new Expression[0]);
	}

	/**
	 * Return true if the specified suffix can be found at the supplied position in the
	 * supplied expression string.
	 * @param expressionString the expression string which may contain the suffix
	 * @param pos the start position at which to check for the suffix
	 * @param suffix the suffix string
	 */
	private boolean isSuffixHere(String expressionString, int pos, String suffix) {
		int suffixPosition = 0;
		for (int i = 0; i < suffix.length() && pos < expressionString.length(); i++) {
			if (expressionString.charAt(pos++) != suffix.charAt(suffixPosition++)) {
				return false;
			}
		}
		if (suffixPosition != suffix.length()) {
			// the expressionString ran out before the suffix could entirely be found
			return false;
		}
		return true;
	}

	/**
	 * Copes with nesting, for example '${...${...}}' where the correct end for the first
	 * ${ is the final }.
	 * @param suffix the suffix
	 * @param expressionString the expression string
	 * @param afterPrefixIndex the most recently found prefix location for which the
	 * matching end suffix is being sought
	 * @return the position of the correct matching nextSuffix or -1 if none can be found
	 */
	private int skipToCorrectEndSuffix(String suffix, String expressionString, int afterPrefixIndex)
			throws ParseException {

		// Chew on the expression text - relying on the rules:
		// brackets must be in pairs: () [] {}
		// string literals are "..." or '...' and these may contain unmatched brackets
		int pos = afterPrefixIndex;
		int maxlen = expressionString.length();
		int nextSuffix = expressionString.indexOf(suffix, afterPrefixIndex);
		if (nextSuffix == -1) {
			return -1; // the suffix is missing
		}
		Deque<Bracket> stack = new ArrayDeque<>();
		while (pos < maxlen) {
			if (isSuffixHere(expressionString, pos, suffix) && stack.isEmpty()) {
				break;
			}
			char ch = expressionString.charAt(pos);
			switch (ch) {
				case '{':
				case '[':
				case '(':
					stack.push(new Bracket(ch, pos));
					break;
				case '}':
				case ']':
				case ')':
					if (stack.isEmpty()) {
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " without an opening '" +
								Bracket.theOpenBracketFor(ch) + "'");
					}
					Bracket p = stack.pop();
					if (!p.compatibleWithCloseBracket(ch)) {
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " but most recent opening is '" + p.bracket +
								"' at position " + p.pos);
					}
					break;
				case '\'':
				case '"':
					// jump to the end of the literal
					int endLiteral = expressionString.indexOf(ch, pos + 1);
					if (endLiteral == -1) {
						throw new ParseException(expressionString, pos,
								"Found non terminating string literal starting at position " + pos);
					}
					pos = endLiteral;
					break;
			}
			pos++;
		}
		if (!stack.isEmpty()) {
			Bracket p = stack.pop();
			throw new ParseException(expressionString, p.pos, "Missing closing '" +
					Bracket.theCloseBracketFor(p.bracket) + "' for '" + p.bracket + "' at position " + p.pos);
		}
		if (!isSuffixHere(expressionString, pos, suffix)) {
			return -1;
		}
		return pos;
	}


	/**
	 * Actually parse the expression string and return an Expression object.
	 * @param expressionString the raw expression string to parse
	 * @param context a context for influencing this expression parsing routine (optional)
	 * @return an evaluator for the parsed expression
	 * @throws ParseException an exception occurred during parsing
	 */
	protected abstract Expression doParseExpression(String expressionString, @Nullable ParserContext context)
			throws ParseException;


	/**
	 * This captures a type of bracket and the position in which it occurs in the
	 * expression. The positional information is used if an error has to be reported
	 * because the related end bracket cannot be found. Bracket is used to describe:
	 * square brackets [] round brackets () and curly brackets {}
	 */
	private static class Bracket {

		char bracket;

		int pos;

		Bracket(char bracket, int pos) {
			this.bracket = bracket;
			this.pos = pos;
		}

		boolean compatibleWithCloseBracket(char closeBracket) {
			if (this.bracket == '{') {
				return closeBracket == '}';
			}
			else if (this.bracket == '[') {
				return closeBracket == ']';
			}
			return closeBracket == ')';
		}

		static char theOpenBracketFor(char closeBracket) {
			if (closeBracket == '}') {
				return '{';
			}
			else if (closeBracket == ']') {
				return '[';
			}
			return '(';
		}

		static char theCloseBracketFor(char openBracket) {
			if (openBracket == '{') {
				return '}';
			}
			else if (openBracket == '[') {
				return ']';
			}
			return ')';
		}
	}

}
