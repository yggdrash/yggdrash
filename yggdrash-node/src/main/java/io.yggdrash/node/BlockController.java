package io.yggdrash.node;

import io.yggdrash.core.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;

@RestController
@RequestMapping("blocks")
class BlockController {
    private final BlockBuilder blockBuilder;
    private final BlockChain blockChain;

    @Autowired
    public BlockController(BlockBuilder blockBuilder, BlockChain blockChain) {
        this.blockBuilder = blockBuilder;
        this.blockChain = blockChain;
    }

    @PostMapping
    public ResponseEntity add() {
        Block generatedBlock = blockBuilder.build("sample");
        blockChain.addBlock(generatedBlock);
        return ResponseEntity.ok(BlockDto.createBy(generatedBlock));
    }

    @GetMapping("{id}")
    public ResponseEntity get(@PathVariable(name = "id") String id) {
        Block foundBlock;
        if (isNumeric(id)) {
            int index = Integer.parseInt(id);
            foundBlock = blockChain.getBlockByIndex(index);
        } else {
            foundBlock = blockChain.getBlockByHash(id);
        }

        if (foundBlock == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(BlockDto.createBy(foundBlock));
    }

    @GetMapping
    public ResponseEntity getAll() {
        LinkedHashMap<byte[], Block> blocks = blockChain.getBlocks();
        return ResponseEntity.ok(blocks);
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