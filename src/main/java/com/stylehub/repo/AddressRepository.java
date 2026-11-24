package com.stylehub.repo;

import com.stylehub.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIdDesc(Long userId);
    Optional<Address> findByUserIdAndDefaultShippingTrue(Long userId);
}
