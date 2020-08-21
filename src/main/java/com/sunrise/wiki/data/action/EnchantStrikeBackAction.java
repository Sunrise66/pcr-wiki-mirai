package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

public class EnchantStrikeBackAction extends BarrierAction {
    @Override
    protected void childInit() {
        super.childInit();
    }

    @Override
    public String localizedDetail(int level, Property property) {
        switch (barrierType){
            case physicalGuard:
                return I18N.getString("Cast_a_barrier_on_s1_to_strike_back_s2_damage_when_taking_physical_damage",
                        targetParameter.buildTargetClause(), buildExpression(level, property));
            case magicalGuard:
                return I18N.getString("Cast_a_barrier_on_s1_to_strike_back_s2_damage_when_taking_magical_damage",
                        targetParameter.buildTargetClause(), buildExpression(level, property));
            case physicalDrain:
                return I18N.getString("Cast_a_barrier_on_s1_to_strike_back_s2_damage_and_recover_the_same_HP_when_taking_physical_damage",
                        targetParameter.buildTargetClause(), buildExpression(level, property));
            case magicalDrain:
                return I18N.getString("Cast_a_barrier_on_s1_to_strike_back_s2_damage_and_recover_the_same_HP_when_taking_magical_damage",
                        targetParameter.buildTargetClause(), buildExpression(level, property));
            case bothGuard:
                return I18N.getString("Cast_a_barrier_on_s1_to_strike_back_s2_damage_when_taking_damage",
                        targetParameter.buildTargetClause(), buildExpression(level, property));
            case bothDrain:
                return I18N.getString("Cast_a_barrier_on_s1_to_strike_back_s2_damage_and_recover_the_same_HP_when_taking_damage",
                        targetParameter.buildTargetClause(), buildExpression(level, property));
            default:
                return super.localizedDetail(level, property);
        }
    }
}
