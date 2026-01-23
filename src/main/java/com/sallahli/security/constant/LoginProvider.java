package com.sallahli.security.constant;

public enum LoginProvider {

    GOOGLE("google", "urn:ietf:params:oauth:token-type:access_token"),
    APPLE("apple", "urn:ietf:params:oauth:token-type:id_token"),
    FACEBOOK("facebook", "urn:ietf:params:oauth:token-type:access_token");

    private String providerName;
    private String subjectTokenType;

    private LoginProvider(String providerName, String subjectTokenType){
        this.providerName = providerName;
        this.subjectTokenType = subjectTokenType;
    }

    public String getSubjectTokenType() {
        return subjectTokenType;
    }

    public String getProviderName() {
        return providerName;
    }

}


