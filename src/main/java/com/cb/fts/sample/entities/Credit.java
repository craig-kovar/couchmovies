package com.cb.fts.sample.entities;

import com.couchbase.client.java.repository.annotation.Field;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Credit implements Serializable {


    @Field("credit_id")
    private String creditId;
    private String department;
    private String gender;
    private Long id;
    private String job;
    private String name;
    @Field("profile_path")
    private String profilePath;

}
