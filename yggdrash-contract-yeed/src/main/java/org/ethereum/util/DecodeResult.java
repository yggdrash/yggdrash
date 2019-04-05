/*
 * Copyright 2019 Akashic Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ethereum.util;

import org.spongycastle.util.encoders.Hex;
import java.io.Serializable;

@SuppressWarnings("serial")
public class DecodeResult implements Serializable {

    private int pos;
    private Object decoded;

    public DecodeResult(int pos, Object decoded) {
        this.pos = pos;
        this.decoded = decoded;
    }

    public int getPos() {
        return pos;
    }

    public Object getDecoded() {
        return decoded;
    }

    public String toString() {
        return asString(this.decoded);
    }

    private String asString(Object decoded) {
        if (decoded instanceof String) {
            return (String) decoded;
        } else if (decoded instanceof byte[]) {
            return Hex.toHexString((byte[]) decoded);
        } else if (decoded instanceof Object[]) {
            StringBuilder result = new StringBuilder();
            for (Object item : (Object[]) decoded) {
                result.append(asString(item));
            }
            return result.toString();
        }
        throw new RuntimeException("Not a valid type. Should not occur");
    }
}
