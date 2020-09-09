package com.sunrise.wiki.messages.impls;

import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.data.AttackPattern;
import com.sunrise.wiki.data.Chara;
import com.sunrise.wiki.data.Property;
import com.sunrise.wiki.data.Skill;
import com.sunrise.wiki.db.CharaHelper;
import com.sunrise.wiki.db.EquipmentStarter;
import com.sunrise.wiki.messages.CharaMessageHelper;
import com.sunrise.wiki.res.values.StringsCN;
import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.sunrise.wiki.utils.ImageUtils.getIconWithPng;
import static com.sunrise.wiki.utils.ImageUtils.mergeSkillImages;

public class CharaMessageHelperImpl implements CharaMessageHelper {

    private EquipmentStarter equipmentStarter;
    private String dataFolder;
    private CharaHelper charaHelper;

    /**
     * 构造方法
     * @param dataFolder 插件的数据文件夹位置
     * @param equipmentStarter
     */
    public CharaMessageHelperImpl(String dataFolder,EquipmentStarter equipmentStarter) {
        this.equipmentStarter = equipmentStarter;
        this.dataFolder = dataFolder;
        this.charaHelper = new CharaHelper();
    }

    @Override
    public Message getCharaInfo(int charaId, GroupMessageEvent event) {
        event.getGroup().sendMessage("正在查询...");
        Chara chara = charaHelper.getFinalChara(charaId, equipmentStarter.getEquipmentMap());
        At at = new At(event.getSender());
        StringBuffer sb = new StringBuffer();
        if (null == chara) {
            return at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
        }
        Image image = getCharaIcon(chara.getPrefabId(), event);
        sb.append(chara.getUnitName()).append("\n");
        sb.append("真名：").append(chara.getActualName()).append("\n");
        sb.append("声优：").append(chara.getVoice()).append("\n");
        sb.append("年龄：").append(chara.getAge()).append("岁").append("\n");
        sb.append("生日：").append(chara.getBirthDate()).append("\n");
        sb.append("身高：").append(chara.getHeight()).append(" cm").append("\n");
        sb.append("体重：").append(chara.getWeight()).append(" kg").append("\n");
        sb.append("血型：").append(chara.getBloodType()).append("型").append("\n");
        sb.append("喜好：").append(chara.getFavorite()).append("\n");
        sb.append("简介：").append(chara.getComment().replace("\\n", "\n")).append("\n\n");
        sb.append("发送 \"角色技能（空格）角色名\"来查询技能").append("\n");
        sb.append("发送 \"角色详情（空格）角色名\"来查询角色详情信息");

        return at.plus("\n").plus(image.plus("\n" + sb.toString()));
    }

    @Override
    public Message getCharaDetails(int charaId, GroupMessageEvent event) {
        event.getGroup().sendMessage("正在查询...");
        Chara chara = charaHelper.getFinalChara(charaId, equipmentStarter.getEquipmentMap());
        At at = new At(event.getSender());
        if (null == chara) {
            return at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
        }
        Image charaIcon = getCharaIcon(charaId, event);
        Property charaProperty = chara.getCharaProperty();
        List<Message> messages = new ArrayList<>();
        messages.add(at);
        messages.add(charaIcon);
        messages.add(new PlainText(chara.getUnitName() + "\n"));
        messages.add(new PlainText("角色状态:-->\n"));
        messages.add(new PlainText("rank：" + chara.getMaxCharaRank() + "  " + "Level：" + chara.getMaxCharaLevel() + "\n"));
        messages.add(new PlainText("======================\n"));
        messages.add(new PlainText(String.format(StringsCN.text_normal_attack_cast_time, chara.getNormalAtkCastTime()) + "\n"));
        messages.add(new PlainText(StringsCN.text_physical_atk + " " + charaProperty.getAtk() + "\n" + StringsCN.text_magical_atk + " " + charaProperty.getMagicStr() + "\n"));
        messages.add(new PlainText(StringsCN.text_physical_crt + " " + charaProperty.getPhysicalCritical() + "\n" + StringsCN.text_magical_crt + " " + charaProperty.getMagicCritical() + "\n"));
        messages.add(new PlainText(StringsCN.text_physical_def + " " + charaProperty.getDef() + "\n" + StringsCN.text_magical_def + " " + charaProperty.getMagicDef() + "\n"));
        messages.add(new PlainText(StringsCN.text_hp + " " + charaProperty.getHp() + "\n" + StringsCN.text_life_steal + " " + charaProperty.getLifeSteal() + "\n"));
        messages.add(new PlainText(StringsCN.text_energy_recovery + " " + charaProperty.getEnergyRecoveryRate() + "\n" + StringsCN.text_energy_reduce + " " + charaProperty.getEnergyReduceRate() + "\n"));
        messages.add(new PlainText(StringsCN.text_accuracy + " " + charaProperty.getAccuracy() + "\n" + StringsCN.text_dodge + " " + charaProperty.getDodge() + "\n"));
        messages.add(new PlainText(StringsCN.text_wave_hp_recovery + " " + charaProperty.getWaveHpRecovery() + "\n" + StringsCN.text_wave_energy_recovery + " " + charaProperty.getWaveEnergyRecovery() + "\n"));
        messages.add(new PlainText(StringsCN.text_physical_penetrate + " " + charaProperty.getPhysicalPenetrate() + "\n" + StringsCN.text_magical_penetrate + " " + charaProperty.getMagicPenetrate() + "\n"));
        messages.add(new PlainText(StringsCN.text_hp_recovery + " " + charaProperty.getHpRecovery() + "\n"));
        messages.add(new PlainText("======================\n"));

        return MessageUtils.newChain(messages);
    }

    @Override
    public Message getCharaSkills(int charaId, GroupMessageEvent event) {
        event.getGroup().sendMessage("正在查询...");
        Image charaIcon = getCharaIcon(charaId, event);
        Image ubIcon;
        Image s1Icon;
        Image s1pIcon;
        Image s2Icon;
        Image s2pIcon;
        Image exIcon;
        Image expIcon;
        At at = new At(event.getSender());
        //获取角色对象，以获得更多信息
        Chara chara = charaHelper.getFinalChara(charaId, equipmentStarter.getEquipmentMap());
        if (null == chara) {
            return at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
        }
        List<Message> messages = new ArrayList<>();
        messages.add(at);
        messages.add(charaIcon);
        messages.add(new PlainText("角色状态 -> \nRank：" + chara.getMaxCharaRank() + " Level：" + chara.getMaxCharaLevel() + "\n"));
        messages.add(new PlainText("技能循环：\n"));
        List<AttackPattern> attackPatternList = chara.getAttackPatternList();
        List<BufferedImage> loopImages = new ArrayList<>();
        List<String> loopStrs = new ArrayList<>();
        List<String> skillNames = new ArrayList<>();
        int index = 0;
        for (AttackPattern pattern : attackPatternList) {
            for (AttackPattern.AttackPatternItem item : pattern.items) {
                BufferedImage icon = getLoopIcon(index, charaId, item.iconUrl);
                loopImages.add(icon);
                loopStrs.add(item.loopText);
                skillNames.add(item.skillText);
                index++;
            }
        }
        index = 0;
        BufferedImage skillImages = mergeSkillImages(loopImages, loopStrs, skillNames);
        if (null != skillImages) {
            messages.add(event.getGroup().uploadImage(skillImages));
        }
        List<Skill> skills = chara.getSkills();
        for (Skill skill : skills) {
            if (skill.getSkillClass().getValue().equals(StringsCN.union_burst)) {
                ubIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(skill.getSkillClass().getValue() + "\n"));
                messages.add(ubIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("M1")) {
                s1Icon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.main_skill_1 + "\n"));
                messages.add(s1Icon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("M1+")) {
                s1pIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.main_skill_1_evo + "\n"));
                messages.add(s1pIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("M2")) {
                s2Icon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.main_skill_2 + "\n"));
                messages.add(s2Icon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("M2+")) {
                s2pIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.main_skill_2_evo + "\n"));
                messages.add(s2pIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("E1")) {
                exIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.ex_skill_1 + "\n"));
                messages.add(exIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("E1+")) {
                expIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.ex_skill_1_evo + "\n"));
                messages.add(expIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
        }

        return MessageUtils.newChain(messages);
    }

    @Override
    public Message getCharaEquipment(int charaId, GroupMessageEvent event) {
        return null;
    }


    private Image getSkillIcon(int unitId, int skillId, String iconUrl, GroupMessageEvent event) {
        File skillIconPath = new File(dataFolder + File.separator + "images" + File.separator + "skillIcons" + File.separator + unitId);
        File png = new File(dataFolder + File.separator + "images" + File.separator + "skillIcons" + File.separator + unitId + File.separator + skillId + ".png");
        return getIconWithPng(iconUrl, skillIconPath, png, 0.5, event);
    }

    /**
     * 获取角色头像
     *
     * @param prefab_id 头像id
     * @param event
     * @return
     */
    private Image getCharaIcon(int prefab_id, GroupMessageEvent event) {
        File unitIconsPath = new File(dataFolder + File.separator + "images" + File.separator + "unitIcons");
        File png = new File(dataFolder + File.separator + "images" + File.separator + "unitIcons" + File.separator + prefab_id + 30 + ".png");
        String iconUrl = String.format(Locale.US, Statics.ICON_URL, prefab_id + 30);
        return getIconWithPng(iconUrl, unitIconsPath, png, 1, event);
    }

    private BufferedImage getLoopIcon(int index, int charaId, String iconUrl) {
        File loopIconPath = new File(dataFolder + File.separator + "images" + File.separator + "loopIcons" + File.separator + charaId);
        File png = new File(dataFolder+ File.separator + "images" + File.separator + "loopIcons" + File.separator + charaId + File.separator + index + ".png");
        return getIconWithPng(iconUrl, loopIconPath, png, 0.5);
    }

}
