package com.pro.model.Enum;

public enum MediaEnum {
    LOGO ("media_logo"),
    IMAGE ("media_image"),
    CATEGORY ("media_category"),
    PRODUCT ("media_product"),
    ITEM ("media_item"),
    CONFIG ("media_config");

    private String name = "";

    //Constructeur
    MediaEnum(String name){
        this.name = name;
    }

    public String toString(){
        return name;
    }
}



