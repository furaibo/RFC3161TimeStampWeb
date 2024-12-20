package com.furaibo.rfc3161timestamptoolweb.repository;

import com.furaibo.rfc3161timestamptoolweb.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    @Query(value = "SELECT * FROM documents WHERE document_key = :key", nativeQuery = true)
    Document getByDocumentKey(String key);

    @Query(value = "SELECT * FROM documents " +
            "ORDER BY created_at DESC " +
            "LIMIT :limit ", nativeQuery = true)
    List<Document> findLatestWithLimit(int limit);

    @Query(value = "SELECT * FROM documents " +
            "WHERE (title LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%') ", nativeQuery = true)
    List<Document> findByKeyword(String keyword);

    @Query(value = "SELECT * FROM documents " +
            "WHERE (created_at BETWEEN :startDate AND :endDate) ", nativeQuery = true)
    List<Document> findByDateRange(LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT * FROM documents " +
            "WHERE (title LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%') " +
            "  AND (created_at BETWEEN :startDate AND :endDate)", nativeQuery = true)
    List<Document> findByKeywordAndDateRange(String keyword, LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT * FROM documents " +
            "WHERE (title LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%') " +
            "\n -- #pageable \n", nativeQuery = true)
    Page<Document> findPageByKeyword(Pageable pageable, String keyword);

    @Query(value = "SELECT * FROM documents " +
            "WHERE (created_at BETWEEN :startDate AND :endDate) " +
            "\n -- #pageable \n", nativeQuery = true)
    Page<Document> findPageByDateRange(Pageable pageable, LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT * FROM documents " +
            "WHERE (title LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%') " +
            "  AND (created_at BETWEEN :startDate AND :endDate) " +
            "\n -- #pageable \n", nativeQuery = true)
    Page<Document> findPageByKeywordAndDateRange(Pageable pageable, String keyword, LocalDate startDate, LocalDate endDate);
}
