package com.sunrise.wiki.common;

import com.sunrise.wiki.res.values.StringsCN;

import java.lang.reflect.Field;

public class I18N {

    private I18N(){
    }

    public static String getString(String name){
        String value = "";
        try {
            Class res = Class.forName("com.sunrise.wiki.res.values.StringsCN");
            Field[] fields = res.getFields();
            for(Field f : fields){
                if(name.equals(f.getName())){
                    value = (String) f.get(f.getName());
                }
            }
            return value;
        } catch (ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getStringWithSpace(String name) {
        String value = "";
        try {
            Class res = Class.forName("com.sunrise.wiki.res.values.StringsCN");
            Field[] fields = res.getFields();
            for (Field f : fields) {
                if (name.equals(f.getName())) {
                    value = getString("space_modifier_2",(String)f.get(f.getName()));
                }
            }
            return value;
        } catch (ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getString(String formatName, Object... formatArgs){
        String value = "";
        try {
            Class res = Class.forName("com.sunrise.wiki.res.values.StringsCN");
            Field[] fields = res.getFields();
            for(Field f : fields){
                if(formatName.equals(f.getName())){
                    value = String.format((String) f.get(f.getName()),formatArgs);
                }
            }
            return value;
        } catch (ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
            return "";
        }
    }
}
