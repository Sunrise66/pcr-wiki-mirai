package com.sunrise.wiki.utils;

import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.data.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
     *
     * @param originalImage 原始图片
     * @param times         缩放倍数
     * @return
     */
    public static BufferedImage zoomInImage(BufferedImage originalImage, double times) {
        int width = (int) (originalImage.getWidth() * times);
        int height = (int) (originalImage.getHeight() * times);
        BufferedImage newImage = new BufferedImage(width, height, originalImage.getType());
        Graphics g = newImage.getGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return newImage;
    }

    /**
     * 2  * 合并任数量的图片成一张图片
     * 3  *
     * 4  * @param isHorizontal
     * 5  *            true代表水平合并，fasle代表垂直合并
     * 6  * @param imgs
     * 7  *            待合并的图片数组
     * 8  * @return
     * 9  * @throws IOException
     * 10
     */
    public static BufferedImage mergeImage(boolean isHorizontal, BufferedImage... imgs) throws IOException {
        // 生成新图片
        BufferedImage destImage = null;
        // 计算新图片的长和高
        int allw = 0, allh = 0, allwMax = 0, allhMax = 0;
        // 获取总长、总宽、最长、最宽
        for (int i = 0; i < imgs.length; i++) {
            BufferedImage img = imgs[i];
            allw += img.getWidth();
            allh += img.getHeight();
            if (img.getWidth() > allwMax) {
                allwMax = img.getWidth();
            }
            if (img.getHeight() > allhMax) {
                allhMax = img.getHeight();
            }
        }
        // 创建新图片
        if (isHorizontal) {
            destImage = new BufferedImage(allw, allhMax, BufferedImage.TYPE_INT_RGB);
        } else {
        }
        // 合并所有子图片到新图片
        int wx = 0, wy = 0;
        for (int i = 0; i < imgs.length; i++) {
            BufferedImage img = imgs[i];
            int w1 = img.getWidth();
            int h1 = img.getHeight();
            // 从图片中读取RGB
            int[] ImageArrayOne = new int[w1 * h1];
            ImageArrayOne = img.getRGB(0, 0, w1, h1, ImageArrayOne, 0, w1); // 逐行扫描图像中各个像素的RGB到数组中
            if (isHorizontal) { // 水平方向合并
                destImage.setRGB(wx, 0, w1, h1, ImageArrayOne, 0, w1); // 设置上半部分或左半部分的RGB
            } else { // 垂直方向合并
                destImage.setRGB(0, wy, w1, h1, ImageArrayOne, 0, w1); // 设置上半部分或左半部分的RGB
            }
            wx += w1;
            wy += h1;
        }
        return destImage;
    }

}
