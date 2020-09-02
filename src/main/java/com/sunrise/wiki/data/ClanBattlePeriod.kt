package com.sunrise.wiki.data

import com.sunrise.wiki.db.DBHelper
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

class ClanBattlePeriod(
    val clanBattleId: Int,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
) {

    val periodText: String by lazy {
        val format = "yyyy-MM-dd"
        startTime.format(DateTimeFormatter.ofPattern(format))
    }

    val phaseList = mutableListOf<ClanBattlePhase>().apply {
        DBHelper.get().getClanBattlePhase(clanBattleId)?.forEach {
            this.add(it.clanBattlePhase)
        }
    }

    val iconBoss1 = phaseList[0].bossList[0].iconUrl
    val iconBoss2 = phaseList[0].bossList[1].iconUrl
    val iconBoss3 = phaseList[0].bossList[2].iconUrl
    val iconBoss4 = phaseList[0].bossList[3].iconUrl
    val iconBoss5 = phaseList[0].bossList[4].iconUrl

    val zodiacImage: BufferedImage = when(startTime.monthValue){
        1 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_aquarious.png"))
        2 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_pisces.png"))
        3 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_aries.png"))
        4 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_taurus.png"))
        5 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_gemini.png"))
        6 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_cancer.png"))
        7 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_leo.png"))
        8 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_virgo.png"))
        9 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_libra.png"))
        10 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_scorpio.png"))
        11 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_sagittarious.png"))
        12 ->ImageIO.read(this.javaClass.classLoader.getResourceAsStream("zodiac_capricorn.png"))
        else -> ImageIO.read(this.javaClass.classLoader.getResourceAsStream("mic_chara_icon_place_holder.png"))
    }
//    val zodiacImage: Int? = when(startTime.monthValue){
//        1 -> R.drawable.zodiac_aquarious
//        2 -> R.drawable.zodiac_pisces
//        3 -> R.drawable.zodiac_aries
//        4 -> R.drawable.zodiac_taurus
//        5 -> R.drawable.zodiac_gemini
//        6 -> R.drawable.zodiac_cancer
//        7 -> R.drawable.zodiac_leo
//        8 -> R.drawable.zodiac_virgo
//        9 -> R.drawable.zodiac_libra
//        10 -> R.drawable.zodiac_scorpio
//        11 -> R.drawable.zodiac_sagittarious
//        12 -> R.drawable.zodiac_capricorn
//        else -> R.drawable.mic_chara_icon_place_holder
//    }

}