import org.gradle.BuildAdapter
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import org.gradle.internal.time.Clock
import kotlin.properties.Delegates

fun Gradle.logTasks(vararg task: Task) {
    task.forEach { targetTask ->
        addListener(object : TaskExecutionListener {
            var clock by Delegates.notNull<Clock>()

            override fun beforeExecute(task: Task) {
                if (task == targetTask) {
                    clock = Clock()
                }
            }

            override fun afterExecute(task: Task, state: TaskState) {
                if (task == targetTask) {
                    val elapsed = clock.elapsed
                    buildFinished {
                        println("Task ${task.name} took: $elapsed")
                    }
                }
            }
        })
    }
}

fun Gradle.logBuild(clock: Clock) {
    useLogger(object : BuildAdapter() {})
    buildFinished {
        println("Total time : ${clock.elapsed}")
    }
}

fun Gradle.assertNonSupportedFeatures() {
    if (startParameter.isContinuous) throw MirakleException("--continuous is not supported yet")
    if (startParameter.includedBuilds.isNotEmpty()) throw MirakleException("Included builds is not supported yet")
}

class MirakleException(message: String) : RuntimeException(message)

inline fun <reified T : Task> Project.task(name: String, noinline configuration: T.() -> Unit) =
        tasks.create(name, T::class.java, configuration)!!
