package tn.engn.employeeapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import tn.engn.assignmentapi.repository.AssignableEntityRepository;
import tn.engn.employeeapi.model.Employee;

import java.util.List;

/**
 * Repository interface for managing Employee entities.
 */
@Repository
public interface EmployeeRepository extends AssignableEntityRepository<Employee> {

    /**
     * Finds employees by first name.
     * @param firstName the first name of the employees
     * @return the list of employees with the given first name
     */
    List<Employee> findByFirstName(String firstName);

    /**
     * Finds employees by first name with pagination.
     * @param firstName the first name of the employees
     * @param pageable the pagination information
     * @return the page of employees with the given first name
     */
    Page<Employee> findByFirstName(String firstName, Pageable pageable);

    /**
     * Finds employees by last name.
     * @param lastName the last name of the employees
     * @return the list of employees with the given last name
     */
    List<Employee> findByLastName(String lastName);

    /**
     * Finds employees by last name with pagination.
     * @param lastName the last name of the employees
     * @param pageable the pagination information
     * @return the page of employees with the given last name
     */
    Page<Employee> findByLastName(String lastName, Pageable pageable);

    /**
     * Finds employees by email.
     * @param email the email of the employees
     * @return the list of employees with the given email
     */
    List<Employee> findByEmail(String email);

    /**
     * Finds employees by email with pagination.
     * @param email the email of the employees
     * @param pageable the pagination information
     * @return the page of employees with the given email
     */
    Page<Employee> findByEmail(String email, Pageable pageable);

    /**
     * Finds employees by first name containing the given substring.
     * @param firstName the substring of the first name of the employees
     * @return the list of employees whose first name contains the given substring
     */
    List<Employee> findByFirstNameContaining(String firstName);

    /**
     * Finds employees by first name containing the given substring with pagination.
     * @param firstName the substring of the first name of the employees
     * @param pageable the pagination information
     * @return the page of employees whose first name contains the given substring
     */
    Page<Employee> findByFirstNameContaining(String firstName, Pageable pageable);

    /**
     * Finds employees by last name containing the given substring.
     * @param lastName the substring of the last name of the employees
     * @return the list of employees whose last name contains the given substring
     */
    List<Employee> findByLastNameContaining(String lastName);

    /**
     * Finds employees by last name containing the given substring with pagination.
     * @param lastName the substring of the last name of the employees
     * @param pageable the pagination information
     * @return the page of employees whose last name contains the given substring
     */
    Page<Employee> findByLastNameContaining(String lastName, Pageable pageable);

    /**
     * Finds employees by email containing the given substring.
     * @param email the substring of the email of the employees
     * @return the list of employees whose email contains the given substring
     */
    List<Employee> findByEmailContaining(String email);

    /**
     * Finds employees by email containing the given substring with pagination.
     * @param email the substring of the email of the employees
     * @param pageable the pagination information
     * @return the page of employees whose email contains the given substring
     */
    Page<Employee> findByEmailContaining(String email, Pageable pageable);

    boolean existsByEmail(String email);
}
