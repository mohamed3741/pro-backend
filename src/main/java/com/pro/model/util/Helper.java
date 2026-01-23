package com.pro.model.util;

import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class Helper {

    public String generateFileName(String orginalName) {
        return new Date().getTime() + "-" + orginalName.replace(" ", "_");
    }
}


