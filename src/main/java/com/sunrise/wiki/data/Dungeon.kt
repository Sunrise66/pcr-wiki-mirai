package com.sunrise.wiki.data

import com.sunrise.wiki.data.Enemy

class Dungeon(
    val dungeonAreaId: Int,
    val waveGroupId: Int,
    val enemyId: Int,
    val dungeonName: String,
    val description: String,
    val dungeonBoss: Enemy
)