package ru.tecon.ejb;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton bean для очистки раз в сутки загруженных файлов
 * @author Maksim Shchelkonogov
 */
@Singleton
@Startup
@LocalBean
public class ParserSB {

    private static final Logger LOGGER = Logger.getLogger(ParserSB.class.getName());

    @Resource(name = "upload.location")
    private String uploadPath;

    /**
     * Метод срабатывает каждый день в 0 часов 5 минут.
     * Удаляем все заруженные файлы
     */
    @Schedule(minute = "5", persistent = false)
    private void timer() {
        try {
            Path path = Paths.get(System.getProperty("user.dir") + uploadPath);
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "error remove files", e);
        }
    }
}
