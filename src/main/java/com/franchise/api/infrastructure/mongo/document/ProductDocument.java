package com.franchise.api.infrastructure.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
@CompoundIndex(def = "{'branchId': 1, 'name': 1}", unique = true)
public class ProductDocument {

    @Id
    private String id;

    @Indexed
    private String branchId;

    private String name;

    @Indexed
    private long stock;

    public ProductDocument() {
    }

    public ProductDocument(String id, String branchId, String name, long stock) {
        this.id = id;
        this.branchId = branchId;
        this.name = name;
        this.stock = stock;
    }

    public String getId() {
        return id;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public long getStock() {
        return stock;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStock(long stock) {
        this.stock = stock;
    }
}

