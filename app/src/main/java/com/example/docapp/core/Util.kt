package com.example.docapp.core

import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
fun now(): Long = System.currentTimeMillis()
