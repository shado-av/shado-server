package server.Output;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.*;
/**
 * Zipping a file and return in web service
 * Created by siyuchen on 3/15/18.
 */
public class ZipDir extends  SimpleFileVisitor<Path> {
    private ZipOutputStream zos;

    private Path sourceDir;

    public ZipDir(Path sourceDir,ZipOutputStream zos) {
        this.sourceDir = sourceDir;
        this.zos = zos;
    }

    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attributes) {

        try {
            Path targetFile = sourceDir.relativize(file);

            zos.putNextEntry(new ZipEntry(targetFile.toString()));

            byte[] bytes = Files.readAllBytes(file);
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();

        } catch (IOException ex) {
            System.err.println(ex);
        }

        return FileVisitResult.CONTINUE;
    }

}
