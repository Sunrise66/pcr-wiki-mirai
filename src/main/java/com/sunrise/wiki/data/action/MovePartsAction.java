package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

public class MovePartsAction extends ActionParameter {
    @Override
    protected void childInit() {
        super.childInit();
    }

    @Override
    public String localizedDetail(int level, Property property) {
        return I18N.getString("Move_Part_d1_d2_forward_then_return",
                (int)actionValue4, (int)-actionValue1);
    }
}
