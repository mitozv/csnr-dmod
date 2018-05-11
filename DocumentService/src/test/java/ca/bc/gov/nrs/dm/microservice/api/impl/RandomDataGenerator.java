package ca.bc.gov.nrs.dm.microservice.api.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

public class RandomDataGenerator {
	private Random random = new Random();
	 
    public <T> T createAndFill(Class<T> clazz) throws Exception {
        T instance = clazz.newInstance();
        for(Field field: clazz.getDeclaredFields()) {
        	try {
        		field.setAccessible(true);
                
                if(!Modifier.isFinal(field.getModifiers())) {
                	Object value = getRandomValueForField(field);
                    field.set(instance, value);
                }
        	}catch(Exception e) {
        		//if error, ignore just don't fill the field
        	}
            
            
        }
        return instance;
    }
 
    private Object getRandomValueForField(Field field) throws Exception {
        Class<?> type = field.getType();
 
        if(type.isEnum()) {
            Object[] enumValues = type.getEnumConstants();
            return enumValues[random.nextInt(enumValues.length)];
        } else if(type.equals(Integer.TYPE) || type.equals(Integer.class)) {
            return random.nextInt();
        } else if(type.equals(Long.TYPE) || type.equals(Long.class)) {
            return random.nextLong();
        } else if(type.equals(Double.TYPE) || type.equals(Double.class)) {
            return random.nextDouble();
        } else if(type.equals(Float.TYPE) || type.equals(Float.class)) {
            return random.nextFloat();
        } else if(type.equals(String.class)) {
            return UUID.randomUUID().toString();
        } else if(type.equals(BigInteger.class)){
            return BigInteger.valueOf(random.nextInt());
        } else if(type.equals(BigDecimal.class)){
            return BigDecimal.valueOf(random.nextDouble());
        } else if(type.equals(Boolean.class)){
            return Boolean.valueOf(random.nextBoolean());
        } else if(type.equals(Date.class)){
            return new Date();
        }
        return createAndFill(type);
    }
}
