package com.example.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class MyTask : DefaultTask() {
    @TaskAction
    fun run() {
        println("Custom task executed!")
    }
}

