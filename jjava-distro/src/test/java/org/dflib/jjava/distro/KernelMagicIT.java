package org.dflib.jjava.distro;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KernelMagicIT extends ContainerizedKernelCase {

    private static final Gson GSON = new Gson();

    @Deprecated
    @Test
    public void jars() throws Exception {
        String jar = CONTAINER_RESOURCES + "/dummy.jar";
        Container.ExecResult jarResult = container.execInContainer(
                "jar", "--create", "--file", jar, "-C", TEST_CLASSPATH, ".");
        assertEquals(0, jarResult.getExitCode(), jarResult.getStderr() + "\n" + jarResult.getStdout());

        KernelRun run = executeInKernel(
                "%jars " + jar,
                "import org.dflib.jjava.Dummy;",
                "Dummy.class.getName()"
        ).assertNoErrors();

        assertEquals("\"org.dflib.jjava.Dummy\"", run.cell(3).result());
    }

    @Test
    public void classpath() throws Exception {
        KernelRun run = executeInKernel(
                "%classpath " + TEST_CLASSPATH,
                "import org.dflib.jjava.Dummy;",
                "\"className = \" + Dummy.class.getName();"
        ).assertNoErrors();

        assertEquals("\"className = org.dflib.jjava.Dummy\"", run.cell(3).result());
    }

    @Test
    public void maven() throws Exception {
        KernelRun run = executeInKernel(
                "%maven jakarta.annotation:jakarta.annotation-api:3.0.0",
                "import jakarta.annotation.Nullable;",
                "Nullable.class.getName()"
        ).assertNoErrors();

        assertEquals("\"jakarta.annotation.Nullable\"", run.cell(3).result());
    }

    @Test
    public void executorKernelHelperFailsExplicitlyWithoutLosingState() throws Exception {
        KernelRun run = executeInKernel(
                "int saved = 40;",
                "kernel();",
                "saved + 2"
        );

        run.cell(2).assertError("JavaKernel.kernel() cannot expose the managing JVM");
        assertEquals("42", run.cell(3).result());
    }

    @Deprecated
    @Test
    public void mavenIvySyntax() throws Exception {
        KernelRun run = executeInKernel(
                "%maven jakarta.annotation#jakarta.annotation-api;3.0.0",
                "import jakarta.annotation.Nullable;",
                "Nullable.class.getName()"
        ).assertNoErrors();

        assertEquals("\"jakarta.annotation.Nullable\"", run.cell(3).result());
    }

    @Test
    public void timeMagicCompletes() throws Exception {
        KernelRun run = executeInKernel("%time 1 + 1").assertNoErrors();

        assertEquals("2", run.cell(1).result());
        assertTrue(run.cell(1).display().contains("Wall time:"), run.cell(1).display());
    }

    @Test
    public void portableDisplayBundlesReachTheManager() throws Exception {
        String cell = String.join("\n",
                "import org.dflib.jjava.jupyter.kernel.display.DisplayData;",
                "var bundle = new DisplayData(\"portable\");",
                "bundle.putHTML(\"<b>portable</b>\");",
                "bundle.putData(\"image/png\", \"AQID\");",
                "String id = display(bundle);"
        );
        KernelRun run = executeInKernel(cell, "updateDisplay(id, new DisplayData(\"updated\"));").assertNoErrors();

        JsonObject mime = GSON.fromJson(run.cell(1).field("mime"), JsonObject.class);
        assertEquals("portable", mime.get("text/plain").getAsString());
        assertEquals("<b>portable</b>", mime.get("text/html").getAsString());
        assertEquals("AQID", mime.get("image/png").getAsString());
        assertEquals("updated", run.cell(2).display());
    }

    @Test
    public void nonPortableDisplayPayloadFailsVisibly() throws Exception {
        KernelRun run = executeInKernel(
                "var data = new org.dflib.jjava.jupyter.kernel.display.DisplayData(); data.putData(\"text/html\", new Object()); display(data);",
                "40 + 2"
        );

        run.cell(1).assertError("Non-portable display value");
        assertEquals("42", run.cell(2).result());
    }

    @Test
    public void load() throws Exception {
        KernelRun run = executeInKernel(
                "%load " + CONTAINER_RESOURCES + "/test-ping.jshell",
                "ping()"
        ).assertNoErrors();

        assertEquals("\"pong!\"", run.cell(2).result());
    }

    @Test
    public void loadFromPOM() throws Exception {
        KernelRun run = executeInKernel(
                "%loadFromPOM " + CONTAINER_RESOURCES + "/test-pom.xml",
                "import jakarta.annotation.Nullable;",
                "Nullable.class.getName()"
        ).assertNoErrors();

        assertEquals("\"jakarta.annotation.Nullable\"", run.cell(3).result());
    }
}
