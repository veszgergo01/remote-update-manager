package com.praxtourlauncher.api.entity;

public class Product {
    private Integer id;
    private String productName;
    private String productLogoButtonPath;
    private Integer defaultSettingsId;
    private Integer blocked =0;
    private Integer supportStreaming =0;
    private String communicationType = "RPM";
    private String productType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductLogoButtonPath() {
        return productLogoButtonPath;
    }

    public void setProductLogoButtonPath(String productLogoButtonPath) {
        this.productLogoButtonPath = productLogoButtonPath;
    }

    public Integer getDefaultSettingsId() {
        return defaultSettingsId;
    }

    public void setDefaultSettingsId(Integer defaultSettingsId) {
        this.defaultSettingsId = defaultSettingsId;
    }

    public Integer getBlocked() {
        return blocked;
    }

    public void setBlocked(Integer blocked) {
        this.blocked = blocked;
    }

    public Integer getSupportStreaming() {
        return supportStreaming;
    }

    public void setSupportStreaming(Integer supportStreaming) {
        this.supportStreaming = supportStreaming;
    }

    public String getCommunicationType() {
        return communicationType;
    }

    public void setCommunicationType(String communicationType) {
        this.communicationType = communicationType;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }
}
