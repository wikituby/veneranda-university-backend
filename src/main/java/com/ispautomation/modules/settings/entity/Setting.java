package com.ispautomation.modules.settings.entity;


import com.ispautomation.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;



@Entity
@Table(name = "settings")
public class Setting extends BaseEntity {



    @Column(name = "tenant_id")
    private Long tenantId;



    @Column(name = "category", nullable = false, length = 50)
    private String category;



    @Column(name = "key", nullable = false, length = 100)
    private String key;



    @Column(name = "value", columnDefinition = "TEXT")
    private String value;



    @Column(
            name = "value_type",
            nullable = false,
            length = 20
    )
    private String valueType = "STRING";



    @Column(name = "description", columnDefinition = "TEXT")
    private String description;



    @Column(
            name = "is_public",
            nullable = false
    )
    private Boolean isPublic = false;



    @Column(
            name = "is_encrypted",
            nullable = false
    )
    private Boolean isEncrypted = false;




    // ================= GETTERS / SETTERS =================


    public Long getTenantId(){
        return tenantId;
    }


    public void setTenantId(Long tenantId){
        this.tenantId = tenantId;
    }



    public String getCategory(){
        return category;
    }


    public void setCategory(String category){
        this.category = category;
    }



    public String getKey(){
        return key;
    }


    public void setKey(String key){
        this.key = key;
    }



    public String getValue(){
        return value;
    }


    public void setValue(String value){
        this.value = value;
    }



    public String getValueType(){
        return valueType;
    }


    public void setValueType(String valueType){
        this.valueType = valueType;
    }



    public String getDescription(){
        return description;
    }


    public void setDescription(String description){
        this.description = description;
    }



    public Boolean getIsPublic(){
        return isPublic;
    }


    public void setIsPublic(Boolean isPublic){
        this.isPublic = isPublic;
    }



    public Boolean getIsEncrypted(){
        return isEncrypted;
    }


    public void setIsEncrypted(Boolean isEncrypted){
        this.isEncrypted = isEncrypted;
    }

}