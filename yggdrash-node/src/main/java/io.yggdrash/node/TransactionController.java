package io.yggdrash.node;

import io.yggdrash.core.Transaction;
import io.yggdrash.core.TransactionPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("txs")
public class TransactionController {
    private final TransactionPool txPool;

    @Autowired
    public TransactionController(TransactionPool txPool) {
        this.txPool = txPool;
    }

    @PostMapping
    public ResponseEntity add(@RequestBody TransactionDto request) {
        try {
            Transaction tx = TransactionDto.of(request);
            Transaction addedTx = txPool.addTx(tx);
            return ResponseEntity.ok(TransactionDto.createBy(addedTx));
        } catch (IOException e) {
            e.printStackTrace();
            // TODO 에러정의를 다시 해 볼수 있도록 합니다.
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("{id}")
    public ResponseEntity get(@PathVariable String id) {
        Transaction tx = txPool.getTxByHash(id);

        if (tx == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(TransactionDto.createBy(tx));
    }
}
