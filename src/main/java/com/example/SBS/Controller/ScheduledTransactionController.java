package com.example.SBS.Controller;

import com.example.SBS.Entity.ScheduledTransaction;
import com.example.SBS.Service.ScheduledTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduled")
@RequiredArgsConstructor
public class ScheduledTransactionController {

    @Autowired
    private ScheduledTransactionService scheduledTransactionService;

    @PostMapping("/create")
    public ResponseEntity<ScheduledTransaction> scheduleTransaction(@RequestBody ScheduledTransaction txn) {
        ScheduledTransaction saved = scheduledTransactionService.scheduleTransaction(txn);
        return ResponseEntity.ok(saved);
    }
}
