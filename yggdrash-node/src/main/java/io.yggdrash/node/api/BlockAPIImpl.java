package io.yggdrash.node.api;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import io.yggdrash.node.mock.BlockMock;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@AutoJsonRpcServiceImpl
public class BlockAPIImpl implements BlockAPI {

    @Override
    public int blockNumber () {
        return 0;
    }

    @Override
    public String getBlockByHash(String address, String tag) throws IOException{
        BlockMock blockMock = new BlockMock();
        String block = blockMock.retBlockMock();
        return block;
    }

    @Override
    public String getBlockByNumber(String hashOfBlock, Boolean bool) throws IOException{
        BlockMock blockMock = new BlockMock();
        String block = blockMock.retBlockMock();
        return block;
    }

    @Override
    public int newBlockFilter() {
        return 0;
    }
}

/*
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"blockNumber","params":{}}' http://localhost:8080/block
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getBlockByHash","params":{"address":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf", "tag":"latest"}}' http://localhost:8080/block
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"getBlockByNumber","params":{"hashOfBlock":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf", "bool":true}}' http://localhost:8080/block
curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"newBlockFilter","params":{}}' http://localhost:8080/block

curl -H "Content-Type:application/json" -d '{"id":"1","jsonrpc":"2.0","method":"test","params":{"address":"0x76a9fa4681a8abf94618543872444ba079d5302203ac6a5b5b2087a9f56ea8bf"}}' http://localhost:8080/block
 */


