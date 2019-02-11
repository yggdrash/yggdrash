package io.yggdrash.common.util;


public class AnsiColorUtil {
    public static void displayMessage(Color color, String message) {
        System.out.print(color.toValue() + message + Color.RESET.toValue());
    }

    public static void displayMessageLN(Color color, String message) {
        System.out.println(color.toValue() + message + Color.RESET.toValue());
    }


    public enum Color {
        BLACK("\u001B[30m"),
        RED("\u001B[31m"),
        GREEN("\u001B[32m"),
        YELLOW("\u001B[33m"),
        BLUE("\u001B[34m"),
        PINK("\u001B[35m"),
        BLUISH_GREEN("\u001B[36m"),
        WHITE("\u001B[37m"),
        RESET("\u001B[0m");

        private String value;

        Color(String value) {
            this.value = value;
        }

        public String toValue() {
            return this.value;
        }
    }
}
