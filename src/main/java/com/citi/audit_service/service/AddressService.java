package com.citi.audit_service.service;

import com.citi.audit_service.annotation.AuditAction;
import com.citi.audit_service.annotation.Auditable;
import com.citi.audit_service.model.Address;
import com.citi.audit_service.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;

    public List<Address> getAllAddresses() {
        return (List<Address>) addressRepository.findAll();
    }

    public Optional<Address> getAddressById(Long id) {
        return addressRepository.findById(id);
    }

    public List<Address> getAddressesByCountry(String country) {
        return addressRepository.findByCountry(country);
    }

    public List<Address> getAddressesByPostalCode(String postalCode) {
        return addressRepository.findByPostalCode(postalCode);
    }

    @Auditable(action = AuditAction.CREATE, domain = "HR", entity = "ADDRESS")
    public Address createAddress(Address address) {
        address.setCreatedTimestamp(LocalDateTime.now());
        address.setUpdatedTimestamp(LocalDateTime.now());
        return addressRepository.save(address);
    }

    @Auditable(action = AuditAction.UPDATE, domain = "HR", entity = "ADDRESS")
    public Address updateAddress(Long id, Address addressDetails) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + id));

        address.setAddressLine1(addressDetails.getAddressLine1());
        address.setAddressLine2(addressDetails.getAddressLine2());
        address.setAddressLine3(addressDetails.getAddressLine3());
        address.setCountry(addressDetails.getCountry());
        address.setPostalCode(addressDetails.getPostalCode());
        address.setUpdatedBy(addressDetails.getUpdatedBy());
        address.setUpdatedTimestamp(LocalDateTime.now());

        return addressRepository.save(address);
    }

    @Auditable(action = AuditAction.DELETE, domain = "HR", entity = "ADDRESS")
    public void deleteAddress(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + id));
        addressRepository.delete(address);
    }
}
