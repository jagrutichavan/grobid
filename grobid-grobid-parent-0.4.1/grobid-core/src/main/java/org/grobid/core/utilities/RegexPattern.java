package org.grobid.core.utilities;

public class RegexPattern {
    public static String ANYTHING = "(.*)";
    public static String ALPHABET = "[A-z]";
    public static String STARTS_WITH = "^";
    public static String ENDS_WITH = "$";
    public static String SPACE = " ";
    public static String NUMBER = "[0-9]";
    public static String PLUS = "+";
    public static String OR = "|";
    public static String PERIOD = "\\.";
    public static String COLON = ":";
    public static String OPENING_BRACKET = "(";
    public static String CLOSING_BRACKET = ")";
    public static String BACKWARD_SLASH = "\\";
    public static String QUESTION_MARK = "?";
    public static String OPENING_SQUARE_BRACKET = "[";
    public static String CLOSING_SQUARE_BRACKET = "]";
    public static String NOT_ALPHA_NUMERIC = "^[^0-9a-zA-Z,;]*$";
    public static String CHARACTER_GROUP = "[%s]";
    public static String COMMA = ",";
    public static String SEMICOLON = ";";
    public static String N_DASH = "–";
    public static String EM_DASH = "—";
    public static String HYPHEN_MINUS = "-";
    public static String SINGLE_STRAIGHT_QUOTE = "'";
    public static String SINGLE_OPENING_QUOTE = "‘";
    public static String SINGLE_CLOSING_QUOTE = "’";
}
