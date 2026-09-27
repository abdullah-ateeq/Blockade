package com.example.blockade

import android.app.Application
import com.example.blockade.data.BlockadeRepository
import com.example.blockade.feedback.SoundPlayer

class BlockadeApplication : Application() {
    val repository: BlockadeRepository by lazy { BlockadeRepository(this) }
    val soundPlayer: SoundPlayer by lazy { SoundPlayer(this) }
}
