package com.cadac.stone_inscription.kafka.exception;

public class EventPublishException extends RuntimeException{
    
    private final String topic;
    private final String key;
    public EventPublishException(String topic,String key,String message,Throwable cause){
        super(message,cause);
        this.topic = topic;
        this.key = key;
    }

    public String getTopic() {return topic;}
    public String getKey() {return key;}
}
