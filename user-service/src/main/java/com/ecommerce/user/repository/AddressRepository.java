package com.ecommerce.user.repository;

import com.ecommerce.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Address Repository
 *
 * Provides database operations for Address entity.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Find all addresses for a specific user
     *
     * @param userId User's ID
     * @return List of addresses
     */
    List<Address> findByUserId(Long userId);

    /**
     * Find a specific address for a user
     *
     * @param id Address ID
     * @param userId User's ID
     * @return Optional containing the address if found
     */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    /**
     * Delete all addresses for a specific user
     *
     * @param userId User's ID
     */
    void deleteByUserId(Long userId);
}
