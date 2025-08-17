package com.techoworld.eightythree.service;

import com.techoworld.eightythree.entity.Invoice;
import com.techoworld.eightythree.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {
    private final InvoiceRepository repo;
    public InvoiceService(InvoiceRepository repo) { this.repo = repo; }

    public List<Invoice> findAll() { return repo.findAll(); }
    public Optional<Invoice> findById(Long id) { return repo.findById(id); }
    public Invoice save(Invoice i) { return repo.save(i); }
    public void deleteById(Long id) { repo.deleteById(id); }
}