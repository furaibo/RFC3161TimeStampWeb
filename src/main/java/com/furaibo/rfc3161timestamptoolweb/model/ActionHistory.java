package com.furaibo.rfc3161timestamptoolweb.model;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.util.Date;

@Data
@Entity
@Table(name = "action_histories")
public class ActionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Expose
    private int id;

    @Column(name = "action_title")
    @Expose
    private String actionTitle;

    @Column(name = "action_desc")
    @Expose
    private String actionDesc;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @CreationTimestamp
    @Expose
    private Date createdAt;

}
