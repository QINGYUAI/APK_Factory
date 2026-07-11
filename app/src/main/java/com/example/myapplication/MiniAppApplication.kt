// Author: PKQ32
// Created: 2026-07-11
// Description: Application 入口，初始化 ServiceLocator

package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.ServiceLocator

class MiniAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
