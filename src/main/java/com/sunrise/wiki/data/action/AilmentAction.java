package com.sunrise.wiki.data.action;



import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Ailment;
import com.sunrise.wiki.data.Property;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class AilmentAction extends ActionParameter {

    private Ailment ailment;
    private List<ActionValue> chanceValues = new ArrayList<>();
    private List<ActionValue> durationValues;

    @Override
    protected void childInit() {
        ailment = new Ailment(rawActionType, actionDetail1);
        actionValues.add(new ActionValue(actionValue1, actionValue2, null));
        chanceValues.add(new ActionValue(actionValue3, actionValue4, null));
        durationValues = chanceValues;
    }

    @Override
    public String localizedDetail(int level, Property property) {
        switch (ailment.ailmentType){
            case action:
                String str;
                switch ((Ailment.ActionDetail)ailment.ailmentDetail.detail){
                    case haste:
                        str = I18N.getString("Raise_s1_d2_attack_speed_for_s3_sec",
                                targetParameter.buildTargetClause(), Math.round((actionValue1 - 1) * 100), buildExpression(level, durationValues, RoundingMode.UNNECESSARY, property));
                        break;
                    case slow:
                        str = I18N.getString("Reduce_s1_d2_attack_speed_for_s3_sec",
                                targetParameter.buildTargetClause(), Math.round((1 - actionValue1) * 100), buildExpression(level, durationValues, RoundingMode.UNNECESSARY, property));
                        break;
                    case sleep:
                        str = I18N.getString("Make_s1_fall_asleep_for_s2_sec",
                                targetParameter.buildTargetClause(), buildExpression(level, durationValues, RoundingMode.UNNECESSARY, property));
                        break;
                    case faint:
                        str = I18N.getString("Make_s1_fall_into_faint_for_s2_sec",
                                targetParameter.buildTargetClause(), buildExpression(level, durationValues, RoundingMode.UNNECESSARY, property));
                        break;
                    case timeStop:
                        str = I18N.getString("Stop_s1_for_s2_sec",
                                targetParameter.buildTargetClause(), buildExpression(level, durationValues, RoundingMode.UNNECESSARY, property));
                        break;
                    default:
                        str = I18N.getString("s1_s2_for_s3_sec",
                                ailment.description(), targetParameter.buildTargetClause(), buildExpression(level, durationValues, RoundingMode.UNNECESSARY, property));
                        break;
                }
                if (actionDetail2 == 1) {
                    str += I18N.getString("This_effect_will_be_released_when_taking_damaged");
                }
                return str;
            case dot:
                switch ((Ailment.DotDetail)ailment.ailmentDetail.detail){
                    case poison:
                        return I18N.getString("Poison_s1_and_deal_s2_damage_per_second_for_s3_sec",
                                targetParameter.buildTargetClause(), buildExpression(level, RoundingMode.UP, property), buildExpression(level, durationValues, RoundingMode.HALF_UP, property));
                    case violentPoison:
                        return I18N.getString("Poison_s1_violently_and_deal_s2_damage_per_second_for_s3_sec",
                                targetParameter.buildTargetClause(), buildExpression(level, RoundingMode.UP, property), buildExpression(level, durationValues, RoundingMode.HALF_UP, property));
                    default:
                        return I18N.getString("s1_s2_and_deal_s3_damage_per_second_for_s4_sec",
                                ailment.description(), targetParameter.buildTargetClause(), buildExpression(level, RoundingMode.UP, property), buildExpression(level, durationValues, RoundingMode.HALF_UP, property));
                }
            case silence:
                return I18N.getString("Silence_s1_with_s2_chance_for_s3_sec",
                        targetParameter.buildTargetClause(), buildExpression(level, chanceValues, RoundingMode.UNNECESSARY, property), buildExpression(level, property));
            case darken:
                return I18N.getString("Blind_s1_with_s2_chance_for_s3_sec_physical_attack_has_d4_chance_to_miss",
                        targetParameter.buildTargetClause(), buildExpression(level, chanceValues, RoundingMode.UNNECESSARY, property), buildExpression(level, RoundingMode.UNNECESSARY, property), 100 - actionDetail1);
            default:
                return super.localizedDetail(level, property);
        }
    }
}
