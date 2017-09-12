import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.events.ProgressListener
import org.gradle.tooling.events.internal.DefaultFinishEvent
import org.gradle.tooling.events.internal.DefaultStartEvent
import org.gradle.tooling.events.task.internal.DefaultTaskFinishEvent
import org.gradle.tooling.events.task.internal.DefaultTaskStartEvent
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.util.*

val benchmarkSubject = File("${System.getProperty("user.home")}/work/benchmark_subject")
val benchmarkName = "with_stub"
val launchNumber = 5
val tasks = listOf("clean", "assembleDevDebug")

val gradleOutputFile = File("benchmark_results/${benchmarkName}_gradle_output.txt")
val resultsFile = File("benchmark_results/${benchmarkName}_result.txt")

val printerWriter get() = PrintWriter(FileOutputStream(resultsFile, true))

fun main(args: Array<String>) {
    File("benchmark_results").apply {
        if(!exists()) mkdir()
    }

    if(gradleOutputFile.exists()) {
        gradleOutputFile.delete()
    }

    gradleOutputFile.createNewFile()

    if(resultsFile.exists()) {
        resultsFile.delete()
    }
    resultsFile.createNewFile()

    printerWriter.use { logOut ->
        logOut.apply {
            appendln("Project dir   = $benchmarkSubject")
            appendln("Launch number = $launchNumber")
            appendln("Tasks         = $tasks")
            appendln("Name          = $benchmarkName")
            appendln("Date          = ${Date()}")
            appendln()
        }
    }

    val connection = GradleConnector.newConnector().forProjectDirectory(benchmarkSubject).connect()

    try {
        (1..launchNumber).asSequence().map {
            println("Launching № $it...")
            runTasks(*tasks.toTypedArray(), connection = connection, output = FileOutputStream(gradleOutputFile, true))
        }.forEachIndexed { i, (beforeFirstTask, upload, execute, download, total) ->

            fun format(long: Long) = "${long / 1000.0} secs"

            printerWriter.use { logOut ->
                logOut.apply {
                    appendln("----- LAUNCH № ${i + 1} -----")
                    appendln("Init     : ${format(beforeFirstTask)}")
                    appendln("Upload   : ${format(upload)}")
                    appendln("Execute  : ${format(execute)}")
                    appendln("Download : ${format(download)}")
                    appendln("Total    : ${format(total)}")
                    appendln("-----------------------")
                    appendln()
                }
            }
        }
    } finally {
        connection.close()
    }
}

fun runTasks(vararg tasks: String, connection: ProjectConnection, output: OutputStream): LogEntry {
    var buildStarted: Long = 0
    var buildFinished: Long = 0

    var firstTaskStarted: Long = 0

    var uploadStarted: Long = 0
    var uploadFinished: Long = 0

    var executeStarted: Long = 0
    var executeFinished: Long = 0

    var downloadStarted: Long = 0
    var downloadFinished: Long = 0

    connection.newBuild()
            .forTasks(*tasks)
            .setStandardOutput(output)
            .setStandardError(output)
            .addProgressListener(ProgressListener { event ->
                when (event::class.java) {
                    DefaultStartEvent::class.java -> {
                        when (event.displayName) {
                            "Run build started" -> buildStarted = event.eventTime
                            "Run tasks started" -> firstTaskStarted = event.eventTime
                        }
                    }

                    DefaultFinishEvent::class.java -> {
                        when (event.displayName) {
                            "Run build succeeded" -> buildFinished = event.eventTime
                        }
                    }

                    DefaultTaskStartEvent::class.java -> {
                        when (event.displayName) {
                            "Task :uploadToRemote started" -> uploadStarted = event.eventTime
                            "Task :executeOnRemote started" -> executeStarted = event.eventTime
                            "Task :downloadFromRemote started" -> downloadStarted = event.eventTime
                        }
                    }

                    DefaultTaskFinishEvent::class.java -> {
                        when (event.displayName) {
                            "Task :uploadToRemote SUCCESS" -> uploadFinished = event.eventTime
                            "Task :executeOnRemote SUCCESS" -> executeFinished = event.eventTime
                            "Task :downloadFromRemote SUCCESS" -> downloadFinished = event.eventTime
                        }
                    }
                }
            })
            .run()

    return LogEntry(
            beforeFirstTask = firstTaskStarted - buildStarted,
            upload = uploadFinished - uploadStarted,
            execute = executeFinished - executeStarted,
            download = downloadFinished - downloadStarted,
            total = buildFinished - buildStarted)
}

data class LogEntry(val beforeFirstTask: Long,
                    val upload: Long,
                    val execute: Long,
                    val download: Long,
                    val total: Long)