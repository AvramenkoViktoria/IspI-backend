package org.docpirates.ispi.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextReader {

    public static String getFileExtension(String pathToFile) {
        int index = pathToFile.lastIndexOf(".");
        if (index > 0) return pathToFile.substring(index + 1).toLowerCase();
        return "";
    }

    public static String getTextFromPDF(String pathToFIle) throws IOException {
        File file = new File(pathToFIle);
        PDDocument document = PDDocument.load(file);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }

    public static String getTextFromDocx(String pathToFile) throws IOException {
        File file = new File(pathToFile);
        StringBuilder text = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file)) {
            XWPFDocument document = new XWPFDocument(fis);
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs)
                text.append(paragraph.getText()).append("\n");
        }

        return text.toString();
    }

    public static String getTextFromTxt(String pathToFile) throws IOException {
        File file = new File(pathToFile);
        StringBuilder text = new StringBuilder();

        if (!file.exists())
            throw new IOException("File does not exist: " + pathToFile);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null)
                text.append(line).append("\n");
        }

        return text.toString().trim();
    }

    public static String getTextFromFB2(String pathToFile) {
        StringBuilder textContent = new StringBuilder();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            File file = new File(pathToFile);
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("body");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node bodyNode = nodeList.item(i);
                if (bodyNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element bodyElement = (Element) bodyNode;
                    NodeList paragraphNodes = bodyElement.getElementsByTagName("p");
                    for (int j = 0; j < paragraphNodes.getLength(); j++) {
                        Node paragraphNode = paragraphNodes.item(j);
                        if (paragraphNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element paragraphElement = (Element) paragraphNode;
                            textContent.append(paragraphElement.getTextContent()).append("\n");
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error reading file: " + e.getMessage();
        }
        return textContent.toString();
    }

    public static String getTextFromSGM(String pathToFile) throws IOException {
        File file = new File(pathToFile);
        StringBuilder text = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            Pattern tagPattern = Pattern.compile("<[^>]+>");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = tagPattern.matcher(line);
                String cleanedLine = matcher.replaceAll("");
                text.append(cleanedLine.trim()).append("\n");
            }
        }

        return text.toString();
    }

    public static String getTextFromCSV(String pathToFile) throws IOException {
        StringBuilder text = new StringBuilder();
        try (FileReader reader = new FileReader(pathToFile)) {
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
            for (CSVRecord record : csvParser) {
                for (String field : record)
                    text.append(field).append(" ");
                text.append("\n");
            }
        }
        return text.toString();
    }

    public static String getTextFromFile(String pathToFile) throws IOException {
        String fileExtension = getFileExtension(pathToFile);
        return switch (fileExtension) {
            case "txt" -> getTextFromTxt(pathToFile);
            case "pdf" -> getTextFromPDF(pathToFile);
            case "docx" -> getTextFromDocx(pathToFile);
            case "fb2" -> getTextFromFB2(pathToFile);
            case "sgm" -> getTextFromSGM(pathToFile);
            case "csv" -> getTextFromCSV(pathToFile);
            default -> {
                System.err.println("Unsupported file type: " + fileExtension);
                yield "";
            }
        };
    }

    /*
    public static String getTextFromDoc(String pathToFile) throws IOException {
        File file = new File(pathToFile);
        StringBuilder text = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file)) {
            POIFSFileSystem fs = new POIFSFileSystem(fis);
            HWPFDocument document = new HWPFDocument(fs);
            String docText = document.getDocumentText();
            text.append(docText);
        }

        return text.toString();
    }
     */
}