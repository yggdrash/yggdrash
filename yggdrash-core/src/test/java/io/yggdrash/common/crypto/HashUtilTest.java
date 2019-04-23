package io.yggdrash.common.crypto;

import io.yggdrash.TestConstants.SlowTest;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.security.SecureRandom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HashUtilTest extends SlowTest {

    private static final Logger log = LoggerFactory.getLogger(HashUtilTest.class);

    @Before
    public void setUp() {
        //todo: change test vectors to NISTs.
    }

    @Test
    public void SHA1StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "SHA-1"));
        log.info(result1);
        assertEquals("0114498021cb8c4f1519f96bdf58dd806f3adb63", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "SHA-1"));
        log.info(result2);
        assertEquals("07a1a50a6273e6bc2eb94d647810cdc5b275b924", result2);
    }

    @Test
    public void SHA224StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "SHA-224"));
        log.info(result1);
        assertEquals("fc5d6aed7146d6747dd6fca075f9fe5a30a4c0c9ff67effc484f10b5", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "SHA-224"));
        log.info(result2);
        assertEquals("820518c626796d9af2db299dfb37c8737d8f56f12c44fb99b7aece54", result2);
    }

    @Test
    public void SHA256StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "SHA-256"));
        log.info(result1);
        assertEquals("60e05bd1b195af2f94112fa7197a5c88289058840ce7c6df9693756bc6250f55", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "SHA-256"));
        log.info(result2);
        assertEquals("3138bb9bc78df27c473ecfd1410f7bd45ebac1f59cf3ff9cfe4db77aab7aedd3", result2);
    }

    @Test
    public void SHA384StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "SHA-384"));
        log.info(result1);
        assertEquals("53871b26a08e90cb62142f2a39f0b80de41792322b0ca560"
                + "2b6eb7b5cf067c49498a7492bb9364bbf90f40c1c5412105", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "SHA-384"));
        log.info(result2);
        assertEquals("751a419cb935b79162930b839109e7b40d06e4a09332bd44"
                + "8f2b089478096e99a1d0c820c31a7aa92a35bfe9e6113425", result2);
    }

    @Test
    public void SHA512StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "SHA-512"));
        log.info(result1);
        assertEquals("8f6beb3c0792f50c176800332f4468f76b4457b41d2f68e294cb46e53addbf57"
                + "69a59eddf33e19394e8ab78e374b1bd33a680d26464fcd1174da226af9c8cd6e", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "SHA-512"));
        log.info(result2);
        assertEquals("6723a63fc813efa037dab2128781cbc395a90ffd83bf2b520d6d62488350d898"
                + "fd5624717ac2fa443388cb80fb7a784a04aa4fa6659c4fcce87e62dec718bb95", result2);
    }

    @Test
    public void RIPEMD160StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "RIPEMD160"));
        log.info(result1);
        assertEquals("fd2bead7cf387c7896e2f42926fca4b4a0483d88", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "RIPEMD160"));
        log.info(result2);
        assertEquals("b2b0034d91c88d857e5c7164086343291c0d9be8", result2);
    }

    @Test
    public void Keccak256StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "KECCAK-256"));
        log.info(result1);
        assertEquals("d874d9e5ad41e13e8908ab82802618272c3433171cdc3d634f3b1ad0e6742827", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "KECCAK-256"));
        log.info(result2);
        assertEquals("01b2a6c0dd38f8fb49ea3594776c584a74321ecebe87a4e885636153c96f79fc", result2);
    }

    @Test
    public void Keccak512StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "KECCAK-512"));
        log.info(result1);
        assertEquals("b4c84e84c5bac30ab535c0ed496c178d093ada704342da9d16f9f16bda957f18"
                + "79a000627f8b0958eb3805803c0a9da269f71d611437334053828309cab6f1bb", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "KECCAK-512"));
        log.info(result2);
        assertEquals("f82a967b743fe010df792d3fa843bbb826c04fecd6d3bc36aa6c40a9fb452ffa"
                + "0e841af94547129c572d5150d37f58909b03c882ac5a18e3cc0b72e9aa73e32b", result2);
    }

    @Test
    public void SHA3_224StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "SHA3-224"));
        log.info(result1);
        assertEquals("ad5c4adcaa5ae42d9ba3ef45f530b7165e1705dd4eb78ef8ab2f8bba", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "SHA3-224"));
        log.info(result2);
        assertEquals("a130f28dc1ab0f7ece1383db81476cdc2ed9818b81002c2438c3e883", result2);
    }

    @Test
    public void SHA3_256StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "SHA3-256"));
        log.info(result1);
        assertEquals("c6fdd7a7f70862b36a26ccd14752268061e98103299b28fe7763bd9629926f4b", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "SHA3-256"));
        log.info(result2);
        assertEquals("a3284ba81d18dfa82dbe17b7a8af3321ec406ff4f264e26d70fd88a870913686", result2);
    }

    @Test
    public void SHA3_384StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "SHA3-384"));
        log.info(result1);
        assertEquals("4c3578fa9e31872b06a2f3cdbd91470591f963fa6c38d76c"
                + "4754970b60a1d9c77fc2adf2fdfef804ea77ef6872dd8616", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "SHA3-384"));
        log.info(result2);
        assertEquals("6a57cf6f631c2beb383cdc5c9d306ba6f57f0584ff8f9306"
                + "9e342b42479625ce5a61e82d1c0f23bc44df941c485ed3d8", result2);
    }

    @Test
    public void SHA3_512StaticTest() {
        String input1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String result1 = Hex.toHexString(HashUtil.hash(input1.getBytes(), "SHA3-512"));
        log.info(result1);
        assertEquals("27f4caaa1d51d54a53cb2393fa4e24b542a963509055a2a4864816a4d2375d3a"
                + "afd433df86c25a4529503a0c99ab46e97871e573d45de78e9508fe581693694e", result1);

        String input2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String result2 = Hex.toHexString(HashUtil.hash(input2.getBytes(), "SHA3-512"));
        log.info(result2);
        assertEquals("6aedf8191b2ff5d4c704df5718eeb35a57dc0438a506294d2a528cc5d3aa2d63"
                + "3f5eceb7b49db152fb0b9a4fd981d3659c4a5ee58454825b824569a15e83a2b6", result2);
    }

    @Test
    public void PBKDF2_SHA256_Test() {
        String password = "testpassword";
        byte[] salt = Hex.decode("ae3cd4e7013836a3df6bd7241b12db061dbe2c6785853cce422d148a624ce0bd");
        int iterations = 262144;
        int len = 32;

        byte[] pbkdf2Password = HashUtil.pbkdf2(password.getBytes(), salt, iterations, len, "SHA-256");
        log.info(Hex.toHexString(pbkdf2Password));
        assertArrayEquals(Hex.decode("f06d69cdc7da0faffb1008270bca38f5e31891a3a773950e6d0fea48a7188551"),
                pbkdf2Password);
    }

    @Test
    public void PBKDF2_SHA512_Test() {
        String password = "testpassword";
        byte[] salt = Hex.decode("ae3cd4e7013836a3df6bd7241b12db061dbe2c6785853cce422d148a624ce0bd");
        int iterations = 262144;
        int len = 32;

        byte[] pbkdf2Password = HashUtil.pbkdf2(password.getBytes(), salt, iterations, len, "SHA-512");
        log.info(Hex.toHexString(pbkdf2Password));
        assertArrayEquals(Hex.decode("5e005b008b20c7ab157725ed2f72faa4357a51eddac85837ca7d925e8daa0d41"),
                pbkdf2Password);
    }

    @Test
    public void PBKDF2_SHA1_Test() {
        String password = "testpassword";
        byte[] salt = Hex.decode("ae3cd4e7013836a3df6bd7241b12db061dbe2c6785853cce422d148a624ce0bd");
        int iterations = 262144;
        int len = 32;

        byte[] pbkdf2Password = HashUtil.pbkdf2(password.getBytes(), salt, iterations, len, "SHA-1");
        log.info(Hex.toHexString(pbkdf2Password));
        assertArrayEquals(Hex.decode("279af536192ea4f5dac04f96471d94ea174c38d4b9a9a908f03217a215fc01f8"),
                pbkdf2Password);
    }

    @Test
    public void PBKDF2_KECCAK256_Test() {
        String password = "testpassword";
        byte[] salt = Hex.decode("ae3cd4e7013836a3df6bd7241b12db061dbe2c6785853cce422d148a624ce0bd");
        int iterations = 262144;
        int len = 32;

        byte[] pbkdf2Password = HashUtil.pbkdf2(password.getBytes(), salt, iterations, len, "KECCAK-256");
        log.info(Hex.toHexString(pbkdf2Password));
        assertArrayEquals(Hex.decode("c5a5e1ea4b87eff0119d4cedf89401c4777d0f1b374c654bb65026eb570183b7"),
                pbkdf2Password);
    }

    @Test
    public void PBKDF2_SHA3256_Test() {
        String password = "testpassword";
        byte[] salt = Hex.decode("ae3cd4e7013836a3df6bd7241b12db061dbe2c6785853cce422d148a624ce0bd");
        int iterations = 262144;
        int len = 32;

        byte[] pbkdf2Password = HashUtil.pbkdf2(password.getBytes(), salt, iterations, len, "SHA3-256");
        log.info(Hex.toHexString(pbkdf2Password));
        assertArrayEquals(Hex.decode("2761ad467ba3d88c8c331e93829ec5ce912048037c50c8d5faeb4af87bbeb102"),
                pbkdf2Password);
    }

    @Test
    public void PBKDF2_SHA3512_Test() {
        String password = "testpassword";
        byte[] salt = Hex.decode("ae3cd4e7013836a3df6bd7241b12db061dbe2c6785853cce422d148a624ce0bd");
        int iterations = 262144;
        int len = 32;

        byte[] pbkdf2Password = HashUtil.pbkdf2(password.getBytes(), salt, iterations, len, "SHA3-512");
        log.info(Hex.toHexString(pbkdf2Password));
        assertArrayEquals(Hex.decode("2cf80c8e19136485eb79eba7349c8f334582f3b7f22e48e07124c4b5a85631c7"),
                pbkdf2Password);
    }

    @Test
    public void PBKDF2_SHA256_Random_Test() {
        String password = "testpassword";
        int iterations = 262144;
        int len = 32;

        byte[] salt = new byte[32];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(salt);
        log.info("salt= {}", Hex.toHexString(salt));

        byte[] pbkdf2Password = HashUtil.pbkdf2(password.getBytes(), salt, iterations, len, "SHA-256");
        log.info(Hex.toHexString(pbkdf2Password));
        assertNotNull(pbkdf2Password);
    }

}
