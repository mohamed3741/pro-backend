package com.pro.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

@Slf4j
public class TimerHelper {

    public static Instant initTimer(String message){
        log.info("*********** start {} ***********",message);
        return Instant.now();
    }

    public static void endTimer(Instant start, String context){
        log.info( "*********** {} Took {} ***********" ,context, Duration.between(start,Instant.now()));
    }


}


