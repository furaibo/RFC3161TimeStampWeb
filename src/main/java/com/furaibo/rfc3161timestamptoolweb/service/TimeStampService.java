package com.furaibo.rfc3161timestamptoolweb.service;

import com.furaibo.rfc3161timestamptoolweb.model.Document;
import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.SecurityProvider;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.COSFilterInputStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@Service
public class TimeStampService {

    // メンバ変数
    @Value("${tsa.service.url}")
    private String tsaServiceURL;

    @Value("${tsa.service.user.account}")
    private String tsaUserAccount;

    @Value("${tsa.service.user.password}")
    private String tsaUserPassword;

    @Value("${app.output.folder.file}")
    private String outputFileFolderPathStr;

    @Value("${app.output.folder.log}")
    private String outputLogFolderPathStr;

    private Path outputFileFolderPath;
    private Path outputLogFolderPath;
    private final DateTimeFormatter dtf1 = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @PostConstruct
    public void init() {
        LocalDateTime ldt = LocalDateTime.now();
        this.outputFileFolderPath = Paths.get(this.outputFileFolderPathStr, ldt.format(dtf1));
        this.outputLogFolderPath = Paths.get(this.outputLogFolderPathStr);
    }

    /**
     * タイムスタンプ情報のテスト、レスポンス情報の表示
     *
     * @param inputBytes
     * @throws IOException
     * @throws InterruptedException
     * @throws TSPException
     */
    public void testTimeStampResponse(byte[] inputBytes)
            throws IOException, InterruptedException, TSPException {

        System.out.println(this.tsaServiceURL);
        System.out.println(this.tsaUserAccount);
        System.out.println(this.tsaUserPassword);

        // タイムスタンプトークンの取得
        TimeStampToken token = this.getTimeStampTokenFromTSA(inputBytes);
        TimeStampTokenInfo tsInfo = token.getTimeStampInfo();

        // X.509証明書の取得
        X509CertificateHolder cert = this.getCertFromTimeStampToken(token);

        // タイムスタンプ情報の出力
        System.out.println("[ タイムスタンプ情報 ]");
        System.out.println("- シリアル番号 : " + tsInfo.getSerialNumber());
        System.out.println("- 生成時刻 : " + tsInfo.getGenTime());
        System.out.println("- TSA情報 : " + tsInfo.getTsa().getName());
        System.out.println("- 有効期限 : " + cert.getNotAfter());
        System.out.println("- 証明書発行者 : " + cert.getIssuer());
    }

    /**
     * 単一のファイルに対してタイムスタンプを付与し、別ファイルとして保存する
     *
     * @param inputFilePath
     * @throws IOException
     * @throws TSPException
     * @throws InterruptedException
     * return Path
     */
    public Path addTimeStampToSingleFile(Path inputFilePath)
            throws IOException, TSPException, InterruptedException {

        // ディレクトリの作成
        Files.createDirectories(this.outputFileFolderPath);

        // 入力ファイル名の取得
        String inputFileName = inputFilePath.getFileName().toString();

        // 出力先ファイルパス
        String outputFilePathStr = String.format(
                "%s/%s",
                this.outputFileFolderPath, inputFileName
            );
        Path outputFilePath = Paths.get(outputFilePathStr);

        // タイムスタンプの取得と埋め込み
        this.embedTimeStampTokenToPDF(inputFilePath, outputFilePath);

        return outputFilePath;
    }

    /**
     *　複数のファイルに対してタイムスタンプを付与し、別ファイルとして保存する
     *
     * @param inputFilePathList
     * @throws IOException
     * @throws TSPException
     * @throws InterruptedException
     */
    public void addTimeStampToMultipleFiles(List<Path> inputFilePathList)
            throws IOException, TSPException, InterruptedException {

        // ディレクトリの作成
        Files.createDirectories(this.outputFileFolderPath);

        for (int i = 0; i < inputFilePathList.size(); i++) {
            // 入力ファイル名の取得
            Path inputFilePath = inputFilePathList.get(i);
            String inputFileName = inputFilePath.getFileName().toString();

            // 出力先ファイルパス
            String outputFilePathStr = String.format(
                    "%s/%03d_%s",
                    this.outputFileFolderPath, i+1, inputFileName
                );
            Path outputFilePath = Paths.get(outputFilePathStr);

            // タイムスタンプの取得と埋め込み
            this.embedTimeStampTokenToPDF(inputFilePath, outputFilePath);

            // タイムスタンプ取得時のsleep処理
            // NOTE:
            //   認定タイムスタンプでは各ベンダー1タイムスタンプ/秒のスペックのため、
            //   必要以上に連続してリクエストを送らないようにする
            Thread.sleep(1000);
        }
    }

    /**
     * 複数ファイルの検証結果を実施後、検証結果をCSVファイルで出力する
     *
     * @param documents
     * @throws IOException
     * @throws TSPException
     * @throws CMSException
     */
    public Path verifyTimeStampInMultipleFiles(
            List<Document> documents)
            throws IOException, TSPException, CMSException {

        // ディレクトリの作成
        Files.createDirectories(this.outputLogFolderPath);

        // 出力先のCSVファイルパスを設定
        String outputCsvFilePathStr = String.format(
                "%s/%s_verify_log.csv",
                this.outputLogFolderPath, dtf2.format(LocalDateTime.now())
            );

        // CSVPrinterの初期化
        FileWriter fileWriter = new FileWriter(outputCsvFilePathStr, false);
        CSVFormat csvFormat = CSVFormat.RFC4180.builder().
                setHeader(
                    "ドキュメントID", "タイトル",
                    "ファイル名", "検証結果", "シリアル番号", "生成時刻",
                    "TSA情報", "有効期限", "証明書発行者").
                build();
        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat);

        for(Document doc: documents) {
            Path path = Paths.get(doc.getTimestampFilePath());
            String inputFileName = path.getFileName().toString();

            // タイムスタンプ情報の取得
            TimeStampToken token = this.extractTimestampTokenFromPDF(path);
            TimeStampTokenInfo tsInfo = token.getTimeStampInfo();

            // タイムスタンプトークンからのX.509証明書の取得
            X509CertificateHolder cert = this.getCertFromTimeStampToken(token);

            // 検証結果の取得
            boolean hasValidDigest = this.checkTimestampHashDigestValidity(path);
            boolean hasValidCert = this.checkTimestampCertValidity(path);
            String timestampStatusStr = (hasValidDigest && hasValidCert) ? "有効": "無効";

            // CSVファイルへの書き込み
            csvPrinter.printRecord(
                doc.getId(),                 // ドキュメントID
                doc.getTitle(),              // ドキュメントタイトル
                inputFileName,               // ファイル名
                timestampStatusStr,          // 検証結果
                tsInfo.getSerialNumber(),    // シリアル番号
                tsInfo.getGenTime(),         // 生成時刻
                tsInfo.getTsa().getName(),   // TSA情報
                cert.getNotAfter(),          // 有効期限
                cert.getIssuer()             // 証明書発行者
            );
        }

        // CSVPrinterの終了時処理
        csvPrinter.flush();
        csvPrinter.close();

        // CSVファイルパスの返却
        return Paths.get(outputCsvFilePathStr);
    }

    /**
     * タイムスタンプ局からのタイムスタンプトークン取得
     *
     * @param inputBytes
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws TSPException
     */
    private TimeStampToken getTimeStampTokenFromTSA(byte[] inputBytes)
            throws IOException, InterruptedException, TSPException {

        // タイムスタンプ要求リクエストの作成
        // NOTE: "setCertReq" によって証明書情報を含めるかを指定
        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        reqGen.setCertReq(true);
        TimeStampRequest timeStampReq = reqGen.generate(TSPAlgorithms.SHA256, inputBytes);

        // HTTPSクライアント
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // HTTPSリクエストの準備
        HttpRequest req;
        if (!this.tsaUserAccount.isEmpty() && !this.tsaUserPassword.isEmpty()) {
            // Basic認証の設定
            String credential = Base64.getEncoder().encodeToString(
                    (this.tsaUserAccount + ":" + this.tsaUserPassword).
                            getBytes(StandardCharsets.UTF_8));

            req = HttpRequest.newBuilder()
                    .uri(URI.create(this.tsaServiceURL))
                    .header("Content-Type", "application/timestamp-query")
                    .header("Authorization", "Basic " + credential)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(timeStampReq.getEncoded()))
                    .build();

        } else {
            req = HttpRequest.newBuilder()
                    .uri(URI.create(this.tsaServiceURL))
                    .header("Content-Type", "application/timestamp-query")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(timeStampReq.getEncoded()))
                    .build();
        }

        // HTTPSリクエストの送信
        // NOTE: タイムスタンプ発行の上限もしくは頻度が多い場合にエラー発生の可能性あり
        var res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            System.out.println("Error: Failed timestamp Request");
            System.out.println("Status Code: " + res.statusCode());
            System.exit(-1);
        }

        // bodyからタイムスタンプトークンの取得
        byte[] resArray = res.body();
        TimeStampResponse resp = new TimeStampResponse(resArray);

        return resp.getTimeStampToken();
    }

    /**
     * タイムスタンプトークンからのX.509証明書の取り出し処理
     *
     * @param token
     * @return
     */
    private X509CertificateHolder getCertFromTimeStampToken(TimeStampToken token) {
        CMSSignedData signedData = token.toCMSSignedData();
        SignerInformation signerInfo = signedData.getSignerInfos().iterator().next();
        Collection<X509CertificateHolder> matches = signedData.getCertificates().getMatches(signerInfo.getSID());
        X509CertificateHolder cert = matches.iterator().next();
        return cert;
    }

    /**
     * PDFへのタイムスタンプ署名の埋め込み処理
     *
     * @param inputFilePath
     * @param outputFilePath
     * @throws IOException
     * @throws TSPException
     * @throws InterruptedException
     */
    private void embedTimeStampTokenToPDF(
            Path inputFilePath, Path outputFilePath)
            throws IOException, TSPException, InterruptedException {

        // PDDocumentの初期化
        PDDocument document = Loader.loadPDF(inputFilePath.toFile());

        // PDDocumentへの署名(タイムスタンプ)の設定
        PDSignature signature = new PDSignature();
        signature.setType(COSName.DOC_TIME_STAMP);
        signature.setSubFilter(COSName.getPDFName("ETSI.RFC3161"));
        document.addSignature(signature);

        // 外部署名サポートを利用する
        // NOTE: 署名用のトークンはすべてタイムスタンプ局側で発行されるため
        FileOutputStream fos = new FileOutputStream(outputFilePath.toString());
        ExternalSigningSupport ess = document.saveIncrementalForExternalSigning(fos);

        // ダイジェスト値を取得する
        // NOTE: 署名追加後にダイジェスト値を取得しないと無効なタイムスタンプ扱いになる
        InputStream docInputStream = ess.getContent();
        byte[] inputBytes = DigestUtils.sha256(docInputStream);

        // タイムスタンプトークンとCMSデータの取得
        TimeStampToken token = this.getTimeStampTokenFromTSA(inputBytes);
        CMSSignedData signedData = token.toCMSSignedData();

        // 最終的な署名データの設定
        ess.setSignature(signedData.getEncoded());
    }

    /**
     * PDFファイルに埋め込まれたタイムスタンプトークンの取り出し
     *
     * @param inputFilePath
     * @return
     * @throws IOException
     * @throws CMSException
     * @throws TSPException
     */
    private TimeStampToken extractTimestampTokenFromPDF(Path inputFilePath)
            throws IOException, CMSException, TSPException {

        // PDDocumentとしてのPDFファイル読み込み
        PDDocument signedDoc = Loader.loadPDF(inputFilePath.toFile());

        // 最新の署名情報の取得
        PDSignature signature = signedDoc.getLastSignatureDictionary();
        byte[] contents = signature.getContents();
        CMSSignedData signedData = new CMSSignedData(contents);

        // タイムスタンプトークンの取り出し
        TimeStampToken token = new TimeStampToken(signedData);

        return token;
    }

    /**
     * ハッシュダイジェスト値が有効かどうかの判定
     *
     * @param inputFilePath
     * @return
     */
    private boolean checkTimestampHashDigestValidity(Path inputFilePath) {

        boolean hashDigestValid = false;

        try {
            // PDDocumentとしてのPDFファイル読み込み
            PDDocument signedDoc = Loader.loadPDF(inputFilePath.toFile());

            // 最新の署名情報の取得
            PDSignature signature = signedDoc.getLastSignatureDictionary();
            byte[] contents = signature.getContents();
            CMSSignedData signedData = new CMSSignedData(contents);

            // タイムスタンプトークン取り出し
            TimeStampToken token = new TimeStampToken(signedData);
            TimeStampTokenInfo tsInfo = token.getTimeStampInfo();

            // ByteRange情報の取得
            // NOTE: ByteRangeは署名の範囲を除いたファイル領域の情報が含まれる
            FileInputStream fis = new FileInputStream(inputFilePath.toString());
            InputStream signedContentAsStream = new COSFilterInputStream(fis, signature.getByteRange());

            // Step 1:　タイムスタンプトークン内のダイジェスト値取得
            String hashAlgorithm = tsInfo.getMessageImprintAlgOID().getId();
            MessageDigest md = MessageDigest.getInstance(hashAlgorithm);

            // Step 2: ByteRange情報からタイムスタンプ付与前のファイルのダイジェスト値を取得
            try (DigestInputStream dis = new DigestInputStream(signedContentAsStream, md))
            {
                while (dis.read() != -1) {
                    // 何もしない
                }
            }

            // Step 1, 2で取得したダイジェスト値を比較
            // NOTE: 比較時には "Arrays.equals" を使用する
            if (Arrays.equals(md.digest(), tsInfo.getMessageImprintDigest())) {
                hashDigestValid = true;
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return hashDigestValid;
    }

    /**
     * タイムスタンプ内の電子証明書が有効かどうかを判定(ただしルート証明書を除く)
     *
     * @param inputFilePath
     * @return
     */
    public boolean checkTimestampCertValidity(Path inputFilePath) {

        boolean hasValidCert = false;

        try {
            // タイムスタンプ付与済みPDFを開く
            PDDocument signedDoc = Loader.loadPDF(inputFilePath.toFile());

            // 署名のうち最新のものを取得
            PDSignature signature = signedDoc.getLastSignatureDictionary();

            // 署名データ・タイムスタンプトークンの取得
            byte[] contents = signature.getContents();
            CMSSignedData cmsSignedData = new CMSSignedData(contents);
            TimeStampToken token = new TimeStampToken(cmsSignedData);

            // X.509電子証明書の検証
            X509CertificateHolder cert = this.getCertFromTimeStampToken(token);
            SignerInformationVerifier siv = new JcaSimpleSignerInfoVerifierBuilder().setProvider(
                    SecurityProvider.getProvider()).build(cert);
            token.validate(siv);

            // ここまでエラー発生がなければ成功扱いとする
            hasValidCert = true;

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return hasValidCert;
    }
}
