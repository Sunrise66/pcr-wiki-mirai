package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;
import com.sunrise.wiki.utils.Utils;

public class ChangePatternAction extends ActionParameter {
    @Override
    protected void childInit() {
        super.childInit();
    }

    @Override
    public String localizedDetail(int level, Property property) {
        switch (actionDetail1){
            case 1:
                if (actionValue1 > 0) {
                    return I18N.getString("Change_attack_pattern_to_d1_for_s2_sec",
                            actionDetail2 % 10, Utils.roundDouble(actionValue1));
                } else {
                    return I18N.getString("Change_attack_pattern_to_d",
                            actionDetail2 % 10);
                }
            case 2:
                return I18N.getString("Change_skill_visual_effect_for_s_sec",
                        Utils.roundDouble(actionValue1));
            default:
                return super.localizedDetail(level, property);
        }
    }
}
