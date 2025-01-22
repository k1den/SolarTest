package com.congratulation.TestSolar.repo;

import com.congratulation.TestSolar.models.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
