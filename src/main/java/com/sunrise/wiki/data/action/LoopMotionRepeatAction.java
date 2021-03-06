package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

public class LoopMotionRepeatAction extends ActionParameter {

    protected String successClause;
    protected String failureClause;

    @Override
    protected void childInit() {
        super.childInit();
        if(actionDetail2 != 0)
            successClause = I18N.getString("use_d_after_time_up", actionDetail2 % 10);
        if(actionDetail3 != 0)
            failureClause = I18N.getString("use_d_after_break", actionDetail3 % 10);
    }

    @Override
    public String localizedDetail(int level, Property property) {
        String mainClause = I18N.getString("Repeat_effect_d1_every_s2_sec_up_to_s3_sec_break_if_taken_more_than_s4_damage",
                actionDetail1 % 10, actionValue2, actionValue1, actionValue3);
        if(successClause != null && failureClause != null)
            return mainClause + successClause + failureClause;
        else if(successClause != null)
            return mainClause + successClause;
        else if(failureClause != null)
            return mainClause + failureClause;
        else
            return mainClause;
    }
}
