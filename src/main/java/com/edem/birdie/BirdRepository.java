package com.edem.birdie;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BirdRepository extends JpaRepository<Bird, Long> {

    @Query("SELECT c FROM Bird c WHERE c.s3url LIKE %:imageKey")
    Optional<Bird> findByS3urlEndingWith(@Param("imageKey")String imageKey);
}