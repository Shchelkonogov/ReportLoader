package ru.tecon.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Сервлет для загрузки файлов на сервер приложений
 */
@WebServlet("/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {

    private static Logger log = Logger.getLogger(UploadServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        Path uploadPath = Paths.get(System.getProperty("user.dir") +
                getServletContext().getInitParameter("upload.location") +
                "/" +
                req.getParameter("UUID") + "/");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        try (Stream<Path> pathStream = Files.walk(uploadPath)) {
            for (Path path: pathStream.filter(path -> Files.isRegularFile(path)).collect(Collectors.toList())) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "can't delete files", e);
            return;
        }

        Part filePart = req.getPart("file[]");
        for (Part part: req.getParts().stream().filter(p -> filePart.getName().equals(p.getName())).collect(Collectors.toList())) {
            String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();
            Path file = createFile(uploadPath, fileName, 0);
            try (InputStream in = part.getInputStream()) {
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private Path createFile(Path path, String name, int index) throws IOException {
        int in = name.lastIndexOf(".");
        String ext = name.substring(in);
        Path filePath = path.resolve(index == 0 ? name : name.replace(ext, "(" + index + ")" + ext));
        if (Files.exists(filePath)) {
            return createFile(path, name, ++index);
        } else {
            return Files.createFile(filePath);
        }
    }
}
