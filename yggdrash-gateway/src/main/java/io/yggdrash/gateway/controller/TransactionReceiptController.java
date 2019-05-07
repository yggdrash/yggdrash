package io.yggdrash.gateway.controller;

import io.yggdrash.contract.core.TransactionReceipt;
import io.yggdrash.core.blockchain.BranchGroup;
import io.yggdrash.core.blockchain.BranchId;
import io.yggdrash.gateway.dto.TransactionReceiptDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.yggdrash.common.config.Constants.BRANCH_ID;

@RestController
@RequestMapping("branches/{branchId}/txr")
public class TransactionReceiptController {
    private final BranchGroup branchGroup;

    @Autowired
    public TransactionReceiptController(BranchGroup branchGroup) {
        this.branchGroup = branchGroup;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable(name = BRANCH_ID) String branchId,
                              @PathVariable String id) {
        TransactionReceipt receipt = branchGroup.getTransactionReceipt(BranchId.of(branchId), id);

        if (receipt == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(TransactionReceiptDto.createBy(receipt));
    }
}
