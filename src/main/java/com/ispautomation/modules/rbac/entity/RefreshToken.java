package com.ispautomation.modules.rbac.entity;


import com.ispautomation.common.entity.BaseEntity;

import jakarta.persistence.*;

import java.time.LocalDateTime;



@Entity
@Table(name="refresh_tokens")
public class RefreshToken extends BaseEntity {



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;



    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 100
    )
    private String tokenHash;



    @Column(
            name = "device_info",
            length = 255
    )
    private String deviceInfo;



    @Column(
            name = "ip_address",
            length = 45
    )
    private String ipAddress;



    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;



    @Column(
            name = "revoked_at"
    )
    private LocalDateTime revokedAt;



    @Column(
            name = "is_revoked",
            nullable = false
    )
    private Boolean isRevoked = false;



    // ================= GETTERS / SETTERS =================


    public User getUser(){
        return user;
    }


    public void setUser(User user){
        this.user=user;
    }



    public String getTokenHash(){
        return tokenHash;
    }


    public void setTokenHash(String tokenHash){
        this.tokenHash=tokenHash;
    }



    public String getDeviceInfo(){
        return deviceInfo;
    }


    public void setDeviceInfo(String deviceInfo){
        this.deviceInfo=deviceInfo;
    }



    public String getIpAddress(){
        return ipAddress;
    }


    public void setIpAddress(String ipAddress){
        this.ipAddress=ipAddress;
    }



    public LocalDateTime getExpiresAt(){
        return expiresAt;
    }


    public void setExpiresAt(LocalDateTime expiresAt){
        this.expiresAt=expiresAt;
    }



    public LocalDateTime getRevokedAt(){
        return revokedAt;
    }


    public void setRevokedAt(LocalDateTime revokedAt){
        this.revokedAt=revokedAt;
    }



    public Boolean getIsRevoked(){
        return isRevoked;
    }


    public void setIsRevoked(Boolean isRevoked){
        this.isRevoked=isRevoked;
    }

}