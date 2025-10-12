package com.citi.audit_service.repository;

import com.citi.audit_service.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByCountry(String country);

    List<Address> findByPostalCode(String postalCode);
}
