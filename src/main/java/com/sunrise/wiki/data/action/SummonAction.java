package com.sunrise.wiki.data.action;



import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

import java.util.List;

public class SummonAction extends ActionParameter {

    enum Side{
        unknown(-1),
        ours(1),
        other(2);

        private int value;
        Side(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }

        public static Side parse(int value){
            for(Side item : Side.values()){
                if(item.getValue() == value)
                    return item;
            }
            return unknown;
        }

        public String description(){
            switch (this){
                case ours: return I18N.getString("own_side");
                case other: return I18N.getString("opposite");
                default: return I18N.getString("unknown");
            }
        }
    }

    enum UnitType{
        unknown(-1),
        normal(1),
        phantom(2);

        private int value;
        UnitType(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }

        public static UnitType parse(int value){
            for(UnitType item : UnitType.values()){
                if(item.getValue() == value)
                    return item;
            }
            return unknown;
        }

        public String description(){
            switch (this){
                case normal: return I18N.getString("normal_type");
                case phantom: return I18N.getString("phantom_type");
                default: return I18N.getString("unknown");
            }
        }
    }

    private Side side;
    private UnitType unitType;


    @Override
    protected void childInit() {
        side = Side.parse(actionDetail3);
        unitType = UnitType.parse((int) actionValue5);
    }

    @Override
    public String localizedDetail(int level, Property property) {
        if(actionValue7 > 0){
            return I18N.getString("At_d1_in_front_of_s2_summon_a_minion_id_d3",
                    (int)actionValue7, targetParameter.buildTargetClause(), actionDetail2);
        } else if (actionValue7 < 0){
            return I18N.getString("At_d1_behind_of_s2_summon_a_minion_id_d3",
                    (int)-actionValue7, targetParameter.buildTargetClause(), actionDetail2);
        } else {
            return I18N.getString("At_the_position_of_s1_summon_a_minion_id_d2",
                    targetParameter.buildTargetClause(), actionDetail2);
        }
    }
}
