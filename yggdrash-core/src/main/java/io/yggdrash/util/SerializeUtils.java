package io.yggdrash.util;

public class SerializeUtils {

    public static byte[] serialize(Object obj) {

        return obj.toString().getBytes();
    }

//    public static byte[] serialize(Object obj) throws IOException {
//
//        byte[] result;
//
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
//                oos.writeObject(obj);
//                result = baos.toByteArray();
//            }
//        }
//
//        return result;
//    }
//
//    public static byte[] serialize(JSONObject obj) throws IOException {
//
//        byte[] result;
//
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
//                oos.writeObject(obj);
//                result = baos.toByteArray();
//            }
//        }
//
//        return result;
//    }
//
//    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
//
//        Object result;
//
//        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
//            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
//                Object obj = ois.readObject();
//                result = obj;
//            }
//        }
//
//        return result;
//    }

}
