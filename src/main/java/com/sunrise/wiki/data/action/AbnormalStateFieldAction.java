package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class AbnormalStateFieldAction extends ActionParameter {

    protected List<ActionValue> durationValues = new ArrayList<>();

    @Override
    protected void childInit() {
        super.childInit();
        durationValues.add(new ActionValue(actionValue1, actionValue2, null));
    }

    @Override
    public String localizedDetail(int level, Property property) {
        return I18N.getString("Summon_a_field_of_radius_d1_on_s2_to_cast_effect_d3_for_s4_sec",
                (int)actionValue3,
                targetParameter.buildTargetClause(),
                actionDetail1 % 10,
                buildExpression(level, durationValues, RoundingMode.UNNECESSARY, property));
    }
}
