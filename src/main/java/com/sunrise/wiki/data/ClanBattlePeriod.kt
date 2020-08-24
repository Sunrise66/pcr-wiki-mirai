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
        1 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_aquarious.png").path))
        2 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_pisces.png").path))
        3 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_aries.png").path))
        4 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_taurus.png").path))
        5 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_gemini.png").path))
        6 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_cancer.png").path))
        7 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_leo.png").path))
        8 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_virgo.png").path))
        9 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_libra.png").path))
        10 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_scorpio.png").path))
        11 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_sagittarious.png").path))
        12 ->ImageIO.read(File(this.javaClass.classLoader.getResource("zodiac_capricorn.png").path))
        else -> ImageIO.read(File(this.javaClass.classLoader.getResource("mic_chara_icon_place_holder.png").path))
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