package com.github.malitsplus.shizurunotes.data

import com.sunrise.wiki.db.DBHelper
import java.awt.Image
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    val zodiacImage: Image = when(startTime.monthValue){
        1 ->ImageIO.read(File("src\\main\\resources\\zodiac_aquarious.webp"))
        2 ->ImageIO.read(File("src\\main\\resources\\zodiac_pisces.webp"))
        3 ->ImageIO.read(File("src\\main\\resources\\zodiac_aries.webp"))
        4 ->ImageIO.read(File("src\\main\\resources\\zodiac_taurus.webp"))
        5 ->ImageIO.read(File("src\\main\\resources\\zodiac_gemini.webp"))
        6 ->ImageIO.read(File("src\\main\\resources\\zodiac_cancer.webp"))
        7 ->ImageIO.read(File("src\\main\\resources\\zodiac_leo.webp"))
        8 ->ImageIO.read(File("src\\main\\resources\\zodiac_virgo.webp"))
        9 ->ImageIO.read(File("src\\main\\resources\\zodiac_libra.webp"))
        10 ->ImageIO.read(File("src\\main\\resources\\zodiac_scorpio.webp"))
        11 ->ImageIO.read(File("src\\main\\resources\\zodiac_sagittarious.webp"))
        12 ->ImageIO.read(File("src\\main\\resources\\zodiac_capricorn.webp"))
        else -> ImageIO.read(File("src\\main\\resources\\mic_chara_icon_error.webp"))
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