package org.sample;

import com.company.monitoring.api.Report;
import com.company.monitoring.api.TestSuite;
import com.company.monitoring.api.Monitoring;
import com.company.monitoring.handlers.JunitTestReportHandler;
import com.company.monitoring.service.Handlers;
import com.company.monitoring.service.MonitoringMain;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Не знаю как рализовать в виде junit теста, ProcessBuilder sh не получается запустить в виде junit
 * Можно запустить как обычное приложение
 */

@Slf4j
@Disabled
public class RealTest {

    private static final String JDK_HOME = "/usr/lib/jvm/java-1.11.0-openjdk-amd64";
    private static final String FILE_URL = "https://dlcdn.apache.org/spark/spark-3.2.0/spark-3.2.0.tgz";
    private static final String FILE_NAME = "spark-3.2.0.tgz";
    private static final String EXTRACT_FILE_NAME = "spark-3.2.0";
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;
    public static final int EXPECTED = 64;
    private Gson gson;


    public RealTest() {
        gson = new Gson();
    }


    public static void main(String[] args) throws NotBoundException, IOException {
        new RealTest().realTest();
    }

    @Test
    void realTest() throws NotBoundException, IOException {
        String dir = System.getProperty("user.dir");
        String streamingDir = Paths.get(dir, "test", EXTRACT_FILE_NAME, "/streaming").toString();
        String graphxDir = Paths.get(dir, "test", EXTRACT_FILE_NAME, "/graphx").toString();

        if (Files.notExists(Paths.get(FILE_NAME))) {
            // download project to run tests
            log.debug("Downloading mvn project {} ...", FILE_URL);
            FileUtils.copyURLToFile(new URL(FILE_URL), new File(FILE_NAME), CONNECT_TIMEOUT, READ_TIMEOUT);
            log.debug("Downloading mvn project done");
        }
        Path test = Paths.get(dir, "test");
        if (Files.notExists(Paths.get(test.toString(), EXTRACT_FILE_NAME))) {
            log.debug("Decompressing mvn project ...");

            decompressTarGzipFile(Paths.get(FILE_NAME), test);
            log.debug("Decompressing mvn project done");
        }

        System.setProperty(Handlers.HANDLERS_MODE, Handlers.HANDLERS_MODE_MANUAL);
        log.debug("Launching monitoring service...");
        MonitoringMain.main(new String[0]);
        log.debug("Launching monitoring service done");
        log.debug("Getting monitoring service remote from RMI registry...");
        Registry registry = LocateRegistry.getRegistry();
        Monitoring monitoring = (Monitoring) registry.lookup(Monitoring.SERVICE_NAME);
        log.debug("Getting monitoring service remote from RMI registry done");

        Path path = FileSystems.getDefault().getPath(streamingDir, "/target/surefire-reports");
        Path path2 = FileSystems.getDefault().getPath(graphxDir, "/target/surefire-reports");

        monitoring.assignHandler("xml", "com.company.monitoring.handlers.JunitTestReportHandler");
        Path reportJson = Paths.get("report.json");
        Files.deleteIfExists(reportJson);
        Files.createFile(reportJson);
        monitoring.configureHandler("xml", new Object[]{reportJson.toString()});

        log.debug("mvn clean...");
        mvnClean(dir);
        log.debug("mvn clean done");

        monitoring.monitorDir(path.toAbsolutePath().toString());
        monitoring.monitorDir(path2.toAbsolutePath().toString());
        monitoring.start();

        try {
            Process start = mvnTest(streamingDir);
            Process start2 = mvnTest(graphxDir);

            await().forever().pollInterval(1, TimeUnit.SECONDS).until(() -> !(start.isAlive() || start2.isAlive()) && !monitoring.isInProgress());

            try {
                Report readReport = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(reportJson))), Report.class);
                log.info("RESULT reports test suites size {}", readReport.getTestSuites().size());
                monitoring.stop();
                Assertions.assertEquals(EXPECTED, readReport.getTestSuites().size());
                monitoring.shutdown();
            } catch (Exception ex) {
               ex.printStackTrace();
               throw new RuntimeException("Can't get results", ex);
            }
        } catch (Exception ex) {
            throw new RuntimeException("cant run mvn test", ex);
        }

    }

    private Process mvnTest(String dir) throws IOException {
        boolean isWindows = isWindows();
        String cmd = (isWindows ? "setx JAVA_HOME " + JDK_HOME + "mvn.bat test" : "export JAVA_HOME=" + JDK_HOME + "; mvn test ");

        ProcessBuilder builder = new ProcessBuilder();
        builder.inheritIO();
        if (isWindows) {
            builder.command("cmd.exe", "/c", cmd);
        } else {
            builder.command("sh", "-c", cmd);
        }

        Path some = FileSystems.getDefault().getPath(dir);
        builder.directory(some.toFile());


        Process start = builder.start();
        return start;
    }

    private void mvnClean(String dir) throws IOException {
        boolean isWindows = isWindows();
        String cmd = (isWindows ? "setx JAVA_HOME " + JDK_HOME + "; mvn.bat test" : "export JAVA_HOME=" + JDK_HOME + "; mvn clean ")
                + "-Dmaven.compiler.fork=true -Dmaven.compiler.executable=" + JDK_HOME;
        ProcessBuilder builder = new ProcessBuilder();
        builder.inheritIO();
        if (isWindows) {
            builder.command("cmd.exe", "/c", cmd);
        } else {
            builder.command("sh", "-c", cmd);
        }

        Path some0 = FileSystems.getDefault().getPath(dir, "test", EXTRACT_FILE_NAME, "/streaming");
        builder.directory(some0.toFile());
        Process start0 = builder.start();
        try {
            start0.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    public static void decompressTarGzipFile(Path source, Path target) throws IOException {

        if (Files.notExists(source)) {
            throw new IOException("File doesn't exists!");
        }

        try (InputStream fi = Files.newInputStream(source); BufferedInputStream bi = new BufferedInputStream(fi); GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi); TarArchiveInputStream ti = new TarArchiveInputStream(gzi)) {

            ArchiveEntry entry;
            while ((entry = ti.getNextEntry()) != null) {

                // create a new path, zip slip validate
                Path newPath = zipSlipProtect(entry, target);

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {

                    // check parent folder again
                    Path parent = newPath.getParent();
                    if (parent != null) {
                        if (Files.notExists(parent)) {
                            Files.createDirectories(parent);
                        }
                    }

                    // copy TarArchiveInputStream to Path newPath
                    Files.copy(ti, newPath, StandardCopyOption.REPLACE_EXISTING);

                }
            }
        }
    }

    private static Path zipSlipProtect(ArchiveEntry entry, Path targetDir) throws IOException {

        Path targetDirResolved = targetDir.resolve(entry.getName());

        // make sure normalized file still has targetDir as its prefix,
        // else throws exception
        Path normalizePath = targetDirResolved.normalize();

        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad entry: " + entry.getName());
        }

        return normalizePath;
    }
}
