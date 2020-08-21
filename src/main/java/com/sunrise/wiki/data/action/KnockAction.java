package com.sunrise.wiki.data.action;


import com.sunrise.wiki.common.I18N;
import com.sunrise.wiki.data.Property;

public class KnockAction extends ActionParameter {

    enum KnockType{
        unknown(0),
        upDown(1),
        up(2),
        back(3),
        moveTarget(4),
        moveTargetParaboric(5),
        backLimited(6);

        private int value;
        KnockType(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }

        public static KnockType parse(int value){
            for(KnockType item : KnockType.values()){
                if(item.getValue() == value)
                    return item;
            }
            return unknown;
        }
    }

    private KnockType knockType;

    @Override
    protected void childInit() {
        knockType = KnockType.parse(actionDetail1);
    }

    @Override
    public String localizedDetail(int level, Property property) {
        switch (knockType){
            case upDown:
                return I18N.getString("Knock_s1_up_d2", targetParameter.buildTargetClause(), (int)actionValue1);
            case back:
            case backLimited:
                if(actionValue1 >= 0)
                    return I18N.getString("Knock_s1_away_d2", targetParameter.buildTargetClause(), (int)actionValue1);
                else
                    return I18N.getString("Draw_s1_toward_self_d2", targetParameter.buildTargetClause(), (int)-actionValue1);
            default:
                return super.localizedDetail(level, property);
        }
    }
}
