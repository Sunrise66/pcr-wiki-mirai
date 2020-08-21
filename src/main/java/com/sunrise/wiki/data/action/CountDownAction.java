package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

public class CountDownAction extends ActionParameter {
    @Override
    protected void childInit() {
        super.childInit();
    }

    @Override
    public String localizedDetail(int level, Property property) {
        return I18N.getString("Set_a_countdown_timer_on_s1_trigger_effect_d2_after_s3_sec",
                targetParameter.buildTargetClause(), actionDetail1 % 10, actionValue1);
    }
}
