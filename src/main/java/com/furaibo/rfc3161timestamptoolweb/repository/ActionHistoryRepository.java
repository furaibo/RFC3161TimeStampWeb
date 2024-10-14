package com.furaibo.rfc3161timestamptoolweb.repository;

import com.furaibo.rfc3161timestamptoolweb.model.ActionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionHistoryRepository extends JpaRepository<ActionHistory, Integer> {

    @Query(value = "SELECT * FROM action_histories" +
            " ORDER BY created_at DESC" +
            " LIMIT :limit", nativeQuery = true)
    List<ActionHistory> findLatestWithLimit(int limit);

}
