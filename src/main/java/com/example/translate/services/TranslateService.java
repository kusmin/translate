package com.example.translate.services;

import com.example.translate.exceptions.ServiceErro;
import com.google.cloud.translate.Translation;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.UUID;

public class TranslateService {

    public String extrairTextoDoPDF(MultipartFile pdfReader){
        PDDocument pdfDocument = null;

        try {
            pdfDocument = PDDocument.load(pdfReader.getInputStream());
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(pdfDocument);
            return texto;
        } catch (IOException e) {
            throw new ServiceErro("Erro no Pdf", e);
        }finally{
            if(pdfDocument != null ) try {
                pdfDocument.close();
            } catch (IOException e) {
                throw new ServiceErro("Erro no Pdf", e);
            }
        }
    }

//    public String getTranslate(String texto){
//        String retorno = new String();
//
//        Translation translation = translate.translate(texto);
//
//        return retorno;
//    }

    public void criarTXT(String texto){
        File arquivo = new File(System.getProperty("java.io.tmpdir" + "/PdfTemp" + UUID.randomUUID().toString() + ".txt"));
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(arquivo));
            bw.write(texto);
            bw.close();
        } catch (IOException e) {
            throw new ServiceErro("Erro no TXT", e);
        }
    }
}
