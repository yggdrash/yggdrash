package io.yggdrash.node;

import io.yggdrash.core.Block;
import io.yggdrash.core.BlockChain;
import io.yggdrash.core.BlockGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("blocks")
class BlockController {
    private final BlockGenerator blockGenerator;
    private final BlockChain blockChain;

    @Autowired
    public BlockController(BlockGenerator blockGenerator, BlockChain blockChain) {
        this.blockGenerator = blockGenerator;
        this.blockChain = blockChain;
    }

    @PostMapping
    public Block add(@RequestBody String data) {
        Block generatedBlock = blockGenerator.generate(data);
        blockChain.addBlock(generatedBlock);
        return generatedBlock;
    }

    @GetMapping("{id}")
    public Block get(@PathVariable(name = "id") String id) {
        Block foundBlock;
        if (isNumeric(id)) {
            int index = Integer.parseInt(id);
            foundBlock = blockChain.getBlockByIndex(index);
        } else {
            foundBlock = blockChain.getBlockByHash(id);
        }
        
        return foundBlock;
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}