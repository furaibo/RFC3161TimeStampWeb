package com.furaibo.rfc3161timestamptoolweb.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Expose
    private int id;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "document_key", nullable = false, updatable = false)
    private String documentKey;

    @Column(name = "title", nullable = false)
    @Expose
    private String title;

    @Column(name = "description")
    @Expose
    private String description;

    @Column(name = "note")
    @Expose
    private String note;

    @Column(name = "upload_file_path")
    private String uploadFilePath;

    @Column(name = "timestamp_file_path")
    private String timestampFilePath;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // コンストラクタ
    public Document() {
        this.isActive = true;
        this.documentKey = UUID.randomUUID().toString();
    }

    // ゲッター
    public String getDetailPageLink() {
        return "/document/" + this.id;
    }

    public String getTitleUpdateApiLink() {
        return "/api/document/update/title";
    }

    public String getDescriptionUpdateApiLink() {
        return "/api/document/update/description";
    }

    public String getNoteUpdateApiLink() {
        return "/api/document/update/note";
    }

    public String getDownloadApiLink() {
        return "/api/document/download?key=" + this.documentKey;
    }

    public String getDeleteApiLink() {
        return "/api/document/delete?key=" + this.documentKey;
    }

    // セッター
    public void renewUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

}
