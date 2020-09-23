package com.sunrise.wiki.common;

import java.util.regex.Pattern;

public class Orders {
    public static Pattern searchCharaPrf = Pattern.compile("角色简介(?<name>.{0,10})");
    public static Pattern searchCharaDetail = Pattern.compile("角色详情(?<name>.{0,10})");
    public static Pattern searchCharaSkill = Pattern.compile("角色技能(?<name1>.{0,5})(?<rank>\\sr\\S+ {0,4})(?<lv>\\sl\\S+ {0,4})|角色技能(?<name2>.{0,5})");
}
