package com.bobko.storage.service;

/**
 * @author oleksii bobko
 * @data 12.08.2013
 * @see PictureService
 */

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.validation.Valid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.bobko.storage.common.StorageConst;
import com.bobko.storage.dao.base.IGenericDao;
import com.bobko.storage.dao.interfaces.IPictureDao;
import com.bobko.storage.domain.Document;
import com.bobko.storage.domain.UserEntity;
import com.bobko.storage.service.interfaces.IPictureService;
import com.bobko.storage.util.AlbumUtils;

@Service
@Transactional
public class PictureService implements IPictureService {

    @Autowired
    private IPictureDao<Document, Integer> pictureDao;

    @Autowired
    private IGenericDao<UserEntity, Integer> userDao;
    
    @Value("${data.root.path}")
    private String rootPath;

    private static final String JPG = "jpg";
    private static final String PNG = "png";
    private static final String DATA = "data";
    private static final String IMAGES = "images";
    private static final String THUMBNAIL = "thumbnail";
    private static final int SIZE = 1024;
    
    private static final Logger LOGGER = LogManager.getLogger(PictureService.class);
    
    public List<Document> list(int shift, int count) {
        return pictureDao.rankList(shift, count);
    }

    public Document getPicture(int id) {
        return pictureDao.find(id);
    }

    public void addPicture(Document pic) {
        pictureDao.add(pic);
    }

    public void removePicture(int id) {
        Document entity = pictureDao.find(id);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(entity.getOwner().equals(auth.getName())) {
            pictureDao.remove(entity);
        }
    }

    @Override
    public void savePicture(@Valid Document pic, MultipartFile multipartFile) throws Exception {
        
        pic.setFilename(multipartFile.getOriginalFilename());

        String username = getLoginedUserName();
        pic.setOwner(username);
        // normalize description length
        if (pic.getDescription().length() >= StorageConst.MAX_DESCRIPTION_SIZE) {
            pic.setDescription(pic.getDescription().substring(0, StorageConst.MAX_DESCRIPTION_SIZE));
        }

        String pathToFile = DATA + File.separator + username + File.separator + IMAGES;
        
        File dir = createDirs(rootPath + pathToFile);
        String pathToThumbnail = DATA + File.separator + username + File.separator + THUMBNAIL;
        File thumbnailDir = createDirs(rootPath + pathToThumbnail);
        String fileName = pic.getFilename();

        String suffix = "";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            suffix = fileName.substring(i);
        }
        String uuid = AlbumUtils.getUUID();
        
        String name = File.separator + uuid + suffix;
        
        File image = new File(dir + File.separator + fileName);
        multipartFile.transferTo(image);
        
        File thumbnail = new File(thumbnailDir + name);
        BufferedImage bufferedImage = ImageIO.read(image);
        
        if (bufferedImage != null) { 
            bufferedImage = AlbumUtils.correctingSize(bufferedImage);
            //TODO create thumbnail creator
            ImageIO.write(bufferedImage, suffix.substring(1), thumbnail);
            pic.setThumbnail(pathToThumbnail + name);
        }
        
        pic.setPath(pathToFile + File.separator + fileName);
        
        UserEntity user = userDao.getByField("login", username).get(0);

        pic.setUser(user);
        
        pic.setCreated(new Timestamp(new Date().getTime()));
        
        addPicture(pic);
        
    }
    
    @Override
    public void savePicture(String url) {
        
        Document pic = new Document();
        
        int slashIndex = url.lastIndexOf('/');
        String originalFileName = url.substring(slashIndex + 1);
        if ((originalFileName != null) && !originalFileName.isEmpty()) {
            pic.setFilename(originalFileName);
        } else {
            pic.setFilename("imgUrl");
        }
        
        String username = getLoginedUserName();
        String pathToFile = DATA + File.separator + username + File.separator + IMAGES;
        File dir = createDirs(rootPath + pathToFile);
        String pathToThumbnail = DATA + File.separator + username + File.separator + THUMBNAIL;
        File thumbnailDir = createDirs(rootPath + pathToThumbnail);
        String uuid = AlbumUtils.getUUID();
        OutputStream outStream = null;
        HttpURLConnection connection = null;
        InputStream is = null;
        String suffix = "";
        String name = "";
        
        try {
            URL urlToPicture;
            byte[] buf;
            int byteRead;
            urlToPicture = new URL(url);

            int i = url.lastIndexOf('.');
            if (i > 0) {
                suffix = url.substring(i + 1);
            }
            
            name = File.separator + uuid + "." + suffix;
            
            File image = new File(dir + File.separator + pic.getFilename());
            
            outStream = new BufferedOutputStream(new FileOutputStream(image));

            // Proxy proxy = new Proxy(Proxy.Type.HTTP, new
            // InetSocketAddress("172.30.0.2", 3128));

            connection = (HttpURLConnection) urlToPicture.openConnection();
            connection.addRequestProperty("User-Agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                LOGGER.error(connection.getErrorStream());
                return;
            } else {
                
                is = connection.getInputStream();
                buf = new byte[SIZE];
                while ((byteRead = is.read(buf)) != -1) {
                    outStream.write(buf, 0, byteRead);
                }
                
                outStream.close();
                
                File thumbnail = new File(thumbnailDir + name);
                
                BufferedImage bufferedImage = ImageIO.read(image);
                
                if(bufferedImage != null) {
                if(suffix.equalsIgnoreCase(JPG) || suffix.equalsIgnoreCase(PNG)) {
                    bufferedImage = AlbumUtils.correctingSize(bufferedImage);
                    ImageIO.write(bufferedImage, suffix, thumbnail);
                } else {
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(image));
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(thumbnail));
                    
                    while ((byteRead = in.read(buf)) != -1) {
                        out.write(buf, 0, byteRead);
                    }
                    
                    in.close();
                    out.close();
                    
                }
                
                pic.setThumbnail(pathToThumbnail + name);
                
                }
            }

        } catch (Exception e) {
            LOGGER.error(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
        
        UserEntity user = userDao.getByField("login", username).get(0);
        pic.setUser(user);
        pic.setDescription(AlbumUtils.getPureAdress(url));
        
        pic.setPath(pathToFile + File.separator + pic.getFilename());
        
        pic.setOwner(username);
        
        pic.setCreated(new Timestamp(new Date().getTime()));
        
        try {
            addPicture(pic);
        } catch (Exception e) {
            LOGGER.error("some error occured", e);
        }
    }    
        
    /**
     * @return authenticated user name
     * */
    private String getLoginedUserName() {
        String userName = "anonimouse";
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null) {
            userName = authentication.getName();
        }
        
        return userName;
    }

    @Override
    public byte[] getPicByPath(String path) {
        
        byte[] fileData = new byte[0];
        
        Path p = FileSystems.getDefault().getPath(rootPath, path);
        try {
            fileData = Files.readAllBytes(p);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return fileData;
        
    }

    private File createDirs(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
}
