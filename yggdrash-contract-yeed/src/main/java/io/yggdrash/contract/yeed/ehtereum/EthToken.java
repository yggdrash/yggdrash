package io.yggdrash.contract.yeed.ehtereum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

public class EthToken extends Eth{
    private static final Logger log = LoggerFactory.getLogger(EthToken.class);

    byte[] method;
    byte[][] params;

    public EthToken(byte[] rawTransaction) {
        super(rawTransaction);
        byte[] data = this.getData();
        log.debug("is EthToken  : {}", (data.length - 4) % 32);
        if((data.length - 4) % 32 == 0) {
            // 4byte is method
            method = Arrays.copyOfRange(data, 0, 4);
            int pos = 4;
            int paramSize = (data.length - 4)/32;
            log.debug("Size : {}", paramSize);
            params = new byte[paramSize][];
            for( int i =0; i < paramSize; i++) {
                log.debug("POSITION : {} ", pos);
                params[i] = Arrays.copyOfRange(data, pos, (pos+32));
                pos += 32;
            }
        }
    }

    public byte[] getMethod() {
        return method;
    }

    public byte[][] getParam() {
        return params;
    }




}
