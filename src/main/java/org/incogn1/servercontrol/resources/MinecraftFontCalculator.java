package org.incogn1.servercontrol.resources;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MinecraftFontCalculator {
    public enum CharacterDetails {
        A('A', 5),
        a('a', 5),
        B('B', 5),
        b('b', 5),
        C('C', 5),
        c('c', 5),
        D('D', 5),
        d('d', 5),
        E('E', 5),
        e('e', 5),
        F('F', 5),
        f('f', 4),
        G('G', 5),
        g('g', 5),
        H('H', 5),
        h('h', 5),
        I('I', 3),
        i('i', 1),
        J('J', 5),
        j('j', 5),
        K('K', 5),
        k('k', 4),
        L('L', 5),
        l('l', 2),
        M('M', 5),
        m('m', 5),
        N('N', 5),
        n('n', 5),
        O('O', 5),
        o('o', 5),
        P('P', 5),
        p('p', 5),
        Q('Q', 5),
        q('q', 5),
        R('R', 5),
        r('r', 5),
        S('S', 5),
        s('s', 5),
        T('T', 5),
        t('t', 3),
        U('U', 5),
        u('u', 5),
        V('V', 5),
        v('v', 5),
        W('W', 5),
        w('w', 5),
        X('X', 5),
        x('x', 5),
        Y('Y', 5),
        y('y', 5),
        Z('Z', 5),
        z('z', 5),
        NUM_1('1', 5),
        NUM_2('2', 5),
        NUM_3('3', 5),
        NUM_4('4', 5),
        NUM_5('5', 5),
        NUM_6('6', 5),
        NUM_7('7', 5),
        NUM_8('8', 5),
        NUM_9('9', 5),
        NUM_0('0', 5),
        EXCLAMATION_POINT('!', 1),
        AT_SYMBOL('@', 6),
        NUM_SIGN('#', 5),
        DOLLAR_SIGN('$', 5),
        PERCENT('%', 5),
        UP_ARROW('^', 5),
        AMPERSAND('&', 5),
        ASTERISK('*', 3),
        LEFT_PARENTHESIS('(', 3),
        RIGHT_PARENTHESIS(')', 3),
        MINUS('-', 5),
        UNDERSCORE('_', 5),
        PLUS_SIGN('+', 5),
        EQUALS_SIGN('=', 5),
        LEFT_CURL_BRACE('{', 3),
        RIGHT_CURL_BRACE('}', 3),
        LEFT_BRACKET('[', 3),
        RIGHT_BRACKET(']', 3),
        COLON(':', 1),
        SEMI_COLON(';', 1),
        DOUBLE_QUOTE('"', 3),
        SINGLE_QUOTE('\'', 1),
        LEFT_ARROW('<', 4),
        RIGHT_ARROW('>', 4),
        QUESTION_MARK('?', 5),
        SLASH('/', 5),
        BACK_SLASH('\\', 5),
        LINE('|', 1),
        TILDE('~', 6),
        TICK('`', 2),
        PERIOD('.', 1),
        COMMA(',', 1),
        SPACE(' ', 3);

        private final char character;
        private final int width;

        CharacterDetails(char character, int width) {
            this.character = character;
            this.width = width + 1; // Spacing between characters is 1px
        }

        public char getCharacter() {
            return this.character;
        }

        public int getWidth() {
            return this.width;
        }

        public static int getCharacterWidth(char character) {
            for (CharacterDetails entry : values()) {
                if (entry.character == character) {
                    return entry.width;
                }
            }
            return 4 + 1; // Average + spacing
        }

        public static int getWordWidth(String word) {
            int width = 0;
            for (char character : word.toCharArray()) {
                width += getCharacterWidth(character);
            }
            return width;
        }
    }

    /**
     * Splits the given string into multiple parts based on
     * the specified line width, by breaking the string into
     * pieces at the location of whitespace characters.
     * Calculation is based on the length of characters in
     * the default Minecraft font.
     * <p>
     * Note: The input String is split using the regex "\\s+"
     * meaning that any whitespace characters (spaces, newlines,
     * etc.) will all be discarded and seen within this method
     * as a single 'space' character.
     *
     * @param input the original non-split text
     * @param maxLineWidth the maximum width a single line may be
     * @return an Array of the original input split into
     *      parts with a maximum length of the given maxLineWidth.
     */
    public static String[] splitTextByWidth(String input, int maxLineWidth) {
        return splitTextByWidth(input, maxLineWidth, 0);
    }

    /**
     * Splits the given string into multiple parts based on
     * the specified line width, by breaking the string into
     * pieces at the location of whitespace characters.
     * Calculation is based on the length of characters in
     * the default Minecraft font.
     * <p>
     * Note: The input String is split using the regex "\\s+"
     * meaning that any whitespace characters (spaces, newlines,
     * etc.) will all be discarded and seen within this method
     * as a single 'space' character.
     *
     * @param input the original non-split text
     * @param maxLineWidth the maximum width a single line may be
     * @param startOffset the initial offset to use in calculating
     *      the length of the first line. May be used when the
     *      result of this method is appended after an already
     *      existing piece of text.
     * @return an Array of the original input split into
     *      parts with a maximum length of the given maxLineWidth.
     */
    public static String[] splitTextByWidth(String input, int maxLineWidth, int startOffset) {
        List<String> result = new ArrayList<>();

        String[] words = input.split("\\s+");

        List<String> currentLine = new ArrayList<>();
        int currentLineWidth = startOffset;
        int spaceWidth = CharacterDetails.SPACE.getWidth();
        for (String word : words) {
            int wordWidth = CharacterDetails.getWordWidth(word);

            // Edge case - Word itself doesn't fit on one line
            //  -> Break word itself into pieces
            if (wordWidth > maxLineWidth) {
                String[] splitWord = splitStringByWidth(word, maxLineWidth, (currentLineWidth > 0 ? currentLineWidth + spaceWidth : 0));

                // Add first part to currentLine
                currentLine.add(splitWord[0]);
                result.add(String.join(" ", currentLine));

                // Add full lines directly to result
                String[] fullLines = Arrays.copyOfRange(splitWord, 1, splitWord.length - 1);
                result.addAll(Arrays.asList(fullLines));

                // Create new line with remainder of word
                String remainder = splitWord[splitWord.length - 1];
                int remainderWidth = CharacterDetails.getWordWidth(remainder);
                currentLine = new ArrayList<>();
                currentLine.add(remainder);
                currentLineWidth = remainderWidth;

                continue;
            }

            // Word fits
            //  -> Add word to line and increase width
            if (currentLineWidth + spaceWidth + wordWidth <= maxLineWidth) {
                currentLine.add(word);
                currentLineWidth += spaceWidth + wordWidth;

                continue;
            }

            // Word doesn't fit
            //  -> Save line and create new line to add word
            result.add(String.join(" ", currentLine));
            currentLine = new ArrayList<>();
            currentLineWidth = wordWidth;
            currentLine.add(word);
        }

        // Add remainder of line to result
        if (!currentLine.isEmpty()) {
            result.add(String.join(" ", currentLine));
        }

        return result.toArray(new String[0]);
    }

    /**
     * Splits the given string into multiple parts based on
     * the specified line width by breaking the string into
     * pieces after any given character. Calculation is based
     * on the length of characters in the default Minecraft font.
     *
     * @param input the original non-split text
     * @param maxLineWidth the maximum width a single line may be
     * @return an Array of the original input split into
     *      parts with a maximum length of the given maxLineWidth.
     */
    public static String[] splitStringByWidth(String input, int maxLineWidth) {
        return splitStringByWidth(input, maxLineWidth, 0);
    }

    /**
     * Splits the given string into multiple parts based on
     * the specified line width by breaking the string into
     * pieces after any given character. Calculation is based
     * on the length of characters in the default Minecraft font.
     *
     * @param input the original non-split text
     * @param maxLineWidth the maximum width a single line may be
     * @param startOffset the initial offset to use in calculating
     *      the length of the first line. May be used when the
     *      result of this method is appended after an already
     *      existing piece of text.
     * @return an Array of the original input split into
     *      parts with a maximum length of the given maxLineWidth.
     */
    public static String[] splitStringByWidth(String input, int maxLineWidth, int startOffset) {
        List<String> result = new ArrayList<>();

        char[] characters = input.toCharArray();

        List<Character> currentLine = new ArrayList<>();
        int currentLineWidth = startOffset;
        for (char character : characters) {
            int characterWidth = CharacterDetails.getCharacterWidth(character);

            // Character fits
            //  -> Add character to line and increase width
            if (currentLineWidth + characterWidth <= maxLineWidth) {
                currentLine.add(character);
                currentLineWidth += characterWidth;

                continue;
            }

            // Character doesn't fit
            //  -> Save line and create new line to add character
            result.add(Joiner.on("").join(currentLine));
            currentLine = new ArrayList<>();
            currentLineWidth = characterWidth;
            currentLine.add(character);
        }

        // Add remainder of line to result
        if (!currentLine.isEmpty()) {
            result.add(Joiner.on("").join(currentLine));
        }

        return result.toArray(new String[0]);
    }
}
