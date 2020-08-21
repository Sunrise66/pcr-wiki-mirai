package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;
import com.sunrise.wiki.utils.Utils;

public class SealAction extends ActionParameter {
    @Override
    protected void childInit() {
        super.childInit();
    }

    @Override
    public String localizedDetail(int level, Property property) {
        if(actionValue4 >= 0)
            return I18N.getString("Add_s1_mark_stacks_max_s2_ID_s3_on_s4_for_s5_sec",
                    Utils.roundDownDouble(actionValue4),
                    Utils.roundDownDouble(actionValue1),
                    Utils.roundDownDouble(actionValue2),
                    targetParameter.buildTargetClause(),
                    Utils.roundDouble(actionValue3));
        else
            return I18N.getString("Remove_s1_mark_stacks_ID_s2_on_s3",
                    Utils.roundDownDouble(-actionValue4),
                    Utils.roundDownDouble(actionValue2),
                    targetParameter.buildTargetClause());
    }
}
