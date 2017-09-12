import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.*
import kotlin.test.assertEquals


object StreamModificationTest : Spek({
    val target by memoized { ByteArrayOutputStream() }
    val output by memoized { modifyOutputStream(target, ".mirakle", "/Users/foo/bar") }
    val writer by memoized { PrintWriter(output) }

    on("stream contains UNIX path") {
        writer.use {
            it.appendln("e: /home/foobar/.mirakle/bla-bla-bla")
            it.appendln("/home/foobar/.mirakle/bla-bla-bla")
            it.appendln("Parsing /home/foobar/.mirakle/bla-bla-bla")
        }

        val result = target.toString()

        it("should replace with local path") {
            assertEquals(result.lines()[0], "e: /Users/foo/bar/bla-bla-bla")
            assertEquals(result.lines()[1], "/Users/foo/bar/bla-bla-bla")
            assertEquals(result.lines()[2], "Parsing /Users/foo/bar/bla-bla-bla")
        }
    }

    /* on("stream contains BUILD SUCCESSFUL and BUILD FAILED") {
         writer.use {
             it.appendln("BUILD SUCCESSFUL")
             it.appendln("BUILD FAILED")
         }

         val result = target.toString()

         it("should replace with REMOTE") {
             assertEquals(result.lines()[0], "REMOTE BUILD SUCCESSFUL")
             assertEquals(result.lines()[1], "REMOTE BUILD FAILED")
         }
     }*/
})