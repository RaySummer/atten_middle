package com.ray.atten.middle.module.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sys_company")
@Data
public class Company extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String name;

}