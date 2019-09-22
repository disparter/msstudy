package com.github.disparter.msstudy.repository;

import com.github.disparter.msstudy.domain.Demo;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Demo entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DemoRepository extends JpaRepository<Demo, Long> {

}
