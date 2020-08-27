package com.sunrise.wiki.utils;

import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.data.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageUtils {
    public static Image getIconWithPng(String iconUrl, File savePath, File target, double times, GroupMessageEvent event) {
        if (!savePath.exists()) {
            savePath.mkdirs();
        }
        Image image;
        try {
            if (!target.exists()) {
                HttpURLConnection conn = (HttpURLConnection) new URL(iconUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
                InputStream inputStream = conn.getInputStream();
                BufferedImage oriImg = ImageIO.read(inputStream);
                BufferedImage bufferedImage = zoomInImage(oriImg, times);
                ImageIO.write(bufferedImage, "png", target);
                inputStream.close();
            }
            image = event.getGroup().uploadImage(target);
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 对图片进行缩放
     * @param originalImage 原始图片
     * @param times 缩放倍数
     * @return
     */
    public static BufferedImage  zoomInImage(BufferedImage  originalImage, double times){
        int width = (int) (originalImage.getWidth()*times);
        int height = (int) (originalImage.getHeight()*times);
        BufferedImage newImage = new BufferedImage(width,height,originalImage.getType());
        Graphics g = newImage.getGraphics();
        g.drawImage(originalImage, 0,0,width,height,null);
        g.dispose();
        return newImage;
    }

}
