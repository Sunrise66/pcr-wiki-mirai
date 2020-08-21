package com.sunrise.wiki.data;


import com.sunrise.wiki.common.I18N;

public class Ailment {

    public class AilmentDetail{
        public Object detail;
        public void setDetail(Object obj){
            this.detail = obj;
        }

        public String description(){
            if(detail instanceof DotDetail){
                return ((DotDetail)detail).description();
            } else if(detail instanceof ActionDetail){
                return ((ActionDetail)detail).description();
            } else if(detail instanceof CharmDetail){
                return ((CharmDetail)detail).description();
            } else {
                return I18N.getString("Unknown");
            }
        }
    }

    public enum DotDetail {
        detain(0),
        poison(1),
        burn(2),
        curse(3),
        violentPoison(4),
        hex(5),
        unknown(-1);

        private int value;
        DotDetail(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }

        public static DotDetail parse(int value){
            for(DotDetail item : DotDetail.values()){
                if(item.getValue() == value)
                    return item;
            }
            return unknown;
        }

        public String description(){
            switch (this){
                case detain:
                    return I18N.getString("Detain_Damage");
                case poison:
                    return I18N.getString("Poison");
                case burn:
                    return I18N.getString("Burn");
                case curse:
                    return I18N.getString("Curse");
                case violentPoison:
                    return I18N.getString("Violent_Poison");
                case hex:
                    return I18N.getString("Hex");
                default:
                    return I18N.getString("Unknown");
            }
        }
    }

    public enum CharmDetail{
        charm(0),
        confuse(1);

        private int value;
        CharmDetail(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }

        public static CharmDetail parse(int value){
            for(CharmDetail item : CharmDetail.values()){
                if(item.getValue() == value)
                    return item;
            }
            return null;
        }

        public String description(){
            switch (this){
                case charm:
                    return I18N.getString("Charm");
                case confuse:
                    return I18N.getString("Confuse");
                default:
                    return I18N.getString("Unknown");
            }
        }
    }

    public enum ActionDetail{
        slow(1),
        haste(2),
        paralyse(3),
        freeze(4),
        bind(5),
        sleep(6),
        stun(7),
        petrify(8),
        detain(9),
        faint(10),
        timeStop(11),
        unknown(12);

        private int value;
        ActionDetail(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }

        public static ActionDetail parse(int value){
            for(ActionDetail item : ActionDetail.values()){
                if(item.getValue() == value)
                    return item;
            }
            return unknown;
        }

        public String description(){
            switch (this){
                case slow:
                    return I18N.getString("Slow");
                case haste:
                    return I18N.getString("Haste");
                case paralyse:
                    return I18N.getString("Paralyse");
                case freeze:
                    return I18N.getString("Freeze");
                case bind:
                    return I18N.getString("Bind");
                case sleep:
                    return I18N.getString("Sleep");
                case stun:
                    return I18N.getString("Stun");
                case petrify:
                    return I18N.getString("Petrify");
                case detain:
                    return I18N.getString("Detain");
                case faint:
                    return I18N.getString("Faint");
                case timeStop:
                    return I18N.getString("time_stop");
                default:
                    return I18N.getString("Unknown");
            }
        }
    }

    public enum AilmentType{
        knockBack(3),
        action(8),
        dot(9),
        charm(11),
        darken(12),
        silence(13),
        confuse(19),
        instantDeath(30),
        countBlind(56),
        inhibitHeal(59),
        attackSeal(60),
        fear(61),
        awe(62),
        toad(69),
        maxHP(70),
        unknown(71);

        private int value;
        AilmentType(int value){
            this.value = value;
        }
        public int getValue(){
            return value;
        }

        public static AilmentType parse(int value){
            for(AilmentType item : AilmentType.values()){
                if(item.getValue() == value)
                    return item;
            }
            return unknown;
        }

        public String description(){
            switch (this){
                case knockBack:
                    return I18N.getString("Knock_Back");
                case action:
                    return I18N.getString("Action");
                case dot:
                    return I18N.getString("Dot");
                case charm:
                    return I18N.getString("Charm");
                case darken:
                    return I18N.getString("Blind");
                case silence:
                    return I18N.getString("Silence");
                case instantDeath:
                    return I18N.getString("Instant_Death");
                case confuse:
                    return I18N.getString("Confuse");
                case countBlind:
                    return I18N.getString("Count_Blind");
                case inhibitHeal:
                    return I18N.getString("Inhibit_Heal");
                case fear:
                    return I18N.getString("Fear");
                case attackSeal:
                    return I18N.getString("Seal");
                case awe:
                    return I18N.getString("Awe");
                case toad:
                    return I18N.getString("Polymorph");
                case maxHP:
                    return I18N.getString("Changing_Max_HP");
                default:
                    return I18N.getString("Unknown_Effect");
            }
        }
    }

    public AilmentType ailmentType;
    public AilmentDetail ailmentDetail;

    public Ailment(int type, int detail){

        ailmentType = AilmentType.parse(type);
        ailmentDetail = new AilmentDetail();
        switch (ailmentType){
            case action:
                ailmentDetail.setDetail(ActionDetail.parse(detail));
                break;
            case dot:
                ailmentDetail.setDetail(DotDetail.parse(detail));
                break;
            case charm:
                ailmentDetail.setDetail(CharmDetail.parse(detail));
                break;
            default:
                ailmentDetail = null;
                break;
        }
    }

    public String description(){
        if(ailmentDetail != null)
            return ailmentDetail.description();
        else
            return ailmentType.description();
    }
}


