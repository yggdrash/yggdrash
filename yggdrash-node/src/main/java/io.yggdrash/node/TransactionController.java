package io.yggdrash.node;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("txs")
public class TransactionController {

    @PostMapping
    public ResponseEntity add(@RequestBody TxDto request) {
        TxDto res = new TxDto();
        res.setFrom(request.getFrom());
        return ResponseEntity.ok(res);
    }
}
