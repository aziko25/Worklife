package telegram.bot.Controller.Admins;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@CrossOrigin(maxAge = 3600)
public class ImageController {

    @GetMapping("/api/media/{imageName}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) {

        try {

            Path file = Paths.get("/var/www/html/images", imageName).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {

                String contentType = Files.probeContentType(file);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }
            else {

                throw new RuntimeException("Could not read the file!");
            }
        }
        catch (Exception e) {

            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}
