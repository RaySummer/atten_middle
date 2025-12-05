package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Employee findByPin(String pin);
}
