package com.techoworld.eightythree.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Long customerId; // keep simple across providers

    public Invoice() {}

    public Invoice(String invoiceNumber, Double totalAmount, Long customerId) {
        this.invoiceNumber = invoiceNumber;
        this.totalAmount = totalAmount;
        this.customerId = customerId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
}
