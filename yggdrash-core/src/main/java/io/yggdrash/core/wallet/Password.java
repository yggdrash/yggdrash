package io.yggdrash.core.wallet;

import io.yggdrash.common.config.Constants;

public class Password {

    private Password() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Password validation check.
     *
     * @param password password
     * @return validation
     */
    public static boolean passwordValid(String password) {
        //todo: considering password check lib or NIST regulation
        //reference: https://github.com/vt-middleware/passay

        // password length check
        if (password.length() > Constants.PASSWORD_MAX || password.length() < Constants.PASSWORD_MIN) {
            return false;
        }

        // check valid character
        if (!password.matches("[A-Za-z0-9\\x21-\\x2F\\x3A-\\x40\\x5B-\\x60\\x7B-\\x7E]+")) {
            return false;
        }

        // 1 more Upper case
        if (!password.matches("(.*[A-Z].*)")) {
            return false;
        }

        // 1 more lower case
        if (!password.matches("(.*[a-z].*)")) {
            return false;
        }

        // 1 more number
        if (!password.matches("(.*[0-9].*)")) {
            return false;
        }

        // 1 more special symbol(ASCII character)
        return password.matches("(.*[\\x21-\\x2F\\x3A-\\x40\\x5B-\\x60\\x7B-\\x7E].*$)");
    }
}