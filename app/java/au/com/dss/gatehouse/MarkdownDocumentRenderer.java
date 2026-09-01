package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MarkdownDocumentRenderer — Native high-fidelity Markdown parser and document renderer.
 * Converts headings, tables, bold/italics, blockquotes, code blocks, lists, and dividers
 * into an elegant native Android layout tailored to Gatehouse theme tokens (Dark and Light themes).
 */
public class MarkdownDocumentRenderer {

    public static LinearLayout renderMarkdown(Context context, String markdown, float baseTextSizeSp, int themeId) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 0, 0, dp(context, 16));

        if (markdown == null || markdown.trim().isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("No document content available.");
            empty.setTextColor(0xFF94A3B8);
            empty.setTextSize(baseTextSizeSp);
            container.addView(empty);
            return container;
        }

        boolean isLight = (themeId == MainActivity.THEME_DAYLIGHT || themeId == MainActivity.THEME_DESERT_SAND);

        int colAccent = 0xFFFFD166;
        int colText = 0xFFE2E8F0;
        int colMuted = 0xFF94A3B8;
        int colCardBg = 0xFF0F172A;
        int colCardBorder = 0xFF334155;
        int colQuoteBar = 0xFFF59E0B;
        int colCodeBg = 0xFF0F172A;
        int colCodeText = 0xFF38BDF8;
        int colDivider = 0xFF334155;

        if (themeId == MainActivity.THEME_RED) {
            colAccent = 0xFFFF4444;
            colText = 0xFFFF8888;
            colMuted = 0xFFAA3333;
            colCardBg = 0xFF2A0808;
            colCardBorder = 0xFF4A1010;
            colQuoteBar = 0xFFFF2222;
            colCodeBg = 0xFF1E0606;
            colCodeText = 0xFFFF8888;
            colDivider = 0xFF3D1212;
        } else if (themeId == MainActivity.THEME_NVG) {
            colAccent = 0xFF00FF66;
            colText = 0xFF88FFAA;
            colMuted = 0xFF33AA55;
            colCardBg = 0xFF082210;
            colCardBorder = 0xFF0D4418;
            colQuoteBar = 0xFF00FF66;
            colCodeBg = 0xFF041E0A;
            colCodeText = 0xFF00FFCC;
            colDivider = 0xFF0A4418;
        } else if (themeId == MainActivity.THEME_VIOLET) {
            colAccent = 0xFFC084FC;
            colText = 0xFFE9D5FF;
            colMuted = 0xFFA855F7;
            colCardBg = 0xFF1E1B4B;
            colCardBorder = 0xFF351559;
            colQuoteBar = 0xFFA855F7;
            colCodeBg = 0xFF140822;
            colCodeText = 0xFFC084FC;
            colDivider = 0xFF351559;
        } else if (themeId == MainActivity.THEME_DAYLIGHT) {
            colAccent = 0xFFD97706;
            colText = 0xFF0F172A;
            colMuted = 0xFF475569;
            colCardBg = 0xFFFFFFFF;
            colCardBorder = 0xFFCBD5E1;
            colQuoteBar = 0xFFD97706;
            colCodeBg = 0xFFF1F5F9;
            colCodeText = 0xFF0284C7;
            colDivider = 0xFFCBD5E1;
        } else if (themeId == MainActivity.THEME_DESERT_SAND) {
            colAccent = 0xFFB45309;
            colText = 0xFF1C1917;
            colMuted = 0xFF57534E;
            colCardBg = 0xFFFFFDF9;
            colCardBorder = 0xFFD4C3AC;
            colQuoteBar = 0xFFB45309;
            colCodeBg = 0xFFF4ECE2;
            colCodeText = 0xFF0369A1;
            colDivider = 0xFFD4C3AC;
        }

        String[] lines = markdown.split("\r?\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                i++;
                continue;
            }

            // 1. Code Block (``` ... ```)
            if (trimmed.startsWith("```")) {
                StringBuilder codeSb = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("```")) {
                    codeSb.append(lines[i]).append("\n");
                    i++;
                }
                if (i < lines.length && lines[i].trim().startsWith("```")) {
                    i++;
                }
                container.addView(createCodeBlockView(context, codeSb.toString().trim(), baseTextSizeSp, colCodeBg, colCodeText, colCardBorder));
                continue;
            }

            // 2. Table Block (| ... |)
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                List<String> tableLines = new ArrayList<>();
                while (i < lines.length && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i].trim());
                    i++;
                }
                container.addView(createTableView(context, tableLines, baseTextSizeSp, colAccent, colText, colCardBg, colCardBorder, isLight));
                continue;
            }

            // 3. Headings (# H1, ## H2, ### H3, #### H4)
            if (trimmed.startsWith("#")) {
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                    level++;
                }
                String headingText = trimmed.substring(level).trim();
                container.addView(createHeadingView(context, headingText, level, baseTextSizeSp, colAccent, colText, colDivider, isLight));
                i++;
                continue;
            }

            // 4. Horizontal Rule (---, ***, ___)
            if (trimmed.equals("---") || trimmed.equals("***") || trimmed.equals("___")) {
                container.addView(createDividerView(context, colDivider));
                i++;
                continue;
            }

            // 5. Blockquote (> ...)
            if (trimmed.startsWith(">")) {
                StringBuilder quoteSb = new StringBuilder();
                while (i < lines.length && lines[i].trim().startsWith(">")) {
                    String qLine = lines[i].trim().substring(1).trim();
                    quoteSb.append(qLine).append(" ");
                    i++;
                }
                container.addView(createBlockquoteView(context, quoteSb.toString().trim(), baseTextSizeSp, colQuoteBar, colText, isLight));
                continue;
            }

            // 6. Bullet List (- item, * item, + item)
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                String itemText = trimmed.substring(2).trim();
                container.addView(createListItemView(context, "•", itemText, baseTextSizeSp, colAccent, colText, isLight));
                i++;
                continue;
            }

            // 7. Numbered List (1. item, 2. item)
            Pattern numPattern = Pattern.compile("^(\\d+\\.)\\s+(.*)");
            Matcher numMatcher = numPattern.matcher(trimmed);
            if (numMatcher.matches()) {
                String num = numMatcher.group(1);
                String itemText = numMatcher.group(2);
                container.addView(createListItemView(context, num, itemText, baseTextSizeSp, colAccent, colText, isLight));
                i++;
                continue;
            }

            // 8. Standard Paragraph
            StringBuilder paraSb = new StringBuilder(trimmed);
            i++;
            while (i < lines.length) {
                String nextTrimmed = lines[i].trim();
                if (nextTrimmed.isEmpty() || nextTrimmed.startsWith("#") || nextTrimmed.startsWith("|")
                        || nextTrimmed.startsWith("```") || nextTrimmed.startsWith(">")
                        || nextTrimmed.startsWith("- ") || nextTrimmed.startsWith("* ")
                        || nextTrimmed.startsWith("+ ") || nextTrimmed.matches("^\\d+\\..*")
                        || nextTrimmed.equals("---")) {
                    break;
                }
                paraSb.append(" ").append(nextTrimmed);
                i++;
            }
            container.addView(createParagraphView(context, paraSb.toString(), baseTextSizeSp, colText, colAccent, isLight));
        }

        return container;
    }

    private static View createHeadingView(Context context, String text, int level, float baseSp, int accentColor, int textColor, int dividerColor, boolean isLight) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView tv = new TextView(context);
        tv.setText(formatInlineMarkdown(text, accentColor, textColor, isLight));
        tv.setTypeface(Typeface.DEFAULT_BOLD);

        if (level == 1) {
            tv.setTextSize(baseSp + 5f);
            tv.setTextColor(accentColor);
            rlp.topMargin = dp(context, 16);
            rlp.bottomMargin = dp(context, 8);
        } else if (level == 2) {
            tv.setTextSize(baseSp + 3f);
            tv.setTextColor(isLight ? 0xFF0284C7 : 0xFF38BDF8);
            rlp.topMargin = dp(context, 14);
            rlp.bottomMargin = dp(context, 6);
        } else if (level == 3) {
            tv.setTextSize(baseSp + 1.5f);
            tv.setTextColor(accentColor);
            rlp.topMargin = dp(context, 10);
            rlp.bottomMargin = dp(context, 4);
        } else {
            tv.setTextSize(baseSp + 0.5f);
            tv.setTextColor(textColor);
            rlp.topMargin = dp(context, 8);
            rlp.bottomMargin = dp(context, 4);
        }

        row.setLayoutParams(rlp);
        row.addView(tv);

        if (level <= 2) {
            View bar = new View(context);
            bar.setBackgroundColor(dividerColor);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1));
            blp.topMargin = dp(context, 4);
            bar.setLayoutParams(blp);
            row.addView(bar);
        }

        return row;
    }

    private static View createParagraphView(Context context, String text, float baseSp, int textColor, int accentColor, boolean isLight) {
        TextView tv = new TextView(context);
        tv.setText(formatInlineMarkdown(text, accentColor, textColor, isLight));
        tv.setTextColor(textColor);
        tv.setTextSize(baseSp);
        tv.setLineSpacing(dp(context, 3), 1.25f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(context, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static View createBlockquoteView(Context context, String text, float baseSp, int barColor, int textColor, boolean isLight) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isLight ? 0x14D97706 : 0x1838BDF8);
        gd.setCornerRadius(dp(context, 6));
        box.setBackground(gd);
        box.setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(context, 6);
        lp.bottomMargin = dp(context, 10);
        box.setLayoutParams(lp);

        // Left Colored Bar
        View bar = new View(context);
        bar.setBackgroundColor(barColor);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(context, 3.5f), LinearLayout.LayoutParams.MATCH_PARENT);
        blp.rightMargin = dp(context, 10);
        bar.setLayoutParams(blp);
        box.addView(bar);

        TextView tv = new TextView(context);
        tv.setText(formatInlineMarkdown(text, barColor, textColor, isLight));
        tv.setTextColor(isLight ? 0xFF334155 : 0xFFCBD5E1);
        tv.setTextSize(baseSp - 0.5f);
        tv.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        tv.setLineSpacing(dp(context, 2), 1.2f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tlp);
        box.addView(tv);

        return box;
    }

    private static View createListItemView(Context context, String bullet, String text, float baseSp, int accentColor, int textColor, boolean isLight) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = dp(context, 4);
        rlp.leftMargin = dp(context, 4);
        row.setLayoutParams(rlp);

        TextView bTv = new TextView(context);
        bTv.setText(bullet);
        bTv.setTextColor(accentColor);
        bTv.setTextSize(baseSp);
        bTv.setTypeface(Typeface.DEFAULT_BOLD);
        bTv.setPadding(0, 0, dp(context, 8), 0);
        row.addView(bTv);

        TextView tv = new TextView(context);
        tv.setText(formatInlineMarkdown(text, accentColor, textColor, isLight));
        tv.setTextColor(textColor);
        tv.setTextSize(baseSp);
        tv.setLineSpacing(dp(context, 2), 1.2f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tlp);
        row.addView(tv);

        return row;
    }

    private static View createCodeBlockView(Context context, String code, float baseSp, int bg, int textCol, int borderCol) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bg);
        gd.setStroke(dp(context, 1), borderCol);
        gd.setCornerRadius(dp(context, 8));
        box.setBackground(gd);
        box.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(context, 6);
        lp.bottomMargin = dp(context, 10);
        box.setLayoutParams(lp);

        TextView tv = new TextView(context);
        tv.setText(code);
        tv.setTextColor(textCol);
        tv.setTextSize(Math.max(10f, baseSp - 1.5f));
        tv.setTypeface(Typeface.MONOSPACE);
        box.addView(tv);

        return box;
    }

    private static View createTableView(Context context, List<String> lines, float baseSp, int accentColor, int textColor, int cardBg, int borderCol, boolean isLight) {
        if (lines.isEmpty()) return new View(context);

        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(context, 8);
        hlp.bottomMargin = dp(context, 12);
        hsv.setLayoutParams(hlp);

        TableLayout table = new TableLayout(context);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(cardBg);
        gd.setStroke(dp(context, 1), borderCol);
        gd.setCornerRadius(dp(context, 8));
        table.setBackground(gd);
        table.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));

        boolean isHeader = true;
        for (String line : lines) {
            if (line.matches("^\\|[\\s\\-:\\|]+\\|$")) {
                // Skip Markdown separator line |---|---|
                isHeader = false;
                continue;
            }

            String[] rawCols = line.split("\\|");
            List<String> cells = new ArrayList<>();
            for (String c : rawCols) {
                String ct = c.trim();
                if (!ct.isEmpty() || c.equals(rawCols[0]) || c.equals(rawCols[rawCols.length - 1])) {
                    if (!c.equals(rawCols[0]) && !c.equals(rawCols[rawCols.length - 1])) {
                        cells.add(ct);
                    }
                }
            }

            if (cells.isEmpty()) continue;

            TableRow row = new TableRow(context);
            if (isHeader) {
                row.setBackgroundColor(isLight ? 0x1F0284C7 : 0x3338BDF8);
            }

            for (int colIdx = 0; colIdx < cells.size(); colIdx++) {
                String cellContent = cells.get(colIdx);
                TextView tv = new TextView(context);
                tv.setText(formatInlineMarkdown(cellContent, accentColor, textColor, isLight));
                tv.setTextSize(isHeader ? (baseSp - 0.5f) : (baseSp - 1f));
                tv.setTypeface(isHeader ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                tv.setTextColor(isHeader ? accentColor : textColor);
                tv.setPadding(dp(context, 8), dp(context, 6), dp(context, 8), dp(context, 6));
                tv.setGravity(Gravity.CENTER_VERTICAL);
                row.addView(tv);
            }

            table.addView(row);
            isHeader = false;
        }

        hsv.addView(table);
        return hsv;
    }

    private static View createDividerView(Context context, int dividerCol) {
        View v = new View(context);
        v.setBackgroundColor(dividerCol);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1));
        lp.topMargin = dp(context, 12);
        lp.bottomMargin = dp(context, 12);
        v.setLayoutParams(lp);
        return v;
    }

    public static CharSequence formatInlineMarkdown(String text, int accentColor, int textColor, boolean isLight) {
        if (text == null) return "";
        SpannableStringBuilder sb = new SpannableStringBuilder();

        // Regex for bold (**text** or __text__), italic (*text* or _text_), inline code (`text`)
        Pattern pattern = Pattern.compile("(\\*\\*\\*|___)(.*?)\\1|(\\*\\*|__)(.*?)\\3|(\\*)(.*?)\\5|(`)(.*?)\\7");
        Matcher matcher = pattern.matcher(text);

        int lastIndex = 0;
        while (matcher.find()) {
            sb.append(text.substring(lastIndex, matcher.start()));

            if (matcher.group(1) != null) { // Bold Italic
                String content = matcher.group(2);
                int start = sb.length();
                sb.append(content);
                sb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(accentColor), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(3) != null) { // Bold
                String content = matcher.group(4);
                int start = sb.length();
                sb.append(content);
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(textColor), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(5) != null) { // Italic
                String content = matcher.group(6);
                int start = sb.length();
                sb.append(content);
                sb.setSpan(new StyleSpan(Typeface.ITALIC), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (matcher.group(7) != null) { // Inline Code
                String content = matcher.group(8);
                int start = sb.length();
                sb.append(" ").append(content).append(" ");
                sb.setSpan(new TypefaceSpan("monospace"), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(isLight ? 0xFF0284C7 : 0xFF38BDF8), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new BackgroundColorSpan(isLight ? 0xFFE2E8F0 : 0xFF1E293B), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new RelativeSizeSpan(0.9f), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < text.length()) {
            sb.append(text.substring(lastIndex));
        }

        return sb;
    }

    private static int dp(Context context, float val) {
        return (int) (val * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
