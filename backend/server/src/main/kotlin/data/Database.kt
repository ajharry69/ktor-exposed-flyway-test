package co.ke.xently.data

import MigrationUtils
import co.ke.xently.UserService.Users
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.vendors.currentDialect
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.random.Random


private fun Path.path(vararg paths: String): String {
    val separator = fileSystem.separator
    return listOf(*paths).joinToString(
        prefix = separator,
        postfix = separator,
        separator = separator,
    )
}

@OptIn(ExperimentalDatabaseMigrationApi::class)
fun Application.runFlywayMigrations(): Database {
    val flywayMigrationPath = Path("db", "migrations").pathString
    val migrationDirectory = environment.classLoader.getResources(flywayMigrationPath)
        .asSequence()
        .firstOrNull()
        ?.toURI()
        ?.toPath()?.run {
            log.info("Migrating $this")
            val path = absolutePathString()
                .plus(fileSystem.separator)
                .replace(path("build", "resources"), path("src"))
                .replace(path(flywayMigrationPath), path("resources", flywayMigrationPath))
            Path(path = path)
        } ?: throw IllegalStateException("Cannot find $flywayMigrationPath in your resources directory")

    log.info("Running FlywayMigration in: $migrationDirectory")

    if (migrationDirectory.notExists()) {
        log.info("Creating migration directory in '${migrationDirectory}'...")
        migrationDirectory.createDirectories()
    }

    val url = environment.config.property("postgres.url").getString()
    val username = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()

    val flyway = Flyway.configure()
        .dataSource(url, username, password)
        .locations("filesystem:${migrationDirectory}")
        .baselineOnMigrate(true) // Used when migrating an existing database for the first time
        .load()

    val database = Database.connect(
        url = url,
        user = username,
        password = password,
    )

    transaction(database) {
        log.info("*** Before migration ***")
        log.info("Primary key: ${currentDialect.existingPrimaryKeys(Users)[Users]}")

        MigrationUtils.generateMigrationScript(
            Users,
            scriptDirectory = migrationDirectory.pathString,
//            scriptName = "V1__create_users_table",
            scriptName = "V2__add_dob",
        )
    }

    transaction(database) {
        // This can be commented out to review the generated migration script before applying a migration
        flyway.migrate()
    }

    transaction(database) {
        log.info("*** After migration ***")
        log.info("Primary key: ${currentDialect.existingPrimaryKeys(Users)[Users]}")

        Users.insert {
            it[this.age] = Random.nextInt(1, 100)
            it[this.name] = "John Doe ${Random.nextInt(1, 100)}"
        }
    }

    return database
}
