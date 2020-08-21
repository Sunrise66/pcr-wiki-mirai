package com.sunrise.wiki.data.action;



import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ToadAction extends ActionParameter {

    protected List<ActionValue> durationValues = new ArrayList<>();

    @Override
    protected void childInit() {
        super.childInit();
        durationValues.add(new ActionValue(actionValue1, actionValue2, null));
    }

    @Override
    public String localizedDetail(int level, Property property) {
        return I18N.getString("Polymorph_s1_for_s2_sec",
                targetParameter.buildTargetClause(),
                buildExpression(level, durationValues, RoundingMode.UNNECESSARY, property));
    }
}
