package com.example.translate.services;

public class PdfExtractor {
}
//package br.com.afort.pdfextractor.services;
//
//        import java.awt.image.BufferedImage;
//        import java.awt.image.DataBufferByte;
//        import java.io.ByteArrayInputStream;
//        import java.io.File;
//        import java.io.IOException;
//        import java.io.InputStream;
//        import java.text.DecimalFormat;
//        import java.text.NumberFormat;
//        import java.util.ArrayList;
//        import java.util.Arrays;
//        import java.util.Date;
//        import java.util.Iterator;
//        import java.util.LinkedList;
//        import java.util.List;
//        import java.util.regex.Matcher;
//        import java.util.regex.Pattern;
//
//        import nu.pattern.OpenCV;
//        import org.apache.pdfbox.pdmodel.PDDocument;
//        import org.apache.pdfbox.rendering.ImageType;
//        import org.apache.pdfbox.rendering.PDFRenderer;
//        import org.opencv.core.CvType;
//        import org.opencv.core.Mat;
//        import org.opencv.core.MatOfByte;
//        import org.opencv.core.Size;
//        import org.opencv.imgcodecs.Imgcodecs;
//        import org.opencv.imgproc.Imgproc;
//        import org.slf4j.Logger;
//        import org.slf4j.LoggerFactory;
//        import org.springframework.beans.factory.annotation.Autowired;
//        import org.springframework.stereotype.Component;
//        import org.springframework.util.StringUtils;
//
//        import com.fasterxml.jackson.databind.ObjectMapper;
//
//        import br.com.afort.pdfextractor.domain.ErroRequestContent;
//        import br.com.afort.pdfextractor.domain.Request;
//        import br.com.afort.pdfextractor.domain.RequestContent;
//        import br.com.afort.pdfextractor.domain.StatusRequest;
//        import br.com.afort.pdfextractor.domain.TypeRequestContent;
//        import br.com.afort.pdfextractor.dto.PDFConfigDTO;
//        import br.com.afort.pdfextractor.dto.PdfDadosDTO;
//        import br.com.afort.pdfextractor.dto.PdfDadosTipo;
//        import br.com.afort.pdfextractor.exceptions.ServiceError;
//        import br.com.afort.pdfextractor.repositories.RequestRepository;
//        import br.com.afort.pdfextractor.services.storage.StorageS3;
//        import net.sourceforge.tess4j.Tesseract;
//        import net.sourceforge.tess4j.TesseractException;
//
//        import javax.imageio.ImageIO;
//
//@Component
//public class PdfExtractorProcessService {
//
//    private final StorageS3 storageS3;
//
//    private final RequestRepository requestRepository;
//
//    private static final Logger logger = LoggerFactory.getLogger("PdfExtractorProcessService");
//
//    @Autowired
//    public PdfExtractorProcessService(StorageS3 storageS3,
//                                      RequestRepository requestRepository) {
//        this.storageS3 = storageS3;
//        this.requestRepository = requestRepository;
//    }
//
//    public Request processRequest(Request request, InputStream inputStream) {
//        request.setStatusRequest(StatusRequest.PROCESSING);
//        request.setData(new ArrayList<>());
//        request = this.requestRepository.saveAndFlush(request);
//        try {
//            request = this.extractValuesFromPDF(request, inputStream);
//        } catch (Throwable t) {
//            request.setStatusRequest(StatusRequest.FINISHED);
//            request.setMessage(t.getMessage());
//            request.setSuccess(false);
//        }
//        request.setDateProcess(new Date());
//        return requestRepository.save(request);
//    }
//
//    private Request extractValuesFromPDF(Request request, InputStream inputStream){
//        List<RequestContent> result = new ArrayList<>();
//        List<String> stringPDF = new ArrayList<>();
//        try {
//            if (inputStream == null) {
//                inputStream = storageS3.read(request.getArquivo());
//            }
//            stringPDF = getTextData(inputStream);
//        } catch (IOException e){
//            logger.error("Erro ao ler pdf recebido",e);
//            throw new ServiceError("Erro ao ler pdf recebido",e);
//        }
//        catch(TesseractException e) {
//            logger.error("Erro ao fazer OCR no pdf recebido",e);
//            throw new ServiceError("Erro ao fazer OCR no pdf recebido",e);
//        }
//
//        try {
//            PDFConfigDTO pdf = getPDFConfigDTO();
//            List<PdfDadosDTO> pdfDados = pdf.getListPdfDadosDto();
//            for(PdfDadosDTO metaDados : pdfDados){
//                RequestContent content = new RequestContent();
//                content.setChave(metaDados.getCode());
//                content.setRequest(request);
//                content.setTypeRequestContent(TypeRequestContent.SUCCESS);
//                Double value = null;
//                if(metaDados.getPdfDadosTipo().equals(PdfDadosTipo.STRING)){
//                    value = getValue(stringPDF, metaDados.getReferenceStart(), metaDados.getQtLines(), metaDados.getPosition(), metaDados.getSeparator());
//                    if(value == null && (metaDados.getCode().equals("RECEITA_ATIVIDADE_RURAL") || metaDados.getCode().equals("DESPESAS_CUSTEIO"))){
//                        value = getPropertyExist(stringPDF, metaDados.getReferenceStart(), pdf.getQtLinesEmptyValue(), pdf.getEmptyValue());
//                    }
//                } else {
//                    value = getArrayValue(stringPDF, metaDados.getReferenceStart(), metaDados.getQtLines(), metaDados.getPosition(), metaDados.getSeparator());
//                }
//
//                if(value == null){
//                    ErroRequestContent contentErro = new ErroRequestContent();
//                    contentErro.setCode("ERRO_LEITURA");
//                    contentErro.setMessage("Não foi possível obter o valor");
//                    contentErro.setRequestContent(content);
//                    content.setErrorMessage(contentErro);
//                    content.setTypeRequestContent(TypeRequestContent.ERROR);
//                } else {
//                    content.setValue(value.toString());
//                }
//                result.add(content);
//            }
//        } catch (IOException e) {
//            logger.error("Erro ao ler pdf",e);
//            throw new ServiceError("Erro ao ler pdf",e);
//        }
//        request.setData(result);
//        request.setSuccess(true);
//        request.setStatusRequest(StatusRequest.FINISHED);
//        return request;
//    }
//
//    private PDFConfigDTO getPDFConfigDTO() throws IOException {
//        ObjectMapper objectMapper = new ObjectMapper();
//        PDFConfigDTO pdfConfigDTO = objectMapper.readValue(new File("/home/renan/projetos/afort/pdf-extractor/pdfextractor/src/main/resources/pdf.json"), PDFConfigDTO.class);
//
//        return pdfConfigDTO;
//    }
//
//    private Tesseract createTesseract() {
//        Tesseract tesseract = new Tesseract();
//        /*Diretório contendo o idioma a ser utilizado*/
//        tesseract.setDatapath("/home/renan/projetos/afort/pdf-extractor/pdfextractor/src/main/resources/tessdata");
//        tesseract.setLanguage("por");
//
//        tesseract.setPageSegMode(1);
//        tesseract.setOcrEngineMode(1);
//        tesseract.setTessVariable("user_defined_dpi", "300");
//        return tesseract;
//    }
//
//    private List<String> getTextData(InputStream pdfInput) throws IOException, TesseractException {
//        StringBuilder result = new StringBuilder();
//        OpenCV.loadShared();
//        Tesseract tesseract = createTesseract();
//
//        PDDocument pdfDocument = PDDocument.load(pdfInput);
//        PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);
//        int pages = pdfDocument.getNumberOfPages();
//        for (int i=0; i<pages; i++) {
//            BufferedImage img = pdfRenderer.renderImageWithDPI(i,300,ImageType.BINARY);
//            img = convertTo3ByteBGRType(img);
//
//            byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
//            Mat mat = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
//            mat.put(0, 0, data);
////			Imgproc.GaussianBlur(mat, mat, new Size(3, 3), 0);
//            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
//            Imgproc.resize(mat, mat, new Size(1400,1000));
//            Imgproc.blur(mat , mat, new Size(3,3));
//
//            MatOfByte matOfByte = new MatOfByte();
//            Imgcodecs.imencode(".jpg", mat, matOfByte);
//            byte[] byteArray = matOfByte.toArray();
//            InputStream in = new ByteArrayInputStream(byteArray);
//            img = ImageIO.read(in);
//
//
//            result.append(tesseract.doOCR(img));
//        }
//
//        pdfInput.close();
//        pdfDocument.close();
//
//        List<String> valor = new LinkedList<String>(Arrays.asList(result.toString().trim().split("\n")));
//        Iterator<String> remover = valor.iterator();
//
//        while (remover.hasNext()){
//            if(remover.next().isBlank()){
//                remover.remove();
//            }
//        }
//
//        return valor;
//    }
//
//    private static BufferedImage convertTo3ByteBGRType(BufferedImage image) {
//        BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(),
//                BufferedImage.TYPE_3BYTE_BGR);
//        convertedImage.getGraphics().drawImage(image, 1, 1, null);
//        return convertedImage;
//    }
//
//
//    private Integer findValor(List<String>stringPDF, String testar) {
//        Pattern pattern = Pattern.compile(testar, Pattern.CASE_INSENSITIVE);
//        for (int i = 0; i < stringPDF.size(); i++) {
//            Matcher matcher = pattern.matcher(stringPDF.get(i));
//            if (matcher.find()) {
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    private String findStringValue(List<String> stringPDF , String testar, Integer qtdLinha){
//        Integer numeroLinha = findValor(stringPDF, testar);
//        if (numeroLinha == -1) {
//            return "";
//        }
//        String result = "";
//        Integer line = numeroLinha + qtdLinha;
//        if(line <= stringPDF.size()){
//            result = stringPDF.get(line);
//        }
//        return result;
//    }
//
//    private Double getValue(List<String> stringPDF, String testar, Integer qtdLinha, Integer position, String separator) {
//        String value = findStringValue(stringPDF, testar, qtdLinha);
//        if(value.equals("")){
//            return null;
//        }
//        if (position != null && StringUtils.hasText(separator)) {
//            String[] values = value.split(separator);
//            if(position > values.length-1){
//                return null;
//            } else {
//                value = values[position];
//            }
//        }
//
//        Pattern pattern2 = Pattern.compile("((0|[1-9][0-9]{0,2}(\\.[0-9]{3})*)(,[0-9]{2})|[0-9]{4})");
//        Matcher matcher = pattern2.matcher(value);
//
//        if (matcher.find()){
//            value = matcher.group();
//        }else{
//            return null;
//        }
//
//        Double result = convertStringToDouble(value);
//        return result;
//    }
//
//    private Double getPropertyExist(List<String> stringPDF, String testar, Integer qtdLinha, String emptyValue) {
//        String value = findStringValue(stringPDF, testar, qtdLinha);
//
//        Pattern pattern2 = Pattern.compile(emptyValue);
//        Matcher matcher = pattern2.matcher(value);
//
//        if (matcher.find()){
//            return 0.0;
//        }else{
//            return null;
//        }
//    }
//
//    private Double getArrayValue(List<String> stringPDF, String testar, Integer qtdLinha, Integer position, String separator) {
//
//        Double result = 0.0;
//
//        Double valor = getValue(stringPDF, testar, qtdLinha, position, separator);
//
//        while (valor != null){
//            result += valor;
//            qtdLinha++;
//            valor = getValue(stringPDF, testar, qtdLinha, position, separator);
//        }
//
//        NumberFormat formatter = new DecimalFormat("#0.00");
//        Double total = convertStringToDouble(formatter.format(result));
//        return total;
//    }
//
//    private Double convertStringToDouble(String valor){
//        Double result = Double.valueOf(valor.replaceAll("\\.","").replace(",",".")).doubleValue();
//        return result;
//    }
//}
//
//package br.com.afort.pdfextractor.services;
//
//        import static org.junit.Assert.assertEquals;
//        import static org.junit.Assert.assertNotNull;
//        import static org.junit.Assert.assertNull;
//        import static org.junit.Assert.assertTrue;
//
//        import java.io.InputStream;
//        import java.util.ArrayList;
//        import java.util.List;
//
//        import br.com.afort.pdfextractor.job.PdfExtractorAsync;
//        import org.junit.jupiter.api.Test;
//        import org.junit.runner.RunWith;
//        import org.springframework.beans.factory.annotation.Autowired;
//        import org.springframework.beans.factory.annotation.Value;
//        import org.springframework.boot.test.context.SpringBootTest;
//        import org.springframework.test.context.junit4.SpringRunner;
//
//        import br.com.afort.pdfextractor.domain.Arquivo;
//        import br.com.afort.pdfextractor.domain.Request;
//        import br.com.afort.pdfextractor.domain.RequestContent;
//        import br.com.afort.pdfextractor.domain.StatusRequest;
//        import br.com.afort.pdfextractor.repositories.RequestRepository;
//        import br.com.afort.pdfextractor.services.storage.StorageS3;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//class PdfExtractorProcessServiceTest {
//
//    @Autowired
//    private PdfExtractorProcessService service;
//
//    @Autowired
//    private StorageS3 s3Service;
//
//    @Autowired
//    private RequestRepository requestRepository;
//
//    @Autowired
//    PdfExtractorAsync pdfExtractorAsync;
//
//    @Value("${s3.bucket.name}")
//    private String bucket;
//
//    @Test
//    public void testReturnAllData(){
//        Request request = createRequest("declaracao-de-teste-dados-ficticios.pdf");
//
//        Request result = service.processRequest(request, null);
//
//        assertEquals(StatusRequest.FINISHED, result.getStatusRequest());
//        assertTrue(result.isSuccess());
//        assertNotNull(result.getDateProcess());
//
//        assertNotNull(result.getData());
//        assertTrue(result.getData().size() > 0);
//
//        for (RequestContent content: result.getData()) {
//            assertNotNull("Deveria ter retornado valor para " + content.getChave(), content.getValue());
//
//            switch (content.getChave()) {
//                case "BENS_DIREITOS":
//                    assertEquals("Valor incorreto para " + content.getChave(), "157661.24", content.getValue());
//                    break;
//                case "DIVIDAS_ONUS_REAIS":
//                    assertEquals("Valor incorreto para " + content.getChave(), "1022.25", content.getValue());
//                    break;
//                case "TOTAL_RENDIMENTOS_TRIBUTAVEIS":
//                    assertEquals("Valor incorreto para " + content.getChave(), "20780.4", content.getValue());
//                    break;
//                case "RENDIMENTOS_ATIVIDADE_RURAL":
//                    assertEquals("Valor incorreto para " + content.getChave(), "1058.15", content.getValue());
//                    break;
//                case "RECEITA_ATIVIDADE_RURAL":
//                    assertEquals("Valor incorreto para " + content.getChave(), "6483.59", content.getValue());
//                    break;
//                case "DESPESAS_CUSTEIO":
//                    assertEquals("Valor incorreto para " + content.getChave(), "5425.44", content.getValue());
//                    break;
//                case "PREJUIZO_COMPENSAR":
//                    assertEquals("Valor incorreto para " + content.getChave(), "2323.44", content.getValue());
//                    break;
//                case "DIVIDAS_ATIVIDADE_RURAL":
//                    assertEquals("Valor incorreto para " + content.getChave(), "18912.1", content.getValue());
//                    break;
//                default:
//                    break;
//            }
//        }
//    }

//	@Test
//	public void testPDFSemAlgunsDadosRurais(){
//		Request request = createRequest("declaracao-de-teste-dados-ficticios-sem-alguns-dados-rurais.pdf");
//
//		Request result = service.processRequest(request);
//
//		assertEquals(StatusRequest.FINISHED, result.getStatusRequest());
//		assertTrue(result.isSuccess());
//		assertNotNull(result.getDateProcess());
//
//		assertNotNull(result.getData());
//		assertTrue(result.getData().size() > 0);
//
//		for (RequestContent content: result.getData()) {
//			switch (content.getChave()) {
//			case "BENS_DIREITOS":
//				assertEquals("Valor incorreto para " + content.getChave(), "157661.24", content.getValue());
//				break;
//			case "DIVIDAS_ONUS_REAIS":
//				assertEquals("Valor incorreto para " + content.getChave(), "32295.84", content.getValue());
//				break;
//			case "TOTAL_RENDIMENTOS_TRIBUTAVEIS":
//				assertEquals("Valor incorreto para " + content.getChave(), "20780.4", content.getValue());
//				break;
//			case "RENDIMENTOS_ATIVIDADE_RURAL":
//				assertEquals("Valor incorreto para " + content.getChave(), "1058.15", content.getValue());
//				break;
//			case "RECEITA_ATIVIDADE_RURAL":
////				assertEquals("Valor incorreto para " + content.getChave(), "6483.59", content.getValue());
//				break;
//			case "DESPESAS_CUSTEIO":
//				assertEquals("Valor incorreto para " + content.getChave(), "5425.44", content.getValue());
//				break;
//			case "PREJUIZO_COMPENSAR":
//				assertEquals("Valor incorreto para " + content.getChave(), "2323.44", content.getValue());
//				break;
//			case "DIVIDAS_ATIVIDADE_RURAL":
//				assertEquals("Valor incorreto para " + content.getChave(), "0", content.getValue());
//				break;
//			default:
//				break;
//			}
//		}
//	}

//    @Test
//    public void testPDFSemAtividadeRural(){
//        Request request = createRequest("declaracao-de-teste-dados-ficticios-sem-atividade-rural.pdf");
//
//        Request result = service.processRequest(request, null);
//
//        assertEquals(StatusRequest.FINISHED, result.getStatusRequest());
//        assertTrue(result.isSuccess());
//        assertNotNull(result.getDateProcess());
//
//        assertNotNull(result.getData());
//        assertTrue(result.getData().size() > 0);
//
//        for (RequestContent content: result.getData()) {
//            switch (content.getChave()) {
//                case "BENS_DIREITOS":
//                    assertEquals("Valor incorreto para " + content.getChave(), "157661.24", content.getValue());
//                    break;
//                case "DIVIDAS_ONUS_REAIS":
//                    assertEquals("Valor incorreto para " + content.getChave(), "32295.84", content.getValue());
//                    break;
//                case "TOTAL_RENDIMENTOS_TRIBUTAVEIS":
//                    assertEquals("Valor incorreto para " + content.getChave(), "19722.25", content.getValue());
//                    break;
//                case "RENDIMENTOS_ATIVIDADE_RURAL":
//                    assertEquals("Valor incorreto para " + content.getChave(), "0.0", content.getValue());
//                    break;
//                case "RECEITA_ATIVIDADE_RURAL":
//                    assertEquals("Valor incorreto para " + content.getChave(), "0.0", content.getValue());
//                    break;
//                case "DESPESAS_CUSTEIO":
//                    assertEquals("Valor incorreto para " + content.getChave(), "0.0", content.getValue());
//                    break;
//                case "PREJUIZO_COMPENSAR":
//                    assertNull(content.getValue());
//                    assertNotNull(content.getErrorMessage());
//                    assertEquals("ERRO_LEITURA", content.getErrorMessage().getCode());
//                    break;
//                case "DIVIDAS_ATIVIDADE_RURAL":
//                    assertEquals("Valor incorreto para " + content.getChave(), "0.0", content.getValue());
//                    break;
//                default:
//                    break;
//            }
//        }
//    }
//
//    @Test
//    public void testPDFSemDivida(){
//        Request request = createRequest("declaracao-de-teste-dados-ficticios-sem-divida.pdf");
//
//        Request result = service.processRequest(request, null);
//
//        assertEquals(StatusRequest.FINISHED, result.getStatusRequest());
//        assertTrue(result.isSuccess());
//        assertNotNull(result.getDateProcess());
//
//        assertNotNull(result.getData());
//        assertTrue(result.getData().size() > 0);
//
//        for (RequestContent content: result.getData()) {
//            switch (content.getChave()) {
//                case "BENS_DIREITOS":
//                    assertEquals("Valor incorreto para " + content.getChave(), "157661.24", content.getValue());
//                    break;
//                case "DIVIDAS_ONUS_REAIS":
//                    assertEquals("Valor incorreto para " + content.getChave(), "0.0", content.getValue());
//                    break;
//                case "TOTAL_RENDIMENTOS_TRIBUTAVEIS":
//                    assertEquals("Valor incorreto para " + content.getChave(), "20780.4", content.getValue());
//                    break;
//                case "RENDIMENTOS_ATIVIDADE_RURAL":
//                    assertEquals("Valor incorreto para " + content.getChave(), "1058.15", content.getValue());
//                    break;
//                case "RECEITA_ATIVIDADE_RURAL":
//                    assertNull(content.getValue());
//                    assertNotNull(content.getErrorMessage());
//                    assertEquals("ERRO_LEITURA", content.getErrorMessage().getCode());
//                    break;
//                case "PREJUIZO_COMPENSAR":
//                    assertEquals("Valor incorreto para " + content.getChave(), "2323.44", content.getValue());
//                    break;
//                case "DIVIDAS_ATIVIDADE_RURAL":
//                    assertEquals("Valor incorreto para " + content.getChave(), "0.0", content.getValue());
//                    break;
//                default:
//                    break;
//            }
//        }
//    }
//
////	@Test
////	public void testCarga(){
////		List<Request> list = new ArrayList<Request>();
////		Request request = createRequest("declaracao-de-teste-dados-ficticios-sem-divida.pdf");
////		Request request1 = createRequest("declaracao-de-teste-dados-ficticios.pdf");
////		Request request2 = createRequest("declaracao-de-teste-dados-ficticios-sem-alguns-dados-rurais.pdf");
////		Request request3 = createRequest("declaracao-de-teste-dados-ficticios-sem-atividade-rural.pdf");
////		Request request4 = createRequest("declaracao-de-teste-dados-ficticios-sem-divida.pdf");
////		Request request5 = createRequest("declaracao-de-teste-dados-ficticios.pdf");
////		Request request6 = createRequest("declaracao-de-teste-dados-ficticios-sem-alguns-dados-rurais.pdf");
////		Request request7 = createRequest("declaracao-de-teste-dados-ficticios-sem-atividade-rural.pdf");
////		Request request8 = createRequest("declaracao-de-teste-dados-ficticios-sem-divida.pdf");
////		Request request9 = createRequest("declaracao-de-teste-dados-ficticios.pdf");
////		Request request10 = createRequest("declaracao-de-teste-dados-ficticios-sem-alguns-dados-rurais.pdf");
////		Request request11 = createRequest("declaracao-de-teste-dados-ficticios-sem-atividade-rural.pdf");
////
////		pdfExtractorAsync.run();
//////		list.add(request);
//////		list.add(request1);
//////		list.add(request2);
//////		list.add(request3);
//////		list.add(request4);
//////		list.add(request5);
//////		list.add(request6);
//////		list.add(request7);
//////		list.add(request8);
//////		list.add(request9);
//////		list.add(request10);
//////		list.add(request11);
//////
//////		for(Request result: list){
//////			result = service.processRequest(request, null);
//////		}
////
////	}
//
//    private Request createRequest(String fileName) {
//        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);
//        assertNotNull(inputStream);
//
//        Arquivo arquivo = s3Service.store("declaracao-de-teste-dados-ficticios.pdf", bucket, inputStream);
//
//        Request request = new Request();
//        request.setArquivo(arquivo);
//        request.setStatusRequest(StatusRequest.RECEIVED);
//        return requestRepository.save(request);
//    }
//}


