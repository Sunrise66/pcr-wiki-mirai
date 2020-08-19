package com.sunrise.wiki.utils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class StringUtil {
    public static void main(String[] args) {
        readXml();
    }

    private static void readXml() {
        File xml = new File("src\\main\\resources\\strings.xml");
        File txt = new File("src\\main\\resources\\strings.txt");
        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(xml);
            Element rootElement = document.getRootElement();
            Element foo;
            if(!txt.exists()){
                txt.createNewFile();
            }
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(txt), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (Iterator i = rootElement.elementIterator("string"); i.hasNext(); ) {
                foo = (Element) i.next();
                sb.append("public static String "+foo.attribute("name").getData()+" = "+"\""+foo.getData()+"\""+";"+"\n");
//                System.out.println(foo.attribute("name").getData());
//                System.out.println(foo.getData());
            }
            bw.write(sb.toString());
            bw.flush();
            bw.close();
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
    }
}
