package com.nstrange.authservice.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nstrange.authservice.eventProducer.UserInfoEvent;
//import com.nstrange.authservice.model.UserInfoDto;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class UserInfoSerializer implements Serializer<UserInfoEvent> {

    @Override
    public void configure(Map<String, ?> map, boolean b) {
    }
    @Override
    public byte[] serialize(String arg0, UserInfoEvent arg1){
        byte[] returnValue = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            returnValue = objectMapper.writeValueAsString(arg1).getBytes();
        }catch (Exception e){
            e.printStackTrace();
        }
        return returnValue;
    }
    @Override
    public void close() {
    }
}
