package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

public class IgnoreDecoyAction extends ActionParameter {
    @Override
    protected void childInit() {
        super.childInit();
    }

    @Override
    public String localizedDetail(int level, Property property) {
        return I18N.getString("Ignore_the_other_units_taunt_when_attacking_s", targetParameter.buildTargetClause());
    }
}
