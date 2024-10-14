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
    @Expose
    private Boolean isActive;

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

    @Column(name = "download_key")
    private String downloadKey;

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
        this.downloadKey = UUID.randomUUID().toString();
    }

    // ゲッター
    public String getDetailPageLink() {
        return "/document/" + this.id;
    }

    public String getDownloadLink() {
        return "/api/document/download?key=" + this.downloadKey;
    }
}
