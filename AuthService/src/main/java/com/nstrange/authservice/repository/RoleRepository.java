package com.nstrange.authservice.repository;

import com.nstrange.authservice.entities.UserRole;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<UserRole, Long> {
    Optional<UserRole> findByName(String name);
}
