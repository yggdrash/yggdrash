package io.yggdrash.crypto;

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.params.KDFParameters;

public class Password {

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
        if (password.length() > 32 || password.length() < 12) {
            //todo: change from static password length to config properties
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


    /**
     * generate KDF value.
     *
     * @param input     input data
     * @param outLength output length
     * @return the derivation key by the password
     */
    public static byte[] generateKeyDerivation(byte[] input, int outLength) {
        //todo: checking safety ( IV & ...)

        ConcatKDFBytesGenerator kdf = new ConcatKDFBytesGenerator(new SHA256Digest());
        kdf.init(new KDFParameters(input, HashUtil.sha3(input)));
        byte[] bytes = new byte[outLength];
        kdf.generateBytes(bytes, 0, bytes.length);

        return bytes;
    }
}