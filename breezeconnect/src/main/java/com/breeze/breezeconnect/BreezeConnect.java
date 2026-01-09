package com.breeze.breezeconnect;
//import com.opencsv.CSVReader;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.emitter.Emitter.Listener;
import io.socket.engineio.client.transports.WebSocket;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.BufferedReader;



import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

import java.net.URL;
import java.net.HttpURLConnection;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;



public class BreezeConnect extends ApificationBreeze {

    public String userId;
    public String apiKey;
    public String sessionToken;
    public Config config;
    private Socket socket=null;

    private Socket socketOrder=null;

    private Socket socketOHLC=null;
    private OnTickEventListener mListener;

    public Map<String,Map<String ,String>> tuxToUserValue = new HashMap<>();

    public ArrayList<JSONObject> stockScriptDictList = new ArrayList<>();
    public ArrayList<JSONObject> tokenScriptDictList = new ArrayList<>();
    private boolean orderNotificationSubscribed = false;
    private String interval = "";

    public BreezeConnect(String apiKey) {
        super();
        config = new Config();
        this.apiKey = apiKey;
        this.getStockScriptList();
    }

    public void errorException(String message) throws Exception {
        throw new Exception(message);
    }

    public JSONObject socketConnectionResponse(String message) throws JSONException {
        return new JSONObject().put("message", message);
    }

    private String getContractName(String underlying, String productType, String expiryDate,
                                   String strikePrice, String optionType) {
        // Mirror Python: strip quotes and normalize
        if (productType == null) productType = "";
        if (underlying == null) underlying = "";
        if (expiryDate == null) expiryDate = "";
        if (strikePrice == null) strikePrice = "";
        if (optionType == null) optionType = "";

        productType = productType.replace("\"", "").toUpperCase();
        underlying = underlying.replace("\"", "");
        expiryDate = expiryDate.replace("\"", "");
        strikePrice = strikePrice.replace("\"", "");
        optionType = optionType.replace("\"", "").toUpperCase();

        String contractName;
        if (productType.contains("FUT")) {
            // FUT-<underlying>-<expiryDate>
            contractName = String.format("FUT-%s-%s", underlying, expiryDate);
        } else {
            // OPT-<underlying>-<expiryDate>-<strikePrice>-<CE|PE>
            String opt = optionType.contains("C") ? "CE" : "PE";
            contractName = String.format("OPT-%s-%s-%s-%s", underlying, expiryDate, strikePrice, opt);
        }
        return contractName;
    }

    private String[] splitCsvLine(String line) {
        List<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }

        list.add(sb.toString());
        return list.toArray(new String[0]);
    }

    public void getStockScriptList() {
        // reset containers to ensure indexes 0..5 exist
        for (int i = 0; i < 6; i++) {
            if (i < this.stockScriptDictList.size()) this.stockScriptDictList.set(i, new JSONObject());
            else this.stockScriptDictList.add(new JSONObject());
            if (i < this.tokenScriptDictList.size()) this.tokenScriptDictList.set(i, new JSONObject());
            else this.tokenScriptDictList.add(new JSONObject());
        }
        HttpURLConnection conn = null;
        try {
            String zipUrl = null;
            // prefer SECURITY_MASTER_URL if available, otherwise fall back to STOCK_SCRIPT_CSV_URL
            try { zipUrl = config.urls.get(Config.UrlEnum.SECURITY_MASTER_URL); } catch (Exception ignored) {}
            if (zipUrl == null || zipUrl.isEmpty()) {
                zipUrl = config.urls.get(Config.UrlEnum.STOCK_SCRIPT_CSV_URL);
            }

            URL url = new URL(zipUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(30_000);
            conn.setRequestMethod("GET");

            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(conn.getInputStream()), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    if (entry.isDirectory()) {
                        zis.closeEntry();
                        continue;
                    }

                    String lower = entryName.toLowerCase();
                    if (!lower.endsWith(".txt") && !lower.endsWith(".csv")) {
                        zis.closeEntry();
                        continue;
                    }

                    String upperName = entryName.toUpperCase();
                    String exchangeCode = null;
                    int idx = -1;
                    if (upperName.contains("FONSE")) { exchangeCode = "NFO"; idx = 4; }
                    else if (upperName.contains("FOBSE")) { exchangeCode = "BFO"; idx = 5; }
                    else if (upperName.contains("MCX")) { exchangeCode = "MCX"; idx = 3; }
                    else if (upperName.contains("NDX")) { exchangeCode = "NDX"; idx = 2; }
                    else if (upperName.contains("NSE")) { exchangeCode = "NSE"; idx = 1; }
                    else if (upperName.contains("BSE")) { exchangeCode = "BSE"; idx = 0; }
                    else {
                        zis.closeEntry();
                        continue;
                    }
                    // Read the entire zip entry into memory (byte array)
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = zis.read(buf)) != -1) {
                        baos.write(buf, 0, n);
                    }
                    byte[] entryBytes = baos.toByteArray();

                    if (entryBytes.length == 0) {
                        zis.closeEntry();
                        continue;
                    }

                    String entryText = new String(entryBytes, StandardCharsets.UTF_8);

                    // parse entryText line-by-line (with robust checks per exchange)
                    try (BufferedReader br2 = new BufferedReader(new java.io.StringReader(entryText))) {
                        String line;
                        int lineNo = 0;
                        while ((line = br2.readLine()) != null) {
                            lineNo++;
                            if (line == null || line.trim().isEmpty()) continue;

                            String[] cols = splitCsvLine(line);
                            if (cols.length < 2) {
                                // too short even for BSE/NSE -> skip
                                System.err.printf("Skipping very short row (len=%d) in %s line %d%n", cols.length, entryName, lineNo);
                                continue;
                            }

                            try {
                                if ("NFO".equals(exchangeCode) || "BFO".equals(exchangeCode)) {
                                    // need at least cols[0..6]
                                    if (cols.length <= 6) {
                                        System.err.printf("Skipping NFO/BFO row (needs >6 cols, got %d) in %s line %d%n", cols.length, entryName, lineNo);
                                        continue;
                                    }
                                    String token = cols[0].replace("\"", "").trim();
                                    String contractName = getContractName(cols[2], cols[3], cols[4], cols[5], cols[6]);
                                    String companyName = cols.length > 29 ? cols[29].replace("\"", "").trim() : "";
                                    this.stockScriptDictList.get(idx).put(contractName, token);
                                    JSONArray v = new JSONArray();
                                    v.put(contractName);
                                    v.put(companyName);
                                    this.tokenScriptDictList.get(idx).put(token, v);

                                } else if ("MCX".equals(exchangeCode)) {
                                    // need at least cols[0..9]
                                    if (cols.length <= 9) {
                                        System.err.printf("Skipping MCX row (needs >9 cols, got %d) in %s line %d%n", cols.length, entryName, lineNo);
                                        continue;
                                    }
                                    String token = cols[0].replace("\"", "").trim();
                                    String contractName = getContractName(cols[2], cols[3], cols[7], cols[9], cols[8]);
                                    String companyName = cols.length > 4 ? cols[4].replace("\"", "").trim() : "";
                                    this.stockScriptDictList.get(idx).put(contractName, token);
                                    JSONArray v = new JSONArray();
                                    v.put(contractName);
                                    v.put(companyName);
                                    this.tokenScriptDictList.get(idx).put(token, v);

                                } else if ("NDX".equals(exchangeCode)) {
                                    // need at least cols[0..6]
                                    if (cols.length <= 6) {
                                        System.err.printf("Skipping NDX row (needs >6 cols, got %d) in %s line %d%n", cols.length, entryName, lineNo);
                                        continue;
                                    }
                                    String token = cols[0].replace("\"", "").trim();
                                    String stockCode = cols[2].replace("\"", "").trim();
                                    String companyName = cols.length > 29 ? cols[29].replace("\"", "").trim() : "";
                                    this.stockScriptDictList.get(idx).put(stockCode, token);
                                    JSONArray v = new JSONArray();
                                    v.put(stockCode);
                                    v.put(companyName);
                                    this.tokenScriptDictList.get(idx).put(token, v);

                                } else if ("BSE".equals(exchangeCode) || "NSE".equals(exchangeCode)) {
                                    // BSE/NSE minimal fields: cols[0], cols[1], optional cols[3]
                                    if (cols.length < 2) {
                                        System.err.printf("Skipping BSE/NSE short row (needs >=2 cols, got %d) in %s line %d%n", cols.length, entryName, lineNo);
                                        continue;
                                    }
                                    String token = cols[0].replace("\"", "").trim();
                                    String stockCode = cols[1].replace("\"", "").trim();
                                    String companyName = cols.length > 3 ? cols[3].replace("\"", "").trim() : "";
                                    this.stockScriptDictList.get(idx).put(stockCode, token);
                                    JSONArray v = new JSONArray();
                                    v.put(stockCode);
                                    v.put(companyName);
                                    this.tokenScriptDictList.get(idx).put(token, v);

                                } else {
                                    // should not reach here
                                    System.err.printf("Unknown exchange %s for entry %s%n", exchangeCode, entryName);
                                }
                            } catch (Exception innerEx) {
                                // protect against unexpected runtime issues per-line; continue parsing next lines
                                System.err.printf("Error parsing line %d in %s (len=%d): %s -> %s%n", lineNo, entryName, cols.length, line, innerEx.getMessage());
                            }
                        }
                    } // br2 closed here
                    zis.closeEntry();
                }
            }
        } catch (Exception e) {
            // log full stack for debugging
            e.printStackTrace();
            System.err.println("Error loading SecurityMaster.zip: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }



//
//    public void getStockScriptList(){
//        for(int i=0;i<5;i++){
//            JSONObject stockScript = new JSONObject();
//            this.stockScriptDictList.add(stockScript);
//        }
//        for(int i=0;i<5;i++){
//            JSONObject tokenScript = new JSONObject();
//            this.tokenScriptDictList.add(tokenScript);
//        }
//        try{
//            int CONNECTION_TIMEOUT_MS = 10000; // Timeout in millis.
//            CloseableHttpClient client = HttpClients.createDefault();
//            HttpEntityEnclosingRequestBase http = new HttpGetWithEntity();
//            http.setConfig(RequestConfig.custom()
//                    .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
//                    .setConnectTimeout(CONNECTION_TIMEOUT_MS)
//                    .setSocketTimeout(CONNECTION_TIMEOUT_MS)
//                    .build());
//            http.setURI(URI.create(config.urls.get(Config.UrlEnum.STOCK_SCRIPT_CSV_URL)));
//            String responseString = "";
//            try {
//                CloseableHttpResponse response = client.execute(http);
//                HttpEntity responseEntity = response.getEntity();
//                responseString = EntityUtils.toString(responseEntity, "UTF-8");
//                String [] stockDataList = responseString.split("\n");
//                for(String rowString:stockDataList){
//                    String [] row = rowString.split(",");
//                    if(row[2].equals("BSE")){
//                        this.stockScriptDictList.get(0).put(row[3],row[5]);
//                        JSONArray values = new JSONArray();
//                        values.put(row[3]);
//                        values.put(row[1]);
//                        this.tokenScriptDictList.get(0).put(row[5],values);
//                    }
//                    else if(row[2].equals("NSE")){
//                        this.stockScriptDictList.get(1).put(row[3],row[5]);
//                        JSONArray values = new JSONArray();
//                        values.put(row[3]);
//                        values.put(row[1]);
//                        this.tokenScriptDictList.get(1).put(row[5],values);
//                    }
//                    else if(row[2].equals("NDX")){
//                        this.stockScriptDictList.get(2).put(row[7],row[5]);
//                        JSONArray values = new JSONArray();
//                        values.put(row[7]);
//                        values.put(row[1]);
//                        this.tokenScriptDictList.get(2).put(row[5],values);
//                    }
//                    else if(row[2].equals("MCX")){
//                        this.stockScriptDictList.get(3).put(row[7],row[5]);
//                        JSONArray values = new JSONArray();
//                        values.put(row[3]);
//                        values.put(row[1]);
//                        this.tokenScriptDictList.get(3).put(row[5],values);
//                    }
//                    else if(row[2].equals("NFO")){
//                        this.stockScriptDictList.get(4).put(row[7],row[5]);
//                        JSONArray values = new JSONArray();
//                        values.put(row[3]);
//                        values.put(row[1]);
//                        this.tokenScriptDictList.get(4).put(row[5],values);
//                    }
//                    else if(row[2].equals("BFO")){
//                        this.stockScriptDictList.get(5).put(row[7],row[5]);
//                        JSONArray values = new JSONArray();
//                        values.put(row[3]);
//                        values.put(row[1]);
//                        this.tokenScriptDictList.get(5).put(row[5],values);
//                    }
//                }
//            } finally {
//                client.close();
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    public Dictionary getDataFromStockTokenValue(String inputStockToken) throws Exception {
        Dictionary outputData = new Hashtable();
        String []stockArray = inputStockToken.split("\\.");
        String exchangeType = stockArray[0];
        String stockToken = stockArray[1].split("!")[1];
        Map<String,String> exchangeCodeList = new HashMap<>();
        exchangeCodeList.put("1","BSE");
        exchangeCodeList.put("4","NSE");
        exchangeCodeList.put("13","NDX");
        exchangeCodeList.put("6","MCX");
        exchangeCodeList.put("8","BFO");

        String exchangeCodeName = exchangeCodeList.getOrDefault(exchangeType,"");
        JSONArray stockData = new JSONArray();
        stockData = null;
        if(exchangeCodeName.equals("")){
            this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.WRONG_EXCHANGE_CODE_EXCEPTION));
        }
        else if(exchangeCodeName.toLowerCase().equals("bse")){
            stockData = this.tokenScriptDictList.get(0).optJSONArray(stockToken);
            if(stockData == null){
                this.errorException(String.format(config.exceptionMessage.get(Config.ExceptionEnum.STOCK_NOT_EXIST_EXCEPTION), "BSE",inputStockToken));
            }

        }
        else if(exchangeCodeName.toLowerCase().equals("nse")){
            stockData = this.tokenScriptDictList.get(1).optJSONArray(stockToken);
            if(stockData == null){
                stockData = this.tokenScriptDictList.get(4).optJSONArray(stockToken);
                if(stockData == null){
                    this.errorException(String.format(config.exceptionMessage.get(Config.ExceptionEnum.STOCK_NOT_EXIST_EXCEPTION),"i.e. NSE or NFO.",inputStockToken));
                }
                else{
                    exchangeCodeName="NFO";
                }
            }
        }
        else if(exchangeCodeName.toLowerCase().equals("ndx")){
            stockData = this.tokenScriptDictList.get(2).optJSONArray(stockToken);
            if(stockData == null)
                this.errorException(String.format(config.exceptionMessage.get(Config.ExceptionEnum.STOCK_NOT_EXIST_EXCEPTION),"NDX",inputStockToken));
        } else if (exchangeCodeName.toLowerCase().equals("mcx")) {
            stockData = this.tokenScriptDictList.get(3).optJSONArray(stockToken);
            if(stockData == null)
                this.errorException(String.format(config.exceptionMessage.get(Config.ExceptionEnum.STOCK_NOT_EXIST_EXCEPTION),"MCX",inputStockToken));
        } else if (exchangeCodeName.toLowerCase().equals("bfo")) {
            stockData = this.tokenScriptDictList.get(5).optJSONArray(stockToken);
            if(stockData == null)
                this.errorException(String.format(config.exceptionMessage.get(Config.ExceptionEnum.STOCK_NOT_EXIST_EXCEPTION),"BFO",inputStockToken));
        }

        outputData.put("stock_name",stockData.get(1));
        String []equityExchangeCodeList = new String[]{"nse","bse"};
        if(!Arrays.asList(equityExchangeCodeList).contains(exchangeCodeName.toLowerCase())){
            String productType = stockData.get(0).toString().split("-")[0];
            if(productType.toLowerCase().equals("fut")){
                outputData.put("product_type","Futures");
            }
            else if(productType.toLowerCase().equals("opt")){
                outputData.put("product_type", "Options");
            }
            String dateString = "";
            String []dateArray = stockData.get(0).toString().split("-");
            String []splitArray = Arrays.copyOfRange(dateArray,2,4);
            for (String s : splitArray) {
                dateString = dateString + s + "-";
            }
            outputData.put("strike_date",dateString.substring(0,dateString.length()-2));
            String []remArray = stockData.get(0).toString().split("-");
            if(remArray.length>5){
                outputData.put("strike_price",remArray[5]);
                String right = stockData.get(0).toString().split("-")[6];
                if(right.toUpperCase().equals("PE"))
                    outputData.put("right","Put");
                if(right.toUpperCase().equals("CE"))
                    outputData.put("right","Call");
            }


        }
        return outputData;
    }

    public Map<String,String> getStockTokenValue(String exchangeCode, String stockCode, String productType, String expiryDate, String strikePrice,
                                                 String right,boolean getExchangeQuotes,boolean getMarketDepth) throws Exception{
        if(!getExchangeQuotes && !getMarketDepth)
            this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.QUOTE_DEPTH_EXCEPTION));
        else {
            Map<String, String> exchangeCodeList = new HashMap<>();
            exchangeCodeList.put("BSE", "1.");
            exchangeCodeList.put("NSE", "4.");
            exchangeCodeList.put("NDX", "13.");
            exchangeCodeList.put("MCX", "6.");
            exchangeCodeList.put("NFO", "4.");
            exchangeCodeList.put("BFO", "2.");

            if (this.interval == null || this.interval.isEmpty()) {
                exchangeCodeList.put("BFO", "8.");
            }

            String exchangeCodeName = exchangeCodeList.getOrDefault(exchangeCode, "");
            if (Objects.isNull(exchangeCodeName) || exchangeCodeName.isEmpty() || exchangeCodeName.trim().isEmpty())
                this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.EXCHANGE_CODE_EXCEPTION));
            else if (Objects.isNull(stockCode) || stockCode.isEmpty() || stockCode.trim().isEmpty())
                this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.STOCK_CODE_EXCEPTION));
            else {
                String tokenValue = "";
                if (exchangeCode.toLowerCase().equals("bse"))
                    tokenValue = this.stockScriptDictList.get(0).optString(stockCode, "");
                else if (exchangeCode.toLowerCase().equals("nse"))
                    tokenValue = this.stockScriptDictList.get(1).optString(stockCode, "");
                else {
                    String contractDetailValue = "";
                    if (expiryDate.equals(""))
                        this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.EXPIRY_DATE_EXCEPTION));
                    if (productType.toLowerCase().equals("futures"))
                        contractDetailValue = "FUT";
                    else if (productType.toLowerCase().equals("options"))
                        contractDetailValue = "OPT";
                    else
                        this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.PRODUCT_TYPE_EXCEPTION));

                    contractDetailValue = contractDetailValue + "-" + stockCode + "-" + expiryDate;
                    if (productType.toLowerCase().equals("options")) {
                        if (strikePrice.equals(""))
                            this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.STRIKE_PRICE_EXCEPTION));
                        else
                            contractDetailValue = contractDetailValue + "-" + strikePrice;

                        if (right.toLowerCase().equals("put"))
                            contractDetailValue = contractDetailValue + "-" + "PE";
                        else if (right.toLowerCase().equals("call"))
                            contractDetailValue = contractDetailValue + "-" + "CE";
                        else
                            this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.RIGHT_EXCEPTION));
                    }
                    if (exchangeCode.toLowerCase().equals("ndx"))
                        tokenValue = this.stockScriptDictList.get(2).optString(contractDetailValue, "");
                    else if (exchangeCode.toLowerCase().equals("mcx"))
                        tokenValue = this.stockScriptDictList.get(3).optString(contractDetailValue, "");
                    else if (exchangeCode.toLowerCase().equals("nfo"))
                        tokenValue = this.stockScriptDictList.get(4).optString(contractDetailValue, "");
                    else if (exchangeCode.toLowerCase().equals("bfo"))
                        tokenValue = this.stockScriptDictList.get(5).optString(contractDetailValue, "");
                }

                if (tokenValue.equals(""))
                    this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.STOCK_NOT_EXIST_EXCEPTION));

                String exchangeQuotesTokenValue = "";
                if (getExchangeQuotes)
                    exchangeQuotesTokenValue = exchangeCodeName + "1!" + tokenValue;

                String marketDepthTokenValue = "";
                if (getMarketDepth)
                    marketDepthTokenValue = exchangeCodeName + "2!" + tokenValue;

                Map<String, String> token_map = new HashMap<>();
                token_map.put("exchangeQuotesToken", exchangeQuotesTokenValue);
                token_map.put("marketDepthToken", marketDepthTokenValue);
                return token_map;
            }
        }

        return null;
    }

    public void generateSession(String secretKey, String sessionToken) throws Exception{
        try{
            JSONObject requestBody = new JSONObject();
            requestBody.put("SessionToken",sessionToken);
            requestBody.put("AppKey",this.apiKey);
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGetWithEntity httpGet = new HttpGetWithEntity();
            StringEntity requestEntity = new StringEntity(requestBody.toString());
            httpGet.setURI(URI.create(config.urls.get(Config.UrlEnum.API_URL)+config.endPoints.get(Config.EndPointEnum.CUST_DETAILS)));
            httpGet.setEntity(requestEntity);
            httpGet.setHeader("Accept", "application/json");
            httpGet.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = client.execute(httpGet);
            client.close();
            HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity, "UTF-8");
            JSONObject responseJson = new JSONObject(responseString);
            if (responseJson.has("Success") && !responseJson.isNull("Success") && !responseJson.getString("Success").isEmpty() && !responseJson.getString("Success").trim().isEmpty()) {
                JSONObject successJson = new JSONObject(responseJson.getString("Success"));
                String base64SessionToken = successJson.get("session_token").toString();
                this.setSession(this.apiKey,secretKey,base64SessionToken);
                Base64 base64 = new Base64();
                String cred = new String(base64.decode(base64SessionToken.getBytes()));
                String[] parts = cred.split(":");
                this.userId = parts[0];
                this.sessionToken = parts[1];
            }
            else
                this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.AUTHENICATION_EXCEPTION));

        } catch (Exception e) {
            this.errorException(e.toString());
        }
    }

    public void connect(boolean isOrder, boolean isOHLC) {
        HashMap<String, String> authDict = new HashMap<>();
        authDict.put("user", this.userId);
        authDict.put("token", this.sessionToken);
        authDict.put("appkey", this.apiKey);

        if(isOrder && (this.socketOrder == null)){
            IO.Options options = IO.Options.builder()
                    .setTransports(new String[]{WebSocket.NAME})
                    .setAuth(authDict)
                    .build();
            URI uriorder = URI.create(config.urls.get(Config.UrlEnum.LIVE_FEEDS_URL));
            this.socketOrder = IO.socket(uriorder, options);
            this.socketOrder.open();
        }
        if(isOHLC && (this.socketOHLC == null)){
            IO.Options options = IO.Options.builder()
                    .setTransports(new String[]{WebSocket.NAME})
                    .setAuth(authDict)
                    .setPath("/ohlcvstream/")
                    .build();
            URI uriohlc = URI.create(config.urls.get(Config.UrlEnum.LIVE_OHLC_STREAM_URL));
            this.socketOHLC = IO.socket(uriohlc, options);
            this.socketOHLC.open();
        }
        else if (this.socket == null) {
            IO.Options options = IO.Options.builder()
                    .setTransports(new String[]{WebSocket.NAME})
                    .setAuth(authDict)
                    .build();
            URI uri = URI.create(config.urls.get(Config.UrlEnum.LIVE_STREAM_URL));
            this.socket = IO.socket(uri, options);
            this.socket.open();
        }

    }
    void connectTicker()
    {
        this.connect(false,false);
    }

    private void watch(String[] stocks) {
        if (stocks != null && stocks.length > 0) {
            for (String stock : stocks) {
                this.socket.emit("join", stock);
            }
        }
    }

    private void unWatch(String[] stocks) {
        if (stocks != null && stocks.length > 0) {
            for (String stock : stocks) {
                this.socket.emit("leave", stock);
            }
        }
    }

    private void watchOHLC(String[] stocks) {
        if (stocks != null && stocks.length > 0) {
            for (String stock : stocks) {
                this.socketOHLC.emit("join", stock);
            }
        }
    }

    private void unWatchOHLC(String[] stocks) {
        if (stocks != null && stocks.length > 0) {
            for (String stock : stocks) {
                this.socketOHLC.emit("leave", stock);
            }
        }
    }

    public JSONObject subscribeFeeds(String stockToken) throws Exception {
        try {
            if(Objects.isNull(stockToken) || stockToken.isEmpty() || stockToken.trim().isEmpty())
                return this.socketConnectionResponse(config.responseMessage.get(Config.ResponseEnum.BLANK_STOCK_CODE));
            this.watch(new String[] {stockToken});
            return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.STOCK_SUBSCRIBE_MESSAGE), stockToken));
        }
        catch (Exception e){
            e.printStackTrace();
            this.errorException(e.toString());
        }
        return null;
    }

    public JSONObject subscribeFeeds(String stockToken,String interval) throws Exception {
        try {
            if(!Objects.equals(interval, "")) {
                if((!(Arrays.asList(config.typeLists.get(Config.ListEnum.INTERVAL_TYPES_STREAM_OHLC))).contains(interval.toLowerCase())))
                    this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.STREAM_OHLC_INTERVAL_ERROR));
                else
                    this.interval = config.channelIntervalMap.get(interval);
            }
            else {
                this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.STREAM_OHLC_INTERVAL_ERROR));
            }
            this.connect(false,true);
            if(Objects.isNull(stockToken) || stockToken.isEmpty() || stockToken.trim().isEmpty())
                return this.socketConnectionResponse(config.responseMessage.get(Config.ResponseEnum.BLANK_STOCK_CODE));
            this.watchOHLC(new String[] {stockToken});
            return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.STOCK_SUBSCRIBE_MESSAGE), stockToken));
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return null;
    }

    public JSONObject subscribeFeeds(boolean getOrderNotification) throws Exception {
        try {
            if(Objects.isNull(getOrderNotification) || !getOrderNotification)
                return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.ORDER_FLAG_NOT_SET), "true"));
            else if(orderNotificationSubscribed)
                return this.socketConnectionResponse(config.responseMessage.get(Config.ResponseEnum.ORDER_NOTIFICATION_SUBSCRIBED));
            else {
                orderNotificationSubscribed = true;
                this.connect(true,false);
                return this.socketConnectionResponse(config.responseMessage.get(Config.ResponseEnum.ORDER_NOTIFICATION_SUBSCRIBED));
            }
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return null;
    }

    public JSONObject subscribeFeeds(String exchangeCode,String stockCode,String productType,String expiryDate,
                                     String strikePrice,String right,boolean getExchangeQuotes,boolean getMarketDepth) throws Exception {
        try {
            Map<String, String> tokenObject = this.getStockTokenValue(exchangeCode, stockCode, productType, expiryDate, strikePrice, right,
                    getExchangeQuotes, getMarketDepth);
            if(!tokenObject.getOrDefault("exchangeQuotesToken","").equals(""))
                this.watch(new String[] {tokenObject.get("exchangeQuotesToken")});
            if(!tokenObject.getOrDefault("marketDepthToken","").equals(""))
                this.watch(new String[] {tokenObject.get("marketDepthToken")});
            return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.STOCK_SUBSCRIBE_MESSAGE), stockCode));
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return null;
    }

    public JSONObject subscribeFeeds(String exchangeCode,String stockCode,String productType,String expiryDate,
                                     String strikePrice,String right,boolean getExchangeQuotes,boolean getMarketDepth,String interval) throws Exception {
        try {
            if(!Objects.equals(interval, "")) {
                if((!(Arrays.asList(config.typeLists.get(Config.ListEnum.INTERVAL_TYPES_STREAM_OHLC))).contains(interval.toLowerCase())))
                    this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.STREAM_OHLC_INTERVAL_ERROR));
                else
                    this.interval = config.channelIntervalMap.get(interval);
            }
            else {
                this.errorException(config.exceptionMessage.get(Config.ExceptionEnum.STREAM_OHLC_INTERVAL_ERROR));
            }
            Map<String, String> tokenObject = this.getStockTokenValue(exchangeCode, stockCode, productType, expiryDate, strikePrice, right,
                    getExchangeQuotes, getMarketDepth);
            this.connect(false,true);
            if(!tokenObject.getOrDefault("exchangeQuotesToken","").equals(""))
                this.watchOHLC(new String[] {tokenObject.get("exchangeQuotesToken")});
            return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.STOCK_SUBSCRIBE_MESSAGE), stockCode));
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return null;
    }

    public JSONObject unsubscribeFeeds(String stockToken) throws Exception {
        try {
            if(Objects.isNull(stockToken) || stockToken.isEmpty() || stockToken.trim().isEmpty())
                return this.socketConnectionResponse(config.responseMessage.get(Config.ResponseEnum.BLANK_STOCK_CODE));
            this.unWatch(new String[] {stockToken});
            return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.STOCK_UNSUBSCRIBE_MESSAGE), stockToken));
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return null;
    }

    public JSONObject unsubscribeFeeds(String stockToken,String interval) throws Exception {
        try {
            if(Objects.isNull(stockToken) || stockToken.isEmpty() || stockToken.trim().isEmpty())
                return this.socketConnectionResponse(config.responseMessage.get(Config.ResponseEnum.BLANK_STOCK_CODE));
            this.unWatchOHLC(new String[] {stockToken});
            return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.STOCK_UNSUBSCRIBE_MESSAGE), stockToken));
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return null;
    }


    public JSONObject unsubscribeFeeds(boolean getOrderNotification) throws Exception {
        try {
            if(Objects.isNull(getOrderNotification) || getOrderNotification)
                return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.ORDER_FLAG_NOT_SET), "false"));
            else if(!orderNotificationSubscribed)
                return this.socketConnectionResponse(config.responseMessage.get(Config.ResponseEnum.ORDER_REFRESH_DISCONNECTED));
            else {
                orderNotificationSubscribed = false;
                return this.socketConnectionResponse(config.responseMessage.get(Config.ResponseEnum.ORDER_REFRESH_DISCONNECTED));
            }
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return null;
    }

    public JSONObject unsubscribeFeeds(String exchangeCode,String stockCode,String productType,String expiryDate,
                                       String strikePrice,String right,boolean getExchangeQuotes,boolean getMarketDepth) throws Exception {
        try {
            JSONObject returnObject = new JSONObject();
            Map<String, String> tokenObject = this.getStockTokenValue(exchangeCode, stockCode, productType, expiryDate, strikePrice, right,
                    getExchangeQuotes, getMarketDepth);
            if(!tokenObject.getOrDefault("exchangeQuotesToken","").equals(""))
                this.unWatch(new String[] {tokenObject.get("exchangeQuotesToken")});
            if(!tokenObject.getOrDefault("marketDepthToken","").equals(""))
                this.unWatch(new String[] {tokenObject.get("marketDepthToken")});
            return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.STOCK_UNSUBSCRIBE_MESSAGE), stockCode));
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return null;
    }

    public JSONObject unsubscribeFeeds(String exchangeCode,String stockCode,String productType,String expiryDate,
                                       String strikePrice,String right,boolean getExchangeQuotes,boolean getMarketDepth,String interval) throws Exception {
        try {
            JSONObject returnObject = new JSONObject();
            Map<String, String> tokenObject = this.getStockTokenValue(exchangeCode, stockCode, productType, expiryDate, strikePrice, right,
                    getExchangeQuotes, getMarketDepth);
            if(!tokenObject.getOrDefault("exchangeQuotesToken","").equals(""))
                this.unWatchOHLC(new String[] {tokenObject.get("exchangeQuotesToken")});
            if(!tokenObject.getOrDefault("marketDepthToken","").equals(""))
                this.unWatchOHLC(new String[] {tokenObject.get("marketDepthToken")});
            return this.socketConnectionResponse(String.format(config.responseMessage.get(Config.ResponseEnum.STOCK_UNSUBSCRIBE_MESSAGE), stockCode));
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return null;
    }

    public void registerOnTickEventListener(OnTickEventListener mListener) throws Exception {
        this.mListener = mListener;
        this.ticker();
    }

    private Object parseMarketDepth(Object data, String exchange) throws JSONException {
        JSONArray depth = new JSONArray();
        JSONArray jArray = (JSONArray) data;
        int counter = 0;
        for (int i = 0; i < jArray.length(); i++) {
            JSONArray lis = (JSONArray) jArray.get(i);
            counter += 1;
            Dictionary dict = new Hashtable();
            if (exchange.matches("1")) {
                dict.put("BestBuyRate-" + String.valueOf(counter), lis.get(0));
                dict.put("BestBuyQty-" + String.valueOf(counter), lis.get(1));
                dict.put("BestSellRate-" + String.valueOf(counter), lis.get(2));
                dict.put("BestSellQty-" + String.valueOf(counter), lis.get(3));
                depth.put(dict);
            } else {
                dict.put("BestBuyRate-" + String.valueOf(counter), lis.get(0));
                dict.put("BestBuyQty-" + String.valueOf(counter), lis.get(1));
                dict.put("BuyNoOfOrders-" + String.valueOf(counter), lis.get(2));
                dict.put("BuyFlag-" + String.valueOf(counter), lis.get(3));
                dict.put("BestSellRate-" + String.valueOf(counter), lis.get(4));
                dict.put("BestSellQty-" + String.valueOf(counter), lis.get(5));
                dict.put("SellNoOfOrders-" + String.valueOf(counter), lis.get(6));
                dict.put("SellFlag-" + String.valueOf(counter), lis.get(7));
                depth.put(dict);
            }
        }
        return depth;
    }

    public String dateParse(int epochValue) {
        long epoch = Long.parseLong(String.valueOf(epochValue));
        Date d = new Date(epoch * 1000);
        SimpleDateFormat ft = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
        return ft.format(d);
    }

    private Object parseData(Object data) throws Exception {
        //decode csv array to json payload
        Dictionary orderObject = new Hashtable();
        JSONArray jArray = (JSONArray) data;
        if (jArray!=null && jArray.get(0).getClass().getSimpleName().equals("String") && jArray.get(0).toString().indexOf('!')<0){
            orderObject.put("sourceNumber", jArray.get(0));                         //Source Number
            orderObject.put("group", jArray.get(1));                                //Group
            orderObject.put("userId", jArray.get(2));                               //User_id
            orderObject.put("key", jArray.get(3));                                  //Key
            orderObject.put("messageLength", jArray.get(4));                        //Message Length
            orderObject.put("requestType", jArray.get(5));                          //Request Type
            orderObject.put("messageSequence", jArray.get(6));                      //Message Sequence
            orderObject.put("messageDate", jArray.get(7));                          //Date
            orderObject.put("messageTime", jArray.get(8));                          //Time
            orderObject.put("messageCategory", jArray.get(9));                      //Message Category
            orderObject.put("messagePriority", jArray.get(10));                    //Priority
            orderObject.put("messageType", jArray.get(11));                        //Message Type
            orderObject.put("orderMatchAccount", jArray.get(12));                  //Order Match Account
            orderObject.put("orderExchangeCode", jArray.get(13));                  //Exchange Code
            if (jArray.get(11).equals("4") || jArray.get(11).equals("5")) {
                orderObject.put("orderStockCode", jArray.get(14));                     //Stock Code
                orderObject.put("orderFlow", this.tuxToUserValue.get("orderFlow").getOrDefault(jArray.get(15).toString().toUpperCase(),jArray.get(15).toString()));                          // Order Flow
                orderObject.put("limitMarketFlag", this.tuxToUserValue.get("limitMarketFlag").getOrDefault(jArray.get(16).toString().toUpperCase(),jArray.get(16).toString()));                    //Limit Market Flag
                orderObject.put("orderType", this.tuxToUserValue.get("orderType").getOrDefault(jArray.get(17).toString().toUpperCase(),jArray.get(17).toString()));                          //OrderType
                orderObject.put("orderLimitRate", jArray.get(18));                     //Limit Rate
                orderObject.put("productType", this.tuxToUserValue.get("productType").getOrDefault(jArray.get(19).toString().toUpperCase(),jArray.get(19).toString()));                        //Product Type
                orderObject.put("orderStatus", this.tuxToUserValue.get("limitMarketFlag").getOrDefault(jArray.get(20).toString().toUpperCase(),jArray.get(20).toString()));                        // Order Status
                orderObject.put("orderDate", jArray.get(21));                          //Order  Date
                orderObject.put("orderTradeDate", jArray.get(22));                     //Trade Date
                orderObject.put("orderReference", jArray.get(23));                     //Order Reference
                orderObject.put("orderQuantity", jArray.get(24));                      //Order Quantity
                orderObject.put("openQuantity", jArray.get(25));                       //Open Quantity
                orderObject.put("orderExecutedQuantity", jArray.get(26));              //Order Executed Quantity
                orderObject.put("cancelledQuantity", jArray.get(27));                  //Cancelled Quantity
                orderObject.put("expiredQuantity", jArray.get(28));                    //Expired Quantity
                orderObject.put("orderDisclosedQuantity", jArray.get(29));             // Order Disclosed Quantity
                orderObject.put("orderStopLossTrigger", jArray.get(30));                //Order Stop Loss Triger
                orderObject.put("orderSquareFlag", jArray.get(31));                    //Order Square Flag
                orderObject.put("orderAmountBlocked", jArray.get(32));                 // Order Amount Blocked
                orderObject.put("orderPipeId", jArray.get(33));                        //Order PipeId
                orderObject.put("channel", jArray.get(34));                            //Channel
                orderObject.put("exchangeSegmentCode", jArray.get(35));                //Exchange Segment Code
                orderObject.put("exchangeSegmentSettlement", jArray.get(36));          //Exchange Segment Settlement
                orderObject.put("segmentDescription", jArray.get(37));                 //Segment Description
                orderObject.put("marginSquareOffMode", jArray.get(38));                //Margin Square Off Mode
                orderObject.put("orderValidDate", jArray.get(40));                     //Order Valid Date
                orderObject.put("orderMessageCharacter", jArray.get(41));              //Order Message Character
                orderObject.put("averageExecutedRate", jArray.get(42));                //Average Exited Rate
                orderObject.put("orderPriceImprovementFlag", jArray.get(43));          //Order Price Flag
                orderObject.put("orderMBCFlag", jArray.get(44));                       //Order MBC Flag
                orderObject.put("orderLimitOffset", jArray.get(45));                   //Order Limit Offset
                orderObject.put("systemPartnerCode", jArray.get(46));                  //System Partner Code
            } else if (jArray.get(11).equals("6") || jArray.get(11).equals("7")) {
                orderObject.put("underlying", jArray.get(14));                         //Underlying
                orderObject.put("productType", this.tuxToUserValue.get("productType").getOrDefault(jArray.get(15).toString().toUpperCase(),jArray.get(15).toString()));                        //Product Type
                orderObject.put("optionType", this.tuxToUserValue.get("optionType").getOrDefault(jArray.get(16).toString().toUpperCase(),jArray.get(16).toString()));                         //Option Type
                orderObject.put("exerciseType", jArray.get(17));                      //Excercise Type
                orderObject.put("strikePrice", jArray.get(18));                        //Strike Price
                orderObject.put("expiryDate", jArray.get(19));                         //Expiry Date
                orderObject.put("orderValidDate", jArray.get(20));                     //Order Valid Date
                orderObject.put("orderFlow", this.tuxToUserValue.get("orderFlow").getOrDefault(jArray.get(21).toString().toUpperCase(),jArray.get(21).toString()));                          //Order  Flow
                orderObject.put("limitMarketFlag", this.tuxToUserValue.get("limitMarketFlag").getOrDefault(jArray.get(22).toString().toUpperCase(),jArray.get(22).toString()));                    //Limit Market Flag
                orderObject.put("orderType", this.tuxToUserValue.get("orderType").getOrDefault(jArray.get(23).toString().toUpperCase(),jArray.get(23).toString()));                          //Order Type
                orderObject.put("limitRate", jArray.get(24));                          //Limit Rate
                orderObject.put("orderStatus", this.tuxToUserValue.get("orderStatus").getOrDefault(jArray.get(25).toString().toUpperCase(),jArray.get(25).toString()));                        //Order Status
                orderObject.put("orderReference", jArray.get(26));                     //Order Reference
                orderObject.put("orderTotalQuantity", jArray.get(27));                 //Order Total Quantity
                orderObject.put("executedQuantity", jArray.get(28));                   //Executed Quantity
                orderObject.put("cancelledQuantity", jArray.get(29));                  //Cancelled Quantity
                orderObject.put("expiredQuantity", jArray.get(30));                    //Expired Quantity
                orderObject.put("stopLossTrigger", jArray.get(31));                    //Stop Loss Trigger
                orderObject.put("specialFlag", jArray.get(32));                        //Special Flag
                orderObject.put("pipeId", jArray.get(33));                             //PipeId
                orderObject.put("channel", jArray.get(34));                            //Channel
                orderObject.put("modificationOrCancelFlag", jArray.get(35));           //Modification or Cancel Flag
                orderObject.put("tradeDate", jArray.get(36));                          //Trade Date
                orderObject.put("acknowledgeNumber", jArray.get(37));                  //Acknowledgement Number
                orderObject.put("stopLossOrderReference", jArray.get(37));             //Stop Loss Order Reference
                orderObject.put("totalAmountBlocked", jArray.get(38));                 // Total Amount Blocked
                orderObject.put("averageExecutedRate", jArray.get(39));                //Average Executed Rate
                orderObject.put("cancelFlag", jArray.get(40));                         //Cancel Flag
                orderObject.put("squareOffMarket", jArray.get(41));                    //SquareOff Market
                orderObject.put("quickExitFlag", jArray.get(42));                      //Quick Exit Flag
                orderObject.put("stopValidTillDateFlag", jArray.get(43));              //Stop Valid till Date Flag
                orderObject.put("priceImprovementFlag", jArray.get(44));               //Price Improvement Flag
                orderObject.put("conversionImprovementFlag", jArray.get(45));          //Conversion Improvement Flag
                orderObject.put("trailUpdateCondition", jArray.get(45));               //Trail Update Condition
                orderObject.put("systemPartnerCode", jArray.get(46));                  //System Partner Code
            }
            return orderObject;
        }
        String exchange = jArray.getString(0).split("!")[0].split("\\.")[0];
        String data_type = jArray.getString(0).split("!")[0].split("\\.")[1];
        Dictionary feedObject = new Hashtable();
        if (data_type.matches("6")) {
            feedObject.put("Symbol", jArray.get(0));
            feedObject.put("AndiOPVolume", jArray.get(1));
            feedObject.put("Reserved", jArray.get(2));
            feedObject.put("IndexFlag", jArray.get(3));
            feedObject.put("ttq", jArray.get(4));
            feedObject.put("last", jArray.get(5));
            feedObject.put("ltq", jArray.get(6));
            feedObject.put("ltt", dateParse(jArray.getInt(7)));
            feedObject.put("AvgTradedPrice", jArray.get(8));
            feedObject.put("TotalBuyQnt", jArray.get(9));
            feedObject.put("TotalSellQnt", jArray.get(10));
            feedObject.put("ReservedStr", jArray.get(11));
            feedObject.put("ClosePrice", jArray.get(12));
            feedObject.put("OpenPrice", jArray.get(13));
            feedObject.put("HighPrice", jArray.get(14));
            feedObject.put("LowPrice", jArray.get(15));
            feedObject.put("ReservedShort", jArray.get(16));
            feedObject.put("CurrOpenInterest", jArray.get(17));
            feedObject.put("TotalTrades", jArray.get(18));
            feedObject.put("HightestPriceEver", jArray.get(19));
            feedObject.put("LowestPriceEver", jArray.get(20));
            feedObject.put("TotalTradedValue", jArray.get(21));
            int marketDepthIndex = 0;
            for (int i = 22; i < jArray.length(); i++) {
                String[] jData = (String[]) jArray.get(i);
                feedObject.put("Quantity-" + marketDepthIndex, jData[0]);
                feedObject.put("OrderPrice-" + marketDepthIndex, jData[1]);
                feedObject.put("TotalOrders-" + marketDepthIndex, jData[2]);
                feedObject.put("Reserved-" + marketDepthIndex, jData[3]);
                feedObject.put("SellQuantity-" + marketDepthIndex, jData[4]);
                feedObject.put("SellOrderPrice-" + marketDepthIndex, jData[5]);
                feedObject.put("SellTotalOrders-" + marketDepthIndex, jData[6]);
                feedObject.put("SellReserved-" + marketDepthIndex, jData[7]);
                marketDepthIndex++;
            }
        } else if (data_type.matches("1")) {
            feedObject.put("symbol", jArray.get(0));
            feedObject.put("open", jArray.get(1));
            feedObject.put("last", jArray.get(2));
            feedObject.put("high", jArray.get(3));
            feedObject.put("low", jArray.get(4));
            feedObject.put("change", jArray.get(5));
            feedObject.put("bPrice", jArray.get(6));
            feedObject.put("bQty", jArray.get(7));
            feedObject.put("sPrice", jArray.get(8));
            feedObject.put("sQty", jArray.get(9));
            feedObject.put("ltq", jArray.get(10));
            feedObject.put("avgPrice", jArray.get(11));
            feedObject.put("quotes", "Quotes Data");
            if (jArray.length() == 21) {
                feedObject.put("ttq", jArray.get(12));
                feedObject.put("totalBuyQt", jArray.get(13));
                feedObject.put("totalSellQ", jArray.get(14));
                feedObject.put("ttv", jArray.get(15));
                feedObject.put("trend", jArray.get(16));
                feedObject.put("lowerCktLm", jArray.get(17));
                feedObject.put("upperCktLm", jArray.get(18));
                feedObject.put("ltt", dateParse(jArray.getInt(19)));
                feedObject.put("close", jArray.get(20));
            } else if (jArray.length() == 23) {
                feedObject.put("OI", jArray.get(12));
                feedObject.put("CHNGOI", jArray.get(13));
                feedObject.put("ttq", jArray.get(14));
                feedObject.put("totalBuyQt", jArray.get(15));
                feedObject.put("totalSellQ", jArray.get(16));
                feedObject.put("ttv", jArray.get(17));
                feedObject.put("trend", jArray.get(18));
                feedObject.put("lowerCktLm", jArray.get(19));
                feedObject.put("upperCktLm", jArray.get(20));
                feedObject.put("ltt", dateParse(jArray.getInt(21)));
                feedObject.put("close", jArray.get(22));
            }
        } else {
            feedObject.put("symbol", jArray.get(0));
            feedObject.put("time", dateParse(jArray.getInt(1)));
            feedObject.put("depth", parseMarketDepth(jArray.get(2), exchange));
            feedObject.put("quotes", "Market Depth");
        }
        if (exchange.matches("4") && jArray.length() == 21) {
            feedObject.put("exchange", "NSE Equity");
        } else if (exchange.matches("1")) {
            feedObject.put("exchange", "BSE");
        } else if (exchange.matches("13")) {
            feedObject.put("exchange", "NSE Currency");
        } else if (exchange.matches("4") && jArray.length() == 23) {
            feedObject.put("exchange", "NSE Futures & Options");
        } else if (exchange.matches("6")) {
            feedObject.put("exchange", "Commodity");
        } else if (exchange.matches("8") && jArray.length() == 23){
            feedObject.put("exchange", "BSE Futures & Options");
        }


        try {
            if (feedObject.get("symbol").toString().length()>0) {
                Dictionary newObj = this.getDataFromStockTokenValue(feedObject.get("symbol").toString());
                for(Enumeration enm = newObj.keys(); enm.hasMoreElements();)
                {
                    String key = enm.nextElement().toString();
                    feedObject.put(key,newObj.get(key));
                }
            }
        }
        catch (Exception e){
            this.errorException(e.toString());
        }
        return feedObject;
    }


    public Object parseOHLCData(Object data) throws JSONException {
        String csvData = (String) data;
        String []arrayData = csvData.split(",");
        JSONObject candleData = null;
        if(Objects.equals(arrayData[0], "NSE") || Objects.equals(arrayData[0], "BSE")) {
            candleData = new JSONObject() {{
                put("interval",config.feedIntervalMap.get(arrayData[8]));
                put("exchange_code",arrayData[0]);
                put("stock_code",arrayData[1]);
                put("low",arrayData[2]);
                put("high",arrayData[3]);
                put("open",arrayData[4]);
                put("close",arrayData[5]);
                put("volume",arrayData[6]);
                put("datetime",arrayData[7]);
            }};
        }
        else if(Objects.equals(arrayData[0], "NFO") || Objects.equals(arrayData[0], "NDX") || Objects.equals(arrayData[0], "MCX") || Objects.equals(arrayData[0], "BFO" )) {
            if(arrayData.length==13){
                candleData = new JSONObject() {{
                    put("interval",config.feedIntervalMap.get(arrayData[12]));
                    put("exchange_code",arrayData[0]);
                    put("stock_code",arrayData[1]);
                    put("expiry_date",arrayData[2]);
                    put("strike_price",arrayData[3]);
                    put("right_type",arrayData[4]);
                    put("low",arrayData[5]);
                    put("high",arrayData[6]);
                    put("open",arrayData[7]);
                    put("close",arrayData[8]);
                    put("volume",arrayData[9]);
                    put("oi",arrayData[10]);
                    put("datetime",arrayData[11]);
                }};
            }
            else{
                candleData = new JSONObject() {{
                    put("interval",config.feedIntervalMap.get(arrayData[10]));
                    put("exchange_code",arrayData[0]);
                    put("stock_code",arrayData[1]);
                    put("expiry_date",arrayData[2]);
                    put("low",arrayData[3]);
                    put("high",arrayData[4]);
                    put("open",arrayData[5]);
                    put("close",arrayData[6]);
                    put("volume",arrayData[7]);
                    put("oi",arrayData[8]);
                    put("datetime",arrayData[9]);
                }};
            }
        }
        return candleData;
    }

    public void ticker(){
        if(this.socket!=null)
            this.socket.on("stock", (Listener) new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (mListener != null) {
                        Object data = null;
                        try {
                            data = parseData(args[0]);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mListener.onTickEvent(data);
                    }
                }
            });
        if(this.socketOrder!=null)
            this.socketOrder.on("order", (Listener) new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (orderNotificationSubscribed && mListener != null) {
                        Object data = null;
                        try {
                            data = parseData(args[0]);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        mListener.onTickEvent(data);
                    }
                }
            });
        if(this.socketOHLC!=null)
            this.socketOHLC.on(interval,(Listener) new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    if (interval!=null && mListener != null) {
                        Object data = null;
                        try {
                            data = parseOHLCData(args[0]);
                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        mListener.onTickEvent(data);
                    }
                }
            });
    }
}