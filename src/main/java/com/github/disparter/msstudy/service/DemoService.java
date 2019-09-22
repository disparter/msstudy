package com.github.disparter.msstudy.service;

import com.github.disparter.msstudy.domain.Demo;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link Demo}.
 */
public interface DemoService {

    /**
     * Save a demo.
     *
     * @param demo the entity to save.
     * @return the persisted entity.
     */
    Demo save(Demo demo);

    /**
     * Get all the demos.
     *
     * @return the list of entities.
     */
    List<Demo> findAll();


    /**
     * Get the "id" demo.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<Demo> findOne(Long id);

    /**
     * Delete the "id" demo.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
