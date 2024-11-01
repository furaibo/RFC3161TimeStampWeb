package com.furaibo.rfc3161timestamptoolweb.repository;

import com.furaibo.rfc3161timestamptoolweb.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    @Query(value = "SELECT * FROM documents WHERE id = :id", nativeQuery = true)
    Document getById(int id);

    @Query(value = "SELECT * FROM documents " +
            " ORDER BY created_at DESC" +
            " LIMIT :limit", nativeQuery = true)
    List<Document> getLatestWithLimit(int limit);

    @Query(value = "SELECT * FROM documents " +
            " WHERE created_at BETWEEN ':fromDate' AND ':toDate'", nativeQuery = true)
    List<Document> findByDateRange(String word, String fromDate, String toDate);
}
