package com.sunrise.wiki.utils;

import io.github.biezhi.webp.WebpIO;
import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.data.Image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {

    public static Image getIconWithPng(String iconUrl, File savePath, File target, double times, GroupMessageEvent event) {
        BufferedImage iconWithPng = getIconWithPng(iconUrl, savePath, target, times);
        if (null != iconWithPng) {
            return event.getGroup().uploadImage(iconWithPng);
        } else {
            return null;
        }
    }

    public static BufferedImage getIconWithPng(String iconUrl, File savePath, File target, double times) {
        if (!savePath.exists()) {
            savePath.mkdirs();
        }
        BufferedImage image = null;
        File destImg = new File(savePath + File.separator + "dest.png");
        try {
            if (destImg.exists()) {
                destImg.delete();
            }
            if (!target.exists()) {
                HttpURLConnection conn = (HttpURLConnection) new URL(iconUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
                InputStream inputStream = conn.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(destImg);
                byte[] buf = new byte[1024 * 1024];
                int numRead;
                while (true) {
                    numRead = inputStream.read(buf);
                    if (numRead <= 0) {
                        break;
                    }
                    outputStream.write(buf, 0, numRead);
                }
                inputStream.close();
                outputStream.close();
                WebpIO.create().toNormalImage(destImg, target);
                destImg.delete();
            }
            BufferedImage oriImg = ImageIO.read(target);
            image = zoomInImage(oriImg,times);
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

    public static BufferedImage mergeSkillImages(List<BufferedImage> imgs, List<String> loops, List<String> skills) {
        List<BufferedImage> destimgs1 = new ArrayList<>();
        List<BufferedImage> destimgs2 = new ArrayList<>();
        List<BufferedImage> destimgs3 = new ArrayList<>();
        List<BufferedImage> destimgs4 = new ArrayList<>();
        List<BufferedImage> dests = new ArrayList<>();
        List<String> destloops1 = new ArrayList<>();
        List<String> destloops2 = new ArrayList<>();
        List<String> destloops3 = new ArrayList<>();
        List<String> destloops4 = new ArrayList<>();
        List<String> destskills1 = new ArrayList<>();
        List<String> destskills2 = new ArrayList<>();
        List<String> destskills3 = new ArrayList<>();
        List<String> destskills4 = new ArrayList<>();
        BufferedImage dest1 = null;
        BufferedImage dest2 = null;
        BufferedImage dest3 = null;
        BufferedImage dest4 = null;
        BufferedImage finalDest = null;
        for (int i = 0; i < imgs.size(); i++) {
            if (i <= 5) {
                destimgs1.add(imgs.get(i));
                destloops1.add(loops.get(i));
                destskills1.add(skills.get(i));
            } else if (i <= 11 && i > 5) {
                destimgs2.add(imgs.get(i));
                destloops2.add(loops.get(i));
                destskills2.add(skills.get(i));
            } else if (i <= 17 && i > 11) {
                destimgs3.add(imgs.get(i));
                destloops3.add(loops.get(i));
                destskills3.add(skills.get(i));
            } else if (i <= 23 && i > 17) {
                destimgs4.add(imgs.get(i));
                destloops4.add(loops.get(i));
                destskills4.add(skills.get(i));
            }
        }
        try {
            if (!destimgs1.isEmpty()) {
                dest1 = mergeImage(true, destimgs1, destloops1, destskills1);
                dests.add(dest1);
            }
            if (!destimgs2.isEmpty()) {
                dest2 = mergeImage(true, destimgs2, destloops2, destskills2);
                dests.add(dest2);
            }
            if (!destimgs3.isEmpty()) {
                dest3 = mergeImage(true, destimgs3, destloops3, destskills3);
                dests.add(dest3);
            }
            if (!destimgs4.isEmpty()) {
                dest4 = mergeImage(true, destimgs4, destloops4, destskills4);
                dests.add(dest4);
            }
            finalDest = mergeImage(false, dests, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            finalDest = null;
        }
        return finalDest;
    }


    /**
     * 拼接图片
     *
     * @param isHorizontal 是否水平拼接
     * @param imgs         待拼接数组
     * @return 拼接过会的图片
     * @throws IOException
     */
    public static BufferedImage mergeImage(boolean isHorizontal, List<BufferedImage> imgs, List<String> loops, List<String> skills) throws IOException {
        // 生成新图片
        BufferedImage destImage = null;
        Graphics graphics = null;
        // 计算新图片的长和高
        int allw = 0, allh = 0, allwMax = 0, allhMax = 0;
        // 获取总长、总宽、最长、最宽
        for (int i = 0; i < imgs.size(); i++) {
            BufferedImage img = imgs.get(i);
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
            destImage = new BufferedImage(allw, allhMax + allhMax / 2, BufferedImage.TYPE_INT_RGB);
            graphics = destImage.getGraphics();
            graphics.fillRect(0, 0, allw, allhMax + allhMax / 2);
            graphics.setColor(Color.WHITE);
            graphics.drawLine(0, 0, allw, allhMax + allhMax / 2);
        } else {
            destImage = new BufferedImage(allwMax, allh, BufferedImage.TYPE_INT_RGB);
            graphics = destImage.getGraphics();
            graphics.fillRect(0, 0, allwMax, allh);
            graphics.setColor(Color.WHITE);
            graphics.drawLine(0, 0, allwMax, allh);
        }
        graphics.dispose();
        // 合并所有子图片到新图片
        int wx = 0, wy = 0;
        Font font = new Font("微软雅黑", Font.BOLD, 12);
        graphics = destImage.createGraphics();
        for (int i = 0; i < imgs.size(); i++) {
            BufferedImage img = imgs.get(i);
            int w1 = img.getWidth();
            int h1 = img.getHeight();
            // 从图片中读取RGB
            int[] ImageArrayOne = new int[w1 * h1];
            ImageArrayOne = img.getRGB(0, 0, w1, h1, ImageArrayOne, 0, w1); // 逐行扫描图像中各个像素的RGB到数组中
            if (isHorizontal) { // 水平方向合并
                destImage.setRGB(wx, h1 / 4, w1, h1, ImageArrayOne, 0, w1); // 设置上半部分或左半部分的RGB
                graphics.setFont(font);
                graphics.setColor(Color.BLACK);
                graphics.drawString(loops.get(i).trim(), wx+6, h1/4 -4);
                graphics.drawString(skills.get(i).trim(), wx+6, h1 + h1 / 2 -4);
            } else { // 垂直方向合并
                destImage.setRGB(0, wy, w1, h1, ImageArrayOne, 0, w1); // 设置上半部分或左半部分的RGB
            }
            wx += w1;
            wy += h1;
        }
        graphics.dispose();
        return destImage;
    }

}
