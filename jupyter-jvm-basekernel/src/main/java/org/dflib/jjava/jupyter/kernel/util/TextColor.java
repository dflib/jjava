package org.dflib.jjava.jupyter.kernel.util;

/**
 * A collection of ANSI escapes that are supported by the Jupyter notebook.
 */
public enum TextColor {
    BLACK_FG(30),
    BOLD_BLACK_FG(1, 30),
    RED_FG(31),
    BOLD_RED_FG(1, 31),
    GREEN_FG(32),
    BOLD_GREEN_FG(1, 32),
    YELLOW_FG(33),
    BOLD_YELLOW_FG(1, 33),
    BLUE_FG(34),
    BOLD_BLUE_FG(1, 34),
    MAGENTA_FG(35),
    BOLD_MAGENTA_FG(1, 35),
    CYAN_FG(36),
    BOLD_CYAN_FG(1, 36),
    WHITE_FG(37),
    BOLD_WHITE_FG(1, 37),
    RESET_FG(39),

    BLACK_BG(40),
    RED_BG(41),
    GREEN_BG(42),
    YELLOW_BG(43),
    BLUE_BG(44),
    MAGENTA_BG(45),
    CYAN_BG(46),
    WHITE_BG(47),
    RESET_BG(49),

    BOLD(1),
    UNDERLINE(4),
    SWAP_FG_BG(7),
    RESET_ALL(0);

    private static String escape(int... codes) {
        StringBuilder sb = new StringBuilder("\033[");
        for (int i = 0; i < codes.length; i++) {
            if (i != codes.length - 1) sb.append(';');
            sb.append(codes[i]);
        }
        sb.append('m');

        return sb.toString();
    }

    private final String ansiEscape;

    TextColor(int... codes) {
        StringBuilder ansiEscape = new StringBuilder();
        for (int code : codes) ansiEscape.append(escape(code));
        this.ansiEscape = ansiEscape.toString();
    }

    public String getAnsiEscape() {
        return this.ansiEscape;
    }

    @Override
    public String toString() {
        return this.getAnsiEscape();
    }

    private static final int SET_FG_CODE = 38;
    private static final int SET_BG_CODE = 48;
    private static final int SET_COLOR_BY_MODE = 5;
    private static final int SET_COLOR_BY_RGB = 2;

    private static final int MAX_COLOR_MODE = 255;

    private static void assertInUByteRange(String name, int val) {
        if (val < 0 || val > MAX_COLOR_MODE)
            throw new IllegalArgumentException(String.format("%s value (%d) not in 0-255", name, val));
    }

    public static String fg(int colorMode) {
        assertInUByteRange("colorMode", colorMode);
        return escape(SET_FG_CODE, SET_COLOR_BY_MODE, colorMode);
    }

    public static String fg(int r, int g, int b) {
        assertInUByteRange("r", r);
        assertInUByteRange("g", g);
        assertInUByteRange("b", b);
        return escape(SET_FG_CODE, SET_COLOR_BY_RGB, r, g, b);
    }

    public static String bg(int colorMode) {
        assertInUByteRange("colorMode", colorMode);
        return escape(SET_BG_CODE, SET_COLOR_BY_MODE, colorMode);
    }

    public static String bg(int r, int g, int b) {
        assertInUByteRange("r", r);
        assertInUByteRange("g", g);
        assertInUByteRange("b", b);
        return escape(SET_BG_CODE, SET_COLOR_BY_RGB, r, g, b);
    }
}
