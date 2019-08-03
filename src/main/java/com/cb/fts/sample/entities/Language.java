package com.cb.fts.sample.entities;

import com.couchbase.client.java.repository.annotation.Field;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Language {

    @Field("iso_639_1")
    private String IsoCode;
    private String name;
}
