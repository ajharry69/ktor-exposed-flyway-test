package co.ke.xently

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureAdministration()
    configureSerialization()
    configureFrameworks()
    configureDatabases()
    configureMonitoring()
    configureHTTP()
    configureRouting()
}
