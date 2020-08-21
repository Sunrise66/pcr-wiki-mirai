package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

public class StealthAction extends ActionParameter {
    @Override
    protected void childInit() {
        super.childInit();
    }

    @Override
    public String localizedDetail(int level, Property property) {
        return I18N.getString("Stealth_for_s_sec", actionValue1);
    }
}
