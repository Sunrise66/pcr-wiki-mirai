package com.sunrise.wiki.common;

import java.util.regex.Pattern;

public class Commands {
    public static String searchCharaCmd = "^查询角色\\s.{0,10}$";
    public static String searchCharaSkillCmd = "(角色技能)(\\s\\S{0,10})(\\s\\S+ {0,10})(\\s\\S+ {0,10})";
    public static Pattern searchCharaSkillPattern = Pattern.compile("角色技能\\s+(?<name1>\\S{0,10}\\s+)(?<rank>\\S+ {0,10})(?<lv>\\s\\S+ {0,10})|角色技能\\s+(?<name2>\\S{0,10}.)|角色技能");
}
