package com.tickaroo.tikxml.sample.rss.converter;

import com.tickaroo.tikxml.TypeConverter;

/**
 * Created by WeaponMan on 6/19/2017.
 */
public class BooleanConverter implements TypeConverter<Boolean> {
    @Override
    public Boolean read(String value) throws Exception {
        if (value == null)
            return false;

        switch (value) {
            case "true":
            case "1":
            case "yes":
                return true;
            default:
                return false;
        }
    }

    @Override
    public String write(Boolean value) throws Exception {
        return value != null && value ? "true" : "false";
    }
}
