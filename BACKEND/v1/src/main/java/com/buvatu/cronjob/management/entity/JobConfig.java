package com.buvatu.cronjob.management.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.UUID;

@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "job_config")
@Data
public class JobConfig extends BaseEntity{

    @Column(unique = true, nullable = false)
    private String name;

    private String expression;

    private Integer poolSize;

    @Version
    private Long version;
}