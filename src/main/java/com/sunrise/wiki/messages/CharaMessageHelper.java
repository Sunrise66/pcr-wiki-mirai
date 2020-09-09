package com.sunrise.wiki.messages;

import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.data.Message;

public interface CharaMessageHelper {
    Message getCharaInfo(int charaId, GroupMessageEvent event);

    Message getCharaDetails(int charaId, GroupMessageEvent event);

    Message getCharaSkills(int charaId, GroupMessageEvent event);

    Message getCharaEquipment(int charaId, GroupMessageEvent event);
}
