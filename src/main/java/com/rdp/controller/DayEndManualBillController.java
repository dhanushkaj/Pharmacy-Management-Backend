package com.rdp.controller;

import com.rdp.model.RdpDayEndManualBill;
import com.rdp.repository.RdpDayEndManualBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/day-end-manual-bills")
@RequiredArgsConstructor
public class DayEndManualBillController {
    private final RdpDayEndManualBillRepository repo;

    @GetMapping("/{date}")
    public ResponseEntity<List<RdpDayEndManualBill>> getBills(@PathVariable String date) {
        return ResponseEntity.ok(repo.findByReportDate(date));
    }

    @PostMapping
    public ResponseEntity<RdpDayEndManualBill> addBill(@RequestBody RdpDayEndManualBill bill) {
        return ResponseEntity.ok(repo.save(bill));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RdpDayEndManualBill> updateBill(@PathVariable Long id, @RequestBody RdpDayEndManualBill bill) {
        bill.setId(id);
        return ResponseEntity.ok(repo.save(bill));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBill(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
