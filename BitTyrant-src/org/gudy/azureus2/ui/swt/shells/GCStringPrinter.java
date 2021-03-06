/*
 * File    : GCStringPrinter.java
 * Created : 16 mars 2004
 * By      : Olivier
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.ui.swt.shells;

import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Olivier Chalouhi
 *
 */
public class GCStringPrinter
{

	public static boolean printString(GC gc, String string, Rectangle printArea) {
		return printString(gc, string, printArea, false, false);
	}

	public static boolean printString(GC gc, String string, Rectangle printArea,
			boolean skipClip, boolean fullLinesOnly)
	{
		return printString(gc, string, printArea, skipClip, fullLinesOnly, SWT.WRAP
				| SWT.TOP);
	}

	public static boolean printString(GC gc, String string, Rectangle printArea,
			boolean skipClip, boolean fullLinesOnly, int flags)
	{
		try {
			return _printString(gc, string, printArea, skipClip, fullLinesOnly, flags);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	// 1) Doesn't handle ".." right when SWT.WRAP mode with multiple lines, 
	// and lines overflow printArea.  In that case it may chop off the last 
	// displayed word plus the 2 last characters of the previous word and append
	// a ".."
	// 2) Doesn't properly vertically center multi-line
	private static boolean _printString(GC gc, String string,
			Rectangle printArea, boolean skipClip, boolean fullLinesOnly, int flags)
	{
		if (printArea.isEmpty()) {
			return false;
		}

		boolean wrap = (flags & SWT.WRAP) > 0;

		Rectangle rectDraw = new Rectangle(printArea.x, printArea.y,
				printArea.width, printArea.height);
		int height = 0;

		Rectangle oldClipping = null;
		try {
			if (!skipClip) {
				oldClipping = gc.getClipping();

				// Protect the GC from drawing outside the drawing area
				gc.setClipping(printArea);
			}

			//We need to add some cariage return ...
			String sTabsReplaced = string.replaceAll("\t", "  ");

			StringBuffer outputLine = new StringBuffer();

			// Process string line by line
			StringTokenizer stLine = new StringTokenizer(sTabsReplaced, "\n");
			while (stLine.hasMoreElements()) {
				int iLineHeight = 0;
				String sLine = stLine.nextToken();
				if (gc.stringExtent(sLine).x > printArea.width) {
					//System.out.println("Line: "+ sLine);
					StringTokenizer stWord = new StringTokenizer(sLine, " ");
					String space = "";
					int iLineLength = 0;
					iLineHeight = gc.stringExtent(" ").y;

					// Process line word by word
					while (stWord.hasMoreElements()) {
						String word = stWord.nextToken();
						if (!wrap) {
							word = sLine;
						}

						// check if word is longer than our print area, and split it
						Point ptWordSize = gc.stringExtent(word + " ");
						while (ptWordSize.x > printArea.width && word.length() > 1) {
							int endIndex = word.length() - 1;
							do {
								endIndex--;
								ptWordSize = gc.stringExtent(word.substring(0, endIndex) + " ");
							} while (endIndex > 3
									&& ptWordSize.x + iLineLength > printArea.width);
							// append part that will fit
							height += ptWordSize.y;
							if (fullLinesOnly && height > printArea.height) {
								return false;
							}
							if (outputLine.length() > 0) {
								outputLine.append(space);
							}
							outputLine.append(word.substring(0, endIndex));
							if (!wrap) {
								outputLine.replace(outputLine.length() - 1,
										outputLine.length(), "..");
							}
							drawLine(gc, outputLine, flags, rectDraw);
							outputLine.setLength(0);

							if (!wrap) {
								return false;
							}

							// setup word as the remaining part that didn't fit
							word = word.substring(endIndex);
							ptWordSize = gc.stringExtent(word + " ");
							iLineLength = 0;
						}

						iLineLength += ptWordSize.x;
						//System.out.println(outputLine + " : " + word + " : " + iLineLength);
						if (iLineLength > printArea.width) {
							iLineLength = ptWordSize.x;
							height += iLineHeight;
							iLineHeight = ptWordSize.y;
							if (fullLinesOnly && height > printArea.height) {
								return false;
							}

							space = "";
							if (!wrap) {
								outputLine.replace(outputLine.length() - 1,
										outputLine.length(), "..");
							}
							drawLine(gc, outputLine, flags, rectDraw);
							outputLine.setLength(0);
							if (!wrap) {
								return false;
							}
						}

						if (outputLine.length() > 0) {
							outputLine.append(space);
						}
						outputLine.append(word);
						space = " ";
					}

				} else {
					outputLine.append(sLine);
					iLineHeight = gc.stringExtent(sLine).y;
				}

				height += iLineHeight;
				if (fullLinesOnly && height > printArea.height) {
					return false;
				}

				if (!wrap && stLine.hasMoreElements()) {
					outputLine.replace(outputLine.length() - 1, outputLine.length(), "..");
				}
				drawLine(gc, outputLine, flags, rectDraw);
				outputLine.setLength(0);
				if (!wrap) {
					return stLine.hasMoreElements();
				}
			}
		} finally {
			if (!skipClip) {
				gc.setClipping(oldClipping);
			}
		}

		return height <= printArea.height;
	}

	/**
	 * @param gc
	 * @param outputLine
	 * @param flags
	 * @param printArea
	 */
	private static void drawLine(GC gc, StringBuffer outputLine, int flags,
			Rectangle printArea)
	{
		String sOutputLine = outputLine.toString();
		Point drawSize = gc.textExtent(sOutputLine);
		int x0;
		if ((flags & SWT.RIGHT) > 0) {
			x0 = printArea.x + printArea.width - drawSize.x;
		} else if ((flags & SWT.CENTER) > 0) {
			x0 = printArea.x + (printArea.width - drawSize.x) / 2;
		} else {
			x0 = printArea.x;
		}

		int y0;
		if ((flags & (SWT.TOP | SWT.BOTTOM)) == 0) {
			// center vert
			y0 = printArea.y + (printArea.height - drawSize.y) / 2;
		} else {
			y0 = printArea.y;
		}

		gc.drawText(sOutputLine, x0, y0, true);
		printArea.y += drawSize.y;
	}

	private static int getAdvanceWidth(GC gc, String s) {
		int result = 0;
		for (int i = 0; i < s.length(); i++) {
			result += gc.getAdvanceWidth(s.charAt(i)) - 1;
		}
		return result;
	}

	public static void main(String[] args) {
		Display display = Display.getDefault();
		final Shell shell = new Shell(display, SWT.SHELL_TRIM);

		shell.setSize(500, 500);

		shell.addListener(SWT.Paint, new Listener() {
			public void handleEvent(Event event) {
				int x = 0;
				int y = 0;

				GC gc = new GC(shell);

				printString(
						gc,
						"This is a test of the string printer averlongwordthisisyesindeed you rule",
						new Rectangle(x, y, 100, 19), true, true, SWT.RIGHT | SWT.WRAP);

				x += 110;
				printString(
						gc,
						"This is a test of the string printer averlongwordthisisyesindeed you rule",
						new Rectangle(x, y, 100, 19), true, false, SWT.LEFT | SWT.WRAP);

				x += 110;
				printString(
						gc,
						"This is a test of the string printer averlongwordthisisyesindeed you rule",
						new Rectangle(x, y, 100, 19), true, false, SWT.CENTER | SWT.WRAP);

				x = 0;
				y += 50;
				printString(
						gc,
						"FLO is a test of the string printer averlongwordthisisyesindeed you rule",
						new Rectangle(x, y, 100, 50), true, true, SWT.RIGHT | SWT.WRAP);

				x += 110;
				printString(
						gc,
						"This is a test of the string printer averlongwordthisisyesindeed you rule",
						new Rectangle(x, y, 100, 50), true, false, SWT.LEFT | SWT.WRAP);

				x += 110;
				printString(
						gc,
						"This is a test of the string printer averlongwordthisisyesindeed you rule",
						new Rectangle(x, y, 100, 50), true, false, SWT.CENTER | SWT.WRAP);

				x = 0;
				y += 100;
				printString(
						gc,
						"This is a test of the string printer averlongwordthisisyesindeed you rule",
						new Rectangle(x, y, 100, 50), true, true, SWT.RIGHT);

				x += 110;
				printString(
						event.gc,
						"This is a test of the string printer averlongwordthisisyesindeed you rule",
						new Rectangle(x, y, 100, 50), true, false, SWT.LEFT);

				x += 110;
				printString(
						gc,
						"This is a test of the string printer averlongwordthisisyesindeed you rule",
						new Rectangle(x, y, 100, 50), true, false, SWT.CENTER);

				x = 0;
				y += 100;
				gc.drawRectangle(x, y, 100, 100);
				x += 1;
				y += 1;
				printString(gc, "Hello", new Rectangle(x, y, 98, 98), true, true,
						SWT.CENTER);

				x += 110;
				gc.drawRectangle(x, y, 100, 100);
				x += 1;
				y += 1;
				printString(gc, "Hello", new Rectangle(x, y, 98, 98), true, true,
						SWT.CENTER | SWT.TOP);

				x += 110;
				gc.drawRectangle(x, y, 100, 100);
				x += 1;
				y += 1;
				printString(gc, "Hello", new Rectangle(x, y, 98, 98), true, true,
						SWT.TOP);

				x += 110;
				gc.drawRectangle(x, y, 50, 50);
				x += 1;
				y += 1;
				printString(gc, "Hello There", new Rectangle(x, y, 48, 48), true, true,
						SWT.NONE);

				gc.dispose();
			}
		});

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
