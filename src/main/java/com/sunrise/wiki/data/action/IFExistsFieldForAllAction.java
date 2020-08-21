package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

public class IFExistsFieldForAllAction extends ActionParameter {
    @Override
    protected void childInit() {
        super.childInit();
    }

    @Override
    public String localizedDetail(int level, Property property) {
        if(actionDetail2 !=0 && actionDetail3 != 0)
            return I18N.getString("Condition_if_the_specific_field_exists_then_use_d1_otherwise_d2",
                    actionDetail2 % 10, actionDetail3 % 10);
        else if(actionDetail2 != 0)
            return I18N.getString("Condition_if_the_specific_field_exists_then_use_d",
                    actionDetail2 % 10);
        else
            return super.localizedDetail(level, property);
    }
}
