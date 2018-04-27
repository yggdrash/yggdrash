package io.yggdrash.core

import spock.lang.Specification

class HashUtilsTest extends Specification {
    def "sha256"() {
        expect:
        def sha256 = HashUtils.sha256("dkdkdk".bytes)
        println sha256
    }

    def "hashed"() {
        expect:
        def sha256 = HashUtils.hashString("TEST")
        println sha256
    }
}
