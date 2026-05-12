package com.franchise.api.infrastructure.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("branches")
@CompoundIndex(def = "{'franchiseId': 1, 'name': 1}", unique = true)
public class BranchDocument {

    @Id
    private String id;

    @Indexed
    private String franchiseId;

    private String name;

    public BranchDocument() {
    }

    public BranchDocument(String id, String franchiseId, String name) {
        this.id = id;
        this.franchiseId = franchiseId;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getFranchiseId() {
        return franchiseId;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setFranchiseId(String franchiseId) {
        this.franchiseId = franchiseId;
    }

    public void setName(String name) {
        this.name = name;
    }
}

