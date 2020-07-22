import groovy.lang.Closure
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.internal.AbstractTask
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import org.gradle.internal.service.ServiceRegistry

fun Gradle.logTasks(vararg task: Task) {
    task.forEach { targetTask ->
        addListener(object : TaskExecutionListener {
            var startTime: Long = 0

            override fun beforeExecute(task: Task) {
                if (task == targetTask) {
                    startTime = System.currentTimeMillis()
                }
            }

            override fun afterExecute(task: Task, state: TaskState) {
                if (task == targetTask && task.didWork) {
                    val finishTime = System.currentTimeMillis()
                    buildFinished {
                        println("Task ${task.name} took: ${prettyTime(finishTime - startTime)}")
                    }
                }
            }
        })
    }
}

fun Gradle.logBuild(startTime: Long) {
    useLogger(object : BuildAdapter() {})
    buildFinished {
        println("Total time : ${prettyTime(System.currentTimeMillis() - startTime)}")
    }
}

fun Gradle.assertNonSupportedFeatures() {
    if (startParameter.isContinuous) throw MirakleException("--continuous is not supported yet")
    if (startParameter.includedBuilds.isNotEmpty()) throw MirakleException("Included builds is not supported yet")
}

private const val MS_PER_MINUTE: Long = 60000
private const val MS_PER_HOUR = MS_PER_MINUTE * 60

fun prettyTime(timeInMs: Long): String {
    val result = StringBuffer()
    if (timeInMs > MS_PER_HOUR) {
        result.append(timeInMs / MS_PER_HOUR).append(" hrs ")
    }
    if (timeInMs > MS_PER_MINUTE) {
        result.append(timeInMs % MS_PER_HOUR / MS_PER_MINUTE).append(" mins ")
    }
    result.append(timeInMs % MS_PER_MINUTE / 1000.0).append(" secs")
    return result.toString()
}

class MirakleException(message: String) : RuntimeException(message)

inline fun <reified T : Task> Project.task(name: String, noinline configuration: T.() -> Unit) =
        tasks.create(name, T::class.java, configuration)!!


//void buildFinished(Action<? super BuildResult> action) is available since 3.4
//this is needed to support Gradle 3.3
fun Gradle.buildFinished(body: (BuildResult) -> Unit) {
    buildFinished(closureOf(body))
}

fun <T : Any> Any.closureOf(action: T.() -> Unit): Closure<Any?> =
        KotlinClosure1(action, this, this)

class KotlinClosure1<in T : Any, V : Any>(
        val function: T.() -> V?,
        owner: Any? = null,
        thisObject: Any? = null) : Closure<V?>(owner, thisObject) {

    @Suppress("unused") // to be called dynamically by Groovy
    fun doCall(it: T): V? = it.function()
}

val Task.services: ServiceRegistry get() {
    val field = AbstractTask::class.java.getDeclaredField("services")
    field.isAccessible = true
    return field.get(this) as ServiceRegistry
}

/*
* On Windows rsync is used only under Cygwin environment
* and classical Windows path "C:\Users" must be replaced by "/cygdrive/c/Users"
* */
fun fixDirForRsync(dir: String) =
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            val windowsDisk = dir.first().toLowerCase()
            val path = dir.substringAfter(":\\").replace('\\', '/')
            "/cygdrive/$windowsDisk/$path"
        } else dir