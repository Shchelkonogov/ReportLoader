package ru.tecon.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@WebServlet("/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        File uploadPath = new File(System.getProperty("user.dir") +
                getServletContext().getInitParameter("upload.location") +
                "/" +
                req.getParameter("UUID"));
        if (!uploadPath.exists() && !uploadPath.mkdirs()) {
               return;
        }
        Part filePart = req.getPart("file[]");
        for (Part part: req.getParts().stream().filter(p -> filePart.getName().equals(p.getName())).collect(Collectors.toList())) {
            String fileName = Paths.get(part.getSubmittedFileName()).getFileName().toString();
            File file = createFile(uploadPath, fileName, 0);
            try (InputStream in = part.getInputStream()) {
                Files.copy(in, file.toPath());
            }
        }
    }

    private File createFile(File path, String name, int index) {
        int in = name.lastIndexOf(".");
        String ext = name.substring(in);
        File file = new File(path, index == 0 ? name : name.replace(ext, "(" + index + ")" + ext));
        if (file.exists()) {
            return createFile(path, name, ++index);
        } else {
            return file;
        }
    }
}
