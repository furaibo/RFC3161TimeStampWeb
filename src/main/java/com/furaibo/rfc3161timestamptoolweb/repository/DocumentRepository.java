package com.furaibo.rfc3161timestamptoolweb.repository;

import com.furaibo.rfc3161timestamptoolweb.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    @Query(value = "SELECT * FROM documents WHERE download_key = :key", nativeQuery = true)
    Document getByDownloadKey(String key);

    @Query(value = "SELECT * FROM documents WHERE id IN :ids", nativeQuery = true)
    List<Document> findByIDs(List<Integer> ids);

    @Query(value = "SELECT * FROM documents " +
            "ORDER BY created_at DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Document> findLatestWithLimit(int limit);

    @Query(value = "SELECT * FROM documents " +
            "WHERE (title LIKE '%' || :keyword || '%') OR (description LIKE '%' || :keyword || '%') " +
            "  AND (created_at BETWEEN :startDate AND :endDate) " +
            "ORDER BY created_at DESC", nativeQuery = true)
    List<Document> findByKeywordAndDateRange(String keyword, LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT * FROM documents " +
            "WHERE created_at BETWEEN ':fromDate' AND ':toDate'", nativeQuery = true)
    List<Document> findByDateRange(String word, String fromDate, String toDate);
}
