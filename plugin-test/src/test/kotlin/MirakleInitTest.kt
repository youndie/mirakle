import com.instamotor.BuildConfig
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.PrintWriter

object MirakleInitTest : Spek({
    BuildConfig.SUPPORTED_GRADLE_VERSIONS.forEach { gradleVersion ->
        describe("project with gradle version $gradleVersion") {
            val folder by temporaryFolder()

            beforeEachTest {
                GradleRunner.create()
                        .withProjectDir(folder.root)
                        .withGradleVersion(gradleVersion)
                        .withArguments("wrapper")
                        .build()
            }

            describe("mirakle init without host") {
                beforeEachTest {
                    folder.newFile("mirakle_init.gradle")
                            .outputStream()
                            .let(::PrintWriter)
                            .use {
                                val MIRAKLE_INIT_WITHOUT_HOST = MIRAKLE_INIT_WITH_FOLDER(folder.root.canonicalPath).replace("host \"localhost\"", "")
                                it.append(MIRAKLE_INIT_WITHOUT_HOST)
                            }
                }

                val gradleRunner by memoized {
                    GradleRunner.create()
                            .withProjectDir(folder.root)
                            .withGradleVersion(gradleVersion)
                            .forwardOutput()
                            .withArguments("-I", "mirakle_init.gradle", "tasks")
                }

                it("should fail") {
                    gradleRunner.buildAndFail()
                }
            }
        }
    }
})