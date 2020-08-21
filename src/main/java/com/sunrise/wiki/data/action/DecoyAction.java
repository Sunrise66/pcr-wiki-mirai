package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

public class DecoyAction extends ActionParameter {
    @Override
    protected void childInit() {
        actionValues.add(new ActionValue(actionValue1, actionValue2, null));
    }

    @Override
    public String localizedDetail(int level, Property property) {
        return I18N.getString("Make_s1_attract_enemy_attacks_last_for_s2_sec",
                targetParameter.buildTargetClause(), buildExpression(level, property));
    }
}
