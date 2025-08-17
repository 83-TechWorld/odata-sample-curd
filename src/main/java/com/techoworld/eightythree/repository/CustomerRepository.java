package com.techoworld.eightythree.repository;

import com.techoworld.eightythree.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {}