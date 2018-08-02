package io.yggdrash.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;

public class SerializeUtils {
    private static final Charset CHARSET = Charset.defaultCharset();

    private SerializeUtils() {
    }

    public static byte[] serialize(Object obj) {
        return obj.toString().getBytes(CHARSET);
    }

    public static byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            try (ObjectOutput out = new ObjectOutputStream(bos)) {
                out.writeObject(object);
                return bos.toByteArray();
            }
        }
    }

    public static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            try (ObjectInput in = new ObjectInputStream(bis)) {
                return in.readObject();
            }
        }
    }
}
