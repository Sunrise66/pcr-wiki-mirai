package com.sunrise.wiki.data;


import com.sunrise.wiki.common.I18N;

import java.util.ArrayList;
import java.util.List;

public enum PropertyKey {
    atk,
    def,
    dodge,
    energyRecoveryRate,
    energyReduceRate,
    hp,
    hpRecoveryRate,
    lifeSteal,
    magicCritical,
    magicDef,
    magicPenetrate,
    magicStr,
    physicalCritical,
    physicalPenetrate,
    waveEnergyRecovery,
    waveHpRecovery,
    accuracy,
    unknown;

    public List<PropertyKey> getKeys(){
        List<PropertyKey> all = new ArrayList<>();
        all.add(atk);
        all.add(def);
        all.add(dodge);
        all.add(energyRecoveryRate);
        all.add(energyReduceRate);
        all.add(hp);
        all.add(hpRecoveryRate);
        all.add(lifeSteal);
        all.add(magicCritical);
        all.add(magicDef);
        all.add(magicPenetrate);
        all.add(magicStr);
        all.add(physicalCritical);
        all.add(physicalPenetrate);
        all.add(waveEnergyRecovery);
        all.add(waveHpRecovery);
        all.add(accuracy);
        return all;
    }


    public String description(){
        switch (this){
            case atk: return I18N.getString("ATK");
            case def: return I18N.getString("DEF");
            case dodge: return I18N.getString("Dodge");
            case energyRecoveryRate: return I18N.getString("Energy_Recovery_Rate");
            case energyReduceRate: return I18N.getString("Energy_Reduce_Rate");
            case hp: return I18N.getString("HP");
            case hpRecoveryRate: return I18N.getString("HP_Recovery_Rate");
            case lifeSteal: return I18N.getString("Life_Steal");
            case magicCritical: return I18N.getString("Magic_Critical");
            case magicDef: return I18N.getString("Magic_DEF");
            case magicPenetrate: return I18N.getString("Magic_Penetrate");
            case magicStr: return I18N.getString("Magic_STR");
            case physicalCritical: return I18N.getString("Physical_Critical");
            case physicalPenetrate: return I18N.getString("Physical_Penetrate");
            case waveEnergyRecovery: return I18N.getString("Wave_Energy_Recovery");
            case waveHpRecovery: return I18N.getString("Wave_HP_Recovery");
            case accuracy: return I18N.getString("Accuracy");
            default: return I18N.getString("Unknown");
        }
    }
}
