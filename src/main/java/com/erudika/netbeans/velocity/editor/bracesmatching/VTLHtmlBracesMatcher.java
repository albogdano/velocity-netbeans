/*
 * Copyright 2013-2026 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.erudika.netbeans.velocity.editor.bracesmatching;

import com.erudika.netbeans.velocity.lexer.VTLTokenId;
import java.util.Set;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import org.netbeans.api.html.lexer.HTMLTokenId;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.spi.editor.bracesmatching.BracesMatcher;
import org.netbeans.spi.editor.bracesmatching.BracesMatcherFactory;
import org.netbeans.spi.editor.bracesmatching.MatcherContext;

public final class VTLHtmlBracesMatcher implements BracesMatcher, BracesMatcherFactory {

	private static final Set<String> VOID_ELEMENTS = Set.of(
		"area", "base", "br", "col", "embed", "hr", "img", "input",
		"link", "meta", "param", "source", "track", "wbr"
	);

	private final MatcherContext context;
	private int originStart;
	private int originEnd;
	private String tagName;
	private boolean isClosingTag;
	private boolean isComment;

	public VTLHtmlBracesMatcher() {
		this(null);
	}

	private VTLHtmlBracesMatcher(MatcherContext context) {
		this.context = context;
	}

	@Override
	public BracesMatcher createMatcher(MatcherContext mc) {
		return new VTLHtmlBracesMatcher(mc);
	}

	@Override
	public int[] findOrigin() throws InterruptedException, BadLocationException {
		if (context == null) return null;
		AbstractDocument doc = (AbstractDocument) context.getDocument();
		doc.readLock();
		try {
			if (MatcherContext.isTaskCanceled()) return null;

			TokenHierarchy<?> th = TokenHierarchy.get(doc);
			if (th == null) return null;

			Language<HTMLTokenId> htmlLang = HTMLTokenId.language();
			TokenSequence<?> htmlTs = findHtmlSequence(th, context.getSearchOffset());
			if (htmlTs == null) return null;

			htmlTs.move(context.getSearchOffset());
			if (!htmlTs.moveNext() && !htmlTs.movePrevious()) return null;

			OriginInfo info = findOriginInHtmlSequence(htmlTs);
			if (info == null) return null;

			originStart = info.start;
			originEnd = info.end;
			tagName = info.tagName;
			isClosingTag = info.closing;
			isComment = info.comment;

			if (info.nameStart >= 0 && info.nameEnd >= 0 && info.closeStart >= 0 && info.closeEnd >= 0) {
				return new int[]{info.start, info.end, info.nameStart, info.nameEnd, info.closeStart, info.closeEnd};
			}
			return new int[]{info.start, info.end};
		} finally {
			doc.readUnlock();
		}
	}

	@Override
	public int[] findMatches() throws InterruptedException, BadLocationException {
		if (context == null || (tagName == null && !isComment)) return null;
		AbstractDocument doc = (AbstractDocument) context.getDocument();
		doc.readLock();
		try {
			if (MatcherContext.isTaskCanceled()) return null;

			TokenHierarchy<?> th = TokenHierarchy.get(doc);
			if (th == null) return null;

			Language<VTLTokenId> vtlLang = VTLTokenId.getLanguage();
			Language<HTMLTokenId> htmlLang = HTMLTokenId.language();

			TokenSequence<?> vtlTs = th.tokenSequence(vtlLang);
			if (vtlTs == null) return null;

			vtlTs.move(originStart);
			if (!vtlTs.moveNext() && !vtlTs.movePrevious()) return null;

			if (isComment) {
				return findCommentMatch(vtlTs, htmlLang);
			}

			if (!isClosingTag) {
				return findMatchingCloseTag(vtlTs, htmlLang);
			} else {
				return findMatchingOpenTag(vtlTs, htmlLang);
			}
		} finally {
			doc.readUnlock();
		}
	}

	private TokenSequence<?> findHtmlSequence(TokenHierarchy<?> th, int offset) {
		var sequences = th.embeddedTokenSequences(offset, false);
		Language<HTMLTokenId> htmlLang = HTMLTokenId.language();
		for (int i = sequences.size() - 1; i >= 0; i--) {
			if (sequences.get(i).language() == htmlLang) {
				return sequences.get(i);
			}
		}
		return null;
	}

	// ---- Origin detection ----

	private OriginInfo findOriginInHtmlSequence(TokenSequence<?> htmlTs) {
		Token<?> token = htmlTs.token();
		HTMLTokenId id = (HTMLTokenId) token.id();

		if (id == HTMLTokenId.BLOCK_COMMENT || id == HTMLTokenId.SGML_COMMENT) {
			return new OriginInfo(htmlTs.offset(), htmlTs.offset() + token.length(), null, false, true);
		}

		if (id == HTMLTokenId.TAG_OPEN_SYMBOL) {
			return handleTagOpenSymbol(htmlTs);
		}
		if (id == HTMLTokenId.TAG_OPEN || id == HTMLTokenId.TAG_CLOSE) {
			return handleTagName(htmlTs);
		}
		if (id == HTMLTokenId.TAG_CLOSE_SYMBOL) {
			return handleTagCloseSymbol(htmlTs);
		}
		if (isTagInternalToken(id)) {
			return expandToTag(htmlTs);
		}
		return null;
	}

	private OriginInfo handleTagOpenSymbol(TokenSequence<?> htmlTs) {
		String symbolText = htmlTs.token().text().toString();
		boolean closing = symbolText.length() > 1 && symbolText.charAt(1) == '/';
		int tagStart = htmlTs.offset();

		String name = null;
		int nameStart = -1;
		int nameEndVal = -1;

		while (htmlTs.moveNext()) {
			Token<?> t = htmlTs.token();
			HTMLTokenId tid = (HTMLTokenId) t.id();
			if (tid == HTMLTokenId.TAG_OPEN || tid == HTMLTokenId.TAG_CLOSE) {
				name = t.text().toString().toLowerCase();
				if (tid == HTMLTokenId.TAG_CLOSE && name.startsWith("/")) name = name.substring(1);
				nameStart = htmlTs.offset();
				nameEndVal = nameStart + t.length();
				break;
			} else if (tid == HTMLTokenId.TAG_CLOSE_SYMBOL || tid == HTMLTokenId.EOL) {
				break;
			}
		}

		if (name == null) return null;
		if (VOID_ELEMENTS.contains(name)) return null;

		int closeStart = -1;
		int closeEnd = -1;
		while (htmlTs.moveNext()) {
			if (((HTMLTokenId) htmlTs.token().id()) == HTMLTokenId.TAG_CLOSE_SYMBOL) {
				closeStart = htmlTs.offset();
				closeEnd = closeStart + htmlTs.token().length();
				break;
			}
		}

		if (closeStart < 0) {
			return new OriginInfo(tagStart, htmlTs.offset() + (htmlTs.token() != null ? htmlTs.token().length() : 0), name, closing, false);
		}
		return new OriginInfo(tagStart, closeEnd, nameStart, nameEndVal, closeStart, closeEnd, name, closing, false);
	}

	private OriginInfo handleTagName(TokenSequence<?> htmlTs) {
		Token<?> nameToken = htmlTs.token();
		boolean closing = ((HTMLTokenId) nameToken.id()) == HTMLTokenId.TAG_CLOSE;
		String name = nameToken.text().toString().toLowerCase();
		if (closing && name.startsWith("/")) name = name.substring(1);
		if (VOID_ELEMENTS.contains(name)) return null;

		int nameStart = htmlTs.offset();
		int nameEnd = nameStart + name.length();
		int tagStart = -1;

		while (htmlTs.movePrevious()) {
			HTMLTokenId tid = (HTMLTokenId) htmlTs.token().id();
			if (tid == HTMLTokenId.TAG_OPEN_SYMBOL) { tagStart = htmlTs.offset(); break; }
			if (tid == HTMLTokenId.TAG_CLOSE_SYMBOL) break;
		}
		if (tagStart < 0) tagStart = nameStart;

		htmlTs.move(nameStart);
		htmlTs.moveNext();

		int closeStart = -1;
		int closeEnd = -1;
		boolean selfClosing = false;
		while (htmlTs.moveNext()) {
			if (((HTMLTokenId) htmlTs.token().id()) == HTMLTokenId.TAG_CLOSE_SYMBOL) {
				selfClosing = htmlTs.token().text().toString().equals("/>");
				closeStart = htmlTs.offset();
				closeEnd = closeStart + htmlTs.token().length();
				break;
			}
		}
		if (selfClosing) return null;

		int tagEnd = closeEnd > 0 ? closeEnd : nameEnd;
		if (closeStart >= 0 && closeEnd >= 0) {
			return new OriginInfo(tagStart, tagEnd, nameStart, nameEnd, closeStart, closeEnd, name, closing, false);
		}
		return new OriginInfo(tagStart, tagEnd, name, closing, false);
	}

	private OriginInfo handleTagCloseSymbol(TokenSequence<?> htmlTs) {
		int closeStart = htmlTs.offset();
		int closeEnd = closeStart + htmlTs.token().length();
		String name = null;
		int nameStart = -1;
		int nameEndVal = -1;
		boolean closing = false;

		while (htmlTs.movePrevious()) {
			HTMLTokenId tid = (HTMLTokenId) htmlTs.token().id();
			if (tid == HTMLTokenId.TAG_OPEN || tid == HTMLTokenId.TAG_CLOSE) {
				name = htmlTs.token().text().toString().toLowerCase();
				if (tid == HTMLTokenId.TAG_CLOSE && name.startsWith("/")) name = name.substring(1);
				closing = (tid == HTMLTokenId.TAG_CLOSE) || closing;
				nameStart = htmlTs.offset();
				nameEndVal = nameStart + htmlTs.token().length();
			} else if (tid == HTMLTokenId.TAG_OPEN_SYMBOL) {
				String sym = htmlTs.token().text().toString();
				closing = sym.length() > 1 && sym.charAt(1) == '/';
				if (name != null && !VOID_ELEMENTS.contains(name)) {
					return new OriginInfo(htmlTs.offset(), closeEnd, nameStart, nameEndVal, closeStart, closeEnd, name, closing, false);
				}
				return null;
			}
		}
		if (name == null || VOID_ELEMENTS.contains(name)) return null;
		return new OriginInfo(nameStart, closeEnd, name, closing, false);
	}

	private OriginInfo expandToTag(TokenSequence<?> htmlTs) {
		int savedOffset = htmlTs.offset();
		while (htmlTs.movePrevious()) {
			HTMLTokenId tid = (HTMLTokenId) htmlTs.token().id();
			if (tid == HTMLTokenId.TAG_OPEN_SYMBOL) return handleTagOpenSymbol(htmlTs);
			if (tid == HTMLTokenId.TAG_CLOSE_SYMBOL) break;
		}
		htmlTs.move(savedOffset);
		while (htmlTs.moveNext()) {
			HTMLTokenId tid = (HTMLTokenId) htmlTs.token().id();
			if (tid == HTMLTokenId.TAG_CLOSE_SYMBOL) { htmlTs.movePrevious(); return findOriginInHtmlSequence(htmlTs); }
		}
		return null;
	}

	private boolean isTagInternalToken(HTMLTokenId id) {
		return id == HTMLTokenId.WS || id == HTMLTokenId.ARGUMENT || id == HTMLTokenId.VALUE
			|| id == HTMLTokenId.OPERATOR || id == HTMLTokenId.EOL;
	}

	// ---- Match finding ----

	private int[] findCommentMatch(TokenSequence<?> vtlTs, Language<HTMLTokenId> htmlLang) {
		if (!isClosingTag) {
			// Scan origin TEXT token first, then subsequent ones
			TokenSequence<?> originHtml = vtlTs.embedded(htmlLang);
			if (originHtml != null) {
				originHtml.moveStart();
				while (originHtml.moveNext()) {
					if (originHtml.token().id() == HTMLTokenId.BLOCK_COMMENT
							&& originHtml.offset() != originStart) {
						return new int[]{originHtml.offset(), originHtml.offset() + originHtml.token().length()};
					}
				}
			}
			while (vtlTs.moveNext()) {
				if (MatcherContext.isTaskCanceled()) return null;
				Token<?> vtlToken = vtlTs.token();
				if (!isTextToken(vtlToken)) continue;
				TokenSequence<?> htmlSeq = vtlTs.embedded(htmlLang);
				if (htmlSeq == null) continue;
				htmlSeq.moveStart();
				while (htmlSeq.moveNext()) {
					if (htmlSeq.token().id() == HTMLTokenId.BLOCK_COMMENT) {
						return new int[]{htmlSeq.offset(), htmlSeq.offset() + htmlSeq.token().length()};
					}
				}
			}
		} else {
			// Scan origin TEXT token first (backward), then preceding ones
			TokenSequence<?> originHtml = vtlTs.embedded(htmlLang);
			if (originHtml != null) {
				originHtml.moveEnd();
				while (originHtml.movePrevious()) {
					if (originHtml.token().id() == HTMLTokenId.BLOCK_COMMENT
							&& originHtml.offset() != originStart) {
						return new int[]{originHtml.offset(), originHtml.offset() + originHtml.token().length()};
					}
				}
			}
			while (vtlTs.movePrevious()) {
				if (MatcherContext.isTaskCanceled()) return null;
				Token<?> vtlToken = vtlTs.token();
				if (!isTextToken(vtlToken)) continue;
				TokenSequence<?> htmlSeq = vtlTs.embedded(htmlLang);
				if (htmlSeq == null) continue;
				htmlSeq.moveEnd();
				while (htmlSeq.movePrevious()) {
					if (htmlSeq.token().id() == HTMLTokenId.BLOCK_COMMENT) {
						return new int[]{htmlSeq.offset(), htmlSeq.offset() + htmlSeq.token().length()};
					}
				}
			}
		}
		return null;
	}

	private int[] findMatchingCloseTag(TokenSequence<?> vtlTs, Language<HTMLTokenId> htmlLang) {
		int level = 1;

		// First: scan the origin TEXT token, starting AFTER the origin tag
		TokenSequence<?> originHtml = vtlTs.embedded(htmlLang);
		if (originHtml != null) {
			// Position right after the origin tag's closing '>'
			originHtml.move(originEnd);
			while (originHtml.moveNext()) {
				if (MatcherContext.isTaskCanceled()) return null;
				int[] result = checkTagForward(originHtml, tagName, level);
				if (result != null) return result;
				level = updateLevelForward(originHtml, tagName, level);
			}
		}

		// Then: scan subsequent TEXT tokens
		while (vtlTs.moveNext()) {
			if (MatcherContext.isTaskCanceled()) return null;
			Token<?> vtlToken = vtlTs.token();
			if (!isTextToken(vtlToken)) continue;

			TokenSequence<?> htmlSeq = vtlTs.embedded(htmlLang);
			if (htmlSeq == null) continue;

			htmlSeq.moveStart();
			while (htmlSeq.moveNext()) {
				if (MatcherContext.isTaskCanceled()) return null;
				int[] result = checkTagForward(htmlSeq, tagName, level);
				if (result != null) return result;
				level = updateLevelForward(htmlSeq, tagName, level);
			}
		}
		return null;
	}

	private int[] findMatchingOpenTag(TokenSequence<?> vtlTs, Language<HTMLTokenId> htmlLang) {
		int level = 1;

		// First: scan the origin TEXT token, starting BEFORE the origin tag
		TokenSequence<?> originHtml = vtlTs.embedded(htmlLang);
		if (originHtml != null) {
			// Position right before the origin tag's opening '<'
			originHtml.move(originStart);
			while (originHtml.movePrevious()) {
				if (MatcherContext.isTaskCanceled()) return null;
				int[] result = checkTagBackward(originHtml, tagName, level);
				if (result != null) return result;
				level = updateLevelBackward(originHtml, tagName, level);
			}
		}

		// Then: scan preceding TEXT tokens
		while (vtlTs.movePrevious()) {
			if (MatcherContext.isTaskCanceled()) return null;
			Token<?> vtlToken = vtlTs.token();
			if (!isTextToken(vtlToken)) continue;

			TokenSequence<?> htmlSeq = vtlTs.embedded(htmlLang);
			if (htmlSeq == null) continue;

			htmlSeq.moveEnd();
			while (htmlSeq.movePrevious()) {
				if (MatcherContext.isTaskCanceled()) return null;
				int[] result = checkTagBackward(htmlSeq, tagName, level);
				if (result != null) return result;
				level = updateLevelBackward(htmlSeq, tagName, level);
			}
		}
		return null;
	}

	private int[] checkTagForward(TokenSequence<?> htmlTs, String target, int level) {
		Token<?> token = htmlTs.token();
		HTMLTokenId id = (HTMLTokenId) token.id();
		if (id == HTMLTokenId.TAG_CLOSE) {
			String name = token.text().toString().toLowerCase();
			if (name.startsWith("/")) name = name.substring(1);
			if (name.equals(target) && level == 1) {
				return buildTagSpan(htmlTs, true);
			}
		}
		return null;
	}

	private int[] checkTagBackward(TokenSequence<?> htmlTs, String target, int level) {
		Token<?> token = htmlTs.token();
		HTMLTokenId id = (HTMLTokenId) token.id();
		if (id == HTMLTokenId.TAG_OPEN) {
			String name = token.text().toString().toLowerCase();
			if (name.equals(target) && level == 1) {
				return buildTagSpan(htmlTs, false);
			}
		}
		return null;
	}

	private int updateLevelForward(TokenSequence<?> htmlTs, String target, int level) {
		Token<?> token = htmlTs.token();
		HTMLTokenId id = (HTMLTokenId) token.id();
		if (id == HTMLTokenId.TAG_OPEN) {
			String name = token.text().toString().toLowerCase();
			if (name.equals(target) && !isNextSelfClosing(htmlTs)) {
				level++;
			}
		} else if (id == HTMLTokenId.TAG_CLOSE) {
			String name = token.text().toString().toLowerCase();
			if (name.startsWith("/")) name = name.substring(1);
			if (name.equals(target)) {
				level--;
			}
		}
		return level;
	}

	private int updateLevelBackward(TokenSequence<?> htmlTs, String target, int level) {
		Token<?> token = htmlTs.token();
		HTMLTokenId id = (HTMLTokenId) token.id();
		if (id == HTMLTokenId.TAG_CLOSE) {
			String name = token.text().toString().toLowerCase();
			if (name.startsWith("/")) name = name.substring(1);
			if (name.equals(target)) level++;
		} else if (id == HTMLTokenId.TAG_OPEN) {
			String name = token.text().toString().toLowerCase();
			if (name.equals(target) && !isNextSelfClosing(htmlTs)) {
				level--;
			}
		}
		return level;
	}

	private boolean isNextSelfClosing(TokenSequence<?> htmlTs) {
		int saveIdx = htmlTs.index();
		boolean selfClosing = false;
		while (htmlTs.moveNext()) {
			HTMLTokenId id = (HTMLTokenId) htmlTs.token().id();
			if (id == HTMLTokenId.TAG_CLOSE_SYMBOL) {
				selfClosing = htmlTs.token().text().toString().equals("/>");
				break;
			}
			if (id == HTMLTokenId.TAG_OPEN_SYMBOL || id == HTMLTokenId.TAG_OPEN || id == HTMLTokenId.TAG_CLOSE) break;
		}
		htmlTs.moveIndex(saveIdx);
		htmlTs.moveNext();
		return selfClosing;
	}

	private int[] buildTagSpan(TokenSequence<?> htmlTs, boolean fromCloseName) {
		int nameStart = htmlTs.offset();
		String detectedName = htmlTs.token().text().toString().toLowerCase();
		if (fromCloseName && detectedName.startsWith("/")) detectedName = detectedName.substring(1);
		int nameEnd = nameStart + detectedName.length();

		int tagStart = -1;
		int saveIdx = htmlTs.index();
		while (htmlTs.movePrevious()) {
			if (((HTMLTokenId) htmlTs.token().id()) == HTMLTokenId.TAG_OPEN_SYMBOL) {
				tagStart = htmlTs.offset();
				break;
			}
		}
		if (tagStart < 0) tagStart = nameStart;

		htmlTs.moveIndex(saveIdx);
		htmlTs.moveNext();

		int closeStart = -1;
		int closeEnd = -1;
		while (htmlTs.moveNext()) {
			if (((HTMLTokenId) htmlTs.token().id()) == HTMLTokenId.TAG_CLOSE_SYMBOL) {
				closeStart = htmlTs.offset();
				closeEnd = closeStart + htmlTs.token().length();
				break;
			}
		}

		if (closeStart >= 0 && closeEnd >= 0) {
			return new int[]{tagStart, closeEnd, nameStart, nameEnd, closeStart, closeEnd};
		}
		return new int[]{tagStart, closeEnd > 0 ? closeEnd : nameEnd};
	}

	private boolean isTextToken(Token<?> token) {
		return token.id() instanceof VTLTokenId && "TEXT".equals(((VTLTokenId) token.id()).name());
	}

	static final class OriginInfo {
		final int start;
		final int end;
		final int nameStart;
		final int nameEnd;
		final int closeStart;
		final int closeEnd;
		final String tagName;
		final boolean closing;
		final boolean comment;

		OriginInfo(int start, int end, String tagName, boolean closing, boolean comment) {
			this(start, end, -1, -1, -1, -1, tagName, closing, comment);
		}

		OriginInfo(int start, int end, int nameStart, int nameEnd, int closeStart, int closeEnd, String tagName, boolean closing, boolean comment) {
			this.start = start;
			this.end = end;
			this.nameStart = nameStart;
			this.nameEnd = nameEnd;
			this.closeStart = closeStart;
			this.closeEnd = closeEnd;
			this.tagName = tagName;
			this.closing = closing;
			this.comment = comment;
		}
	}

	static boolean isVoidElement(String name) {
		return VOID_ELEMENTS.contains(name.toLowerCase());
	}
}