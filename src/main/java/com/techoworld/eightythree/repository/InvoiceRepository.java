package com.techoworld.eightythree.repository;

import com.techoworld.eightythree.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {}
