package com.sunrise.wiki.utils;


import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class Utils {


    /***
     * 以逗号分隔字符串
     * @param list
     * @return
     */
    public static String splitIntegerWithComma(ArrayList<Integer> list){
        if(list.isEmpty())
            return null;

        StringBuilder sb = new StringBuilder();
        for(Integer item : list){
            sb.append(item).append(",");
        }
        sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }


    public static Object getValueFromObject(Object object,String fieldName) {
        Field field;
        try {
            field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }


    private static DecimalFormat format = new DecimalFormat("#");
    public static String roundDownDouble(double value){
        return format.format(Math.floor(value));
    }
    public static String roundUpDouble(double value){
        return format.format(Math.ceil(value));
    }
    public static String roundDouble(double value){
        return format.format(Math.round(value));
    }
    public static String roundIfNeed(double value) {
        if (value % 1 == 0) {
            return roundDouble(value);
        } else {
            return String.valueOf(value);
        }
    }

    private static DecimalFormat format2 = new DecimalFormat("0.0");
    public static String getOneDecimalPlaces(Double value) {
        return format2.format(value);
    }

    public static String getCurrentProcessName() {
        return Thread.currentThread().getName();
    }

}
